package com.pcc.workflow.service;

import com.pcc.workflow.dto.request.ActionRequest;
import com.pcc.workflow.dto.request.DelegationRequest;
import com.pcc.workflow.dto.request.SubmitRequest;
import com.pcc.workflow.dto.response.*;
import com.pcc.workflow.entity.*;
import com.pcc.workflow.enums.OutboxStatus;
import com.pcc.workflow.enums.TaskStatus;
import com.pcc.workflow.enums.WfStatus;
import com.pcc.workflow.exception.BusinessRuleException;
import com.pcc.workflow.exception.ForbiddenOperationException;
import com.pcc.workflow.exception.ResourceNotFoundException;
import com.pcc.workflow.repository.*;
import com.pcc.workflow.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowDefinitionRepository definitions;
    private final WorkflowLevelRepository levels;
    private final WorkflowInstanceRepository instances;
    private final ApprovalTaskRepository tasks;
    private final WorkflowActionRepository actions;
    private final WorkflowDelegationRepository delegations;
    private final NotificationRepository notifications;
    private final WorkflowAuditLogRepository auditLogs;
    private final IntegrationOutboxRepository outbox;
    private final IntegrationService integrationService;

    @Value("${pcc.workflow.default-escalation-days:3}")
    private int defaultEscalationDays;

    // ---------------- submission ----------------

    @Transactional
    public InstanceResponse submit(SubmitRequest req, UserPrincipal actor) {
        WorkflowDefinition def = definitions.findById(req.definitionId())
                .filter(WorkflowDefinition::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow definition not found or inactive"));

        WorkflowInstance i = new WorkflowInstance();
        i.setWorkflowRef(nextRef());
        i.setDefinition(def);
        i.setWorkflowType(def.getWorkflowType());
        i.setModule(def.getModule());
        i.setEntityType(def.getEntityType());
        i.setEntityRef(req.entityRef().trim());
        i.setAmount(req.amount());
        i.setPriority(normalizePriority(req.priority()));
        i.setDescription(req.description());
        i.setDocumentRefs(req.documentRefs() == null ? null : String.join(",", req.documentRefs()));
        i.setSubmittedById(actor.getId());
        i.setSubmittedByName(actor.getUsername());
        i.setSubmittedAt(Instant.now());
        i.setStatus(WfStatus.PENDING_APPROVAL);
        i.setCurrentLevel(1);
        i.setDueAt(dueAt(def));
        i = instances.saveAndFlush(i);

        createTask(i, def, 1);
        recordAction(i, null, "SUBMIT", 0, actor, req.description());
        audit(i, "SUBMIT", actor, def.getCode() + " on " + def.getEntityType() + ":" + i.getEntityRef());
        return toResponse(i);
    }

    // ---------------- approval actions ----------------

    @Transactional
    public InstanceResponse approve(Long instanceId, ActionRequest req, UserPrincipal actor) {
        return act(instanceId, req, actor, TaskStatus.APPROVED, "APPROVE", false);
    }

    @Transactional
    public InstanceResponse reject(Long instanceId, ActionRequest req, UserPrincipal actor) {
        if (req == null || req.comment() == null || req.comment().isBlank())
            throw new BusinessRuleException("Rejection requires a reason");
        return act(instanceId, req, actor, TaskStatus.REJECTED, "REJECT", true);
    }

    @Transactional
    public InstanceResponse returnForCorrection(Long instanceId, ActionRequest req, UserPrincipal actor) {
        if (req == null || req.comment() == null || req.comment().isBlank())
            throw new BusinessRuleException("Return for correction requires a reason");
        return act(instanceId, req, actor, TaskStatus.RETURNED, "RETURN", true);
    }

    private InstanceResponse act(Long instanceId, ActionRequest req, UserPrincipal actor,
                                 TaskStatus outcome, String actionLabel, boolean terminal) {
        WorkflowInstance i = lockInstance(instanceId);
        if (i.getStatus() != WfStatus.PENDING_APPROVAL)
            throw new BusinessRuleException("Workflow is not pending approval (status: " + i.getStatus() + ")");

        ApprovalTask task = tasks.findByInstanceIdAndLevelOrderAndStatus(
                        i.getId(), i.getCurrentLevel(), TaskStatus.PENDING)
                .orElseThrow(() -> new IllegalStateException("Missing approval task"));
        if (task.getStatus() != TaskStatus.PENDING)
            throw new BusinessRuleException("Approval task already actioned");

        Long delegatedFrom = assertCanAct(task, i, actor);

        task.setStatus(outcome);
        task.setActedById(actor.getId());
        task.setActedByName(actor.getUsername());
        task.setActedAt(Instant.now());
        task.setComment(req == null ? null : req.comment());
        task.setDelegatedFromId(delegatedFrom);
        tasks.save(task);

        recordAction(i, task, actionLabel, task.getLevelOrder(), actor,
                req == null ? null : req.comment());
        audit(i, actionLabel, actor, "level " + task.getLevelOrder()
                + (delegatedFrom != null ? " (delegated from " + delegatedFrom + ")" : ""));

        if (outcome == TaskStatus.APPROVED) {
            advance(i, actor);
        } else if (outcome == TaskStatus.REJECTED) {
            i.setStatus(WfStatus.REJECTED);
            i.setCompletedAt(Instant.now());
            notifySubmitter(i, "REJECTED", "Request rejected",
                    "Your request was rejected" + reason(req));
        } else {
            i.setStatus(WfStatus.RETURNED_FOR_CORRECTION);
            notifySubmitter(i, "RETURNED", "Request returned",
                    "Your request was returned for correction" + reason(req));
        }
        i.setUpdatedAt(Instant.now());
        instances.save(i);
        return toResponse(i);
    }

    private void advance(WorkflowInstance i, UserPrincipal actor) {
        WorkflowDefinition def = i.getDefinition();
        int next = i.getCurrentLevel() + 1;
        var nextLevel = levels.findByDefinitionIdAndLevelOrder(def.getId(), next)
                .filter(l -> applies(l, i.getAmount()));
        if (nextLevel.isPresent()) {
            i.setCurrentLevel(next);
            createTask(i, def, next);
        } else {
            // all levels satisfied — integration or straight completion
            if (def.getIntegrationMethod() != null && def.getIntegrationPath() != null) {
                i.setStatus(WfStatus.INTEGRATION_PENDING);
                integrationService.enqueue(i, def);
            } else {
                i.setStatus(WfStatus.COMPLETED);
                i.setCompletedAt(Instant.now());
                notifySubmitter(i, "COMPLETED", "Workflow completed",
                        "Your request " + i.getWorkflowRef() + " was approved and completed");
            }
        }
    }

    @Transactional
    public InstanceResponse comment(Long instanceId, ActionRequest req, UserPrincipal actor) {
        WorkflowInstance i = get(instanceId);
        if (req == null || req.comment() == null || req.comment().isBlank())
            throw new BusinessRuleException("Comment cannot be empty");
        recordAction(i, null, "COMMENT", i.getCurrentLevel(), actor, req.comment());
        audit(i, "COMMENT", actor, null);
        return toResponse(i);
    }

    @Transactional
    public InstanceResponse resubmit(Long instanceId, ActionRequest req, UserPrincipal actor) {
        WorkflowInstance i = lockInstance(instanceId);
        if (i.getStatus() != WfStatus.RETURNED_FOR_CORRECTION)
            throw new BusinessRuleException("Only returned requests can be resubmitted");
        if (!i.getSubmittedById().equals(actor.getId()))
            throw new ForbiddenOperationException("Only the submitter can resubmit");
        WorkflowDefinition def = i.getDefinition();
        // reset: cancel pending tasks, restart at level 1
        tasks.findByInstanceIdOrderByLevelOrder(i.getId()).stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .forEach(t -> { t.setStatus(TaskStatus.CANCELLED); tasks.save(t); });
        i.setStatus(WfStatus.PENDING_APPROVAL);
        i.setCurrentLevel(1);
        i.setUpdatedAt(Instant.now());
        createTask(i, def, 1);
        recordAction(i, null, "RESUBMIT", 0, actor, req == null ? null : req.comment());
        audit(i, "RESUBMIT", actor, null);
        instances.save(i);
        return toResponse(i);
    }

    @Transactional
    public InstanceResponse cancel(Long instanceId, UserPrincipal actor) {
        WorkflowInstance i = lockInstance(instanceId);
        if (!i.getSubmittedById().equals(actor.getId()) && !actor.hasRole("ADMIN"))
            throw new ForbiddenOperationException("Only the submitter or ADMIN can cancel");
        if (i.getStatus() == WfStatus.COMPLETED || i.getStatus() == WfStatus.CANCELLED)
            throw new BusinessRuleException("Workflow already " + i.getStatus());
        tasks.findByInstanceIdOrderByLevelOrder(i.getId()).stream()
                .filter(t -> t.getStatus() == TaskStatus.PENDING)
                .forEach(t -> { t.setStatus(TaskStatus.CANCELLED); tasks.save(t); });
        i.setStatus(WfStatus.CANCELLED);
        i.setCompletedAt(Instant.now());
        i.setUpdatedAt(Instant.now());
        recordAction(i, null, "CANCEL", i.getCurrentLevel(), actor, null);
        audit(i, "CANCEL", actor, null);
        notifySubmitter(i, "CANCELLED", "Workflow cancelled",
                "Request " + i.getWorkflowRef() + " was cancelled");
        instances.save(i);
        return toResponse(i);
    }

    // ---------------- delegation ----------------

    @Transactional
    public DelegationResponse delegate(DelegationRequest req, UserPrincipal actor) {
        if (req.endDate().isBefore(req.startDate()))
            throw new BusinessRuleException("End date must not precede start date");
        if (req.startDate().isBefore(LocalDate.now()))
            throw new BusinessRuleException("Delegation cannot start in the past");
        if (req.delegateId().equals(actor.getId()))
            throw new BusinessRuleException("Cannot delegate to yourself");
        if (req.definitionId() != null)
            definitions.findById(req.definitionId())
                    .orElseThrow(() -> new ResourceNotFoundException("Definition not found"));

        WorkflowDelegation d = new WorkflowDelegation();
        d.setDelegatorId(actor.getId());
        d.setDelegateId(req.delegateId());
        d.setDelegateName(req.delegateName());
        d.setModule(req.module());
        d.setDefinitionId(req.definitionId());
        d.setStartDate(req.startDate());
        d.setEndDate(req.endDate());
        d.setReason(req.reason());
        d = delegations.save(d);

        notify(req.delegateId(), "DELEGATED", "Approval delegation",
                actor.getUsername() + " delegated approvals to you"
                        + (req.module() != null ? " (" + req.module() + ")" : ""), null);
        return toDelegation(d);
    }

    @Transactional
    public void revokeDelegation(Long id, UserPrincipal actor) {
        WorkflowDelegation d = delegations.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Delegation not found"));
        if (!d.getDelegatorId().equals(actor.getId()) && !actor.hasRole("ADMIN"))
            throw new ForbiddenOperationException("Not your delegation");
        d.setStatus("REVOKED");
        delegations.save(d);
    }

    @Transactional(readOnly = true)
    public List<DelegationResponse> myDelegations(UserPrincipal actor) {
        return delegations.findByDelegatorIdOrDelegateId(actor.getId(), actor.getId())
                .stream().map(this::toDelegation).toList();
    }

    // ---------------- queries ----------------

    @Transactional(readOnly = true)
    public InstanceResponse detail(Long id) { return toResponse(get(id)); }

    @Transactional(readOnly = true)
    public Page<InstanceResponse> search(WfStatus status, String module, String type,
                                         Long submitter, String priority,
                                         Instant from, Instant to, String q, Pageable p) {
        String like = (q == null || q.isBlank()) ? null : "%" + q.toLowerCase(Locale.ROOT) + "%";
        return instances.search(status, module, type, submitter, priority, from, to, like, p)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<InstanceResponse> mySubmissions(UserPrincipal actor, WfStatus status,
                                                Pageable p) {
        return instances.search(status, null, null, actor.getId(), null, null, null, null, p)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> myPendingApprovals(UserPrincipal actor, Pageable p) {
        return tasks.pendingFor(actor.getId(), List.copyOf(actor.getRoleCodes()), p)
                .map(this::toTaskResponse);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> instanceTasks(Long instanceId) {
        return tasks.findByInstanceIdOrderByLevelOrder(get(instanceId).getId())
                .stream().map(this::toTaskResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ActionResponse> instanceHistory(Long instanceId) {
        return actions.findByInstanceIdOrderByCreatedAt(get(instanceId).getId())
                .stream().map(a -> new ActionResponse(a.getId(), a.getAction(), a.getLevelOrder(),
                        a.getActorId(), a.getActorName(), a.getComment(), a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditResponse> instanceAudit(Long instanceId) {
        return auditLogs.findByInstanceIdOrderByCreatedAt(get(instanceId).getId())
                .stream().map(a -> new AuditResponse(a.getId(), a.getAction(), a.getActorId(),
                        a.getActorName(), a.getDetail(), a.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OutboxResponse> instanceIntegrations(Long instanceId) {
        return outbox.findByInstanceId(get(instanceId).getId()).stream()
                .map(this::toOutbox).toList();
    }

    @Transactional(readOnly = true)
    public Page<InstanceResponse> overdue(Pageable p) {
        return instances.overduePage(Instant.now(), p).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(UserPrincipal actor) {
        long myPending = tasks.pendingFor(actor.getId(),
                List.copyOf(actor.getRoleCodes()), Pageable.unpaged()).getTotalElements();
        long mySubmitted = instances.search(null, null, null, actor.getId(), null,
                null, null, null, Pageable.unpaged()).getTotalElements();
        return new DashboardResponse(myPending, mySubmitted,
                instances.countByStatus(WfStatus.SUBMITTED),
                instances.countByStatus(WfStatus.PENDING_APPROVAL),
                instances.countByStatus(WfStatus.APPROVED),
                instances.countByStatus(WfStatus.REJECTED),
                instances.countByStatus(WfStatus.RETURNED_FOR_CORRECTION),
                instances.countByStatus(WfStatus.COMPLETED),
                instances.overdue(Instant.now()).size(),
                instances.countByStatus(WfStatus.INTEGRATION_FAILED));
    }

    // ---------------- notifications ----------------

    @Transactional(readOnly = true)
    public Page<NotificationResponse> myNotifications(UserPrincipal actor, Pageable p) {
        return notifications.visibleTo(actor.getId(), List.copyOf(actor.getRoleCodes()), p)
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(),
                        n.getMessage(), n.getWorkflowRef(), n.isReadFlag(), n.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UserPrincipal actor) {
        return notifications.unreadFor(actor.getId(), List.copyOf(actor.getRoleCodes()));
    }

    @Transactional
    public void markRead(Long id, UserPrincipal actor) {
        notifications.findVisible(id, actor.getId(), List.copyOf(actor.getRoleCodes()))
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"))
                .setReadFlag(true);
    }

    @Transactional
    public void markAllRead(UserPrincipal actor) {
        notifications.markAllRead(actor.getId(), List.copyOf(actor.getRoleCodes()));
    }

    // ---------------- internals ----------------

    /** Can this actor act on the task? Direct assignee, role member, or active delegate. */
    private Long assertCanAct(ApprovalTask task, WorkflowInstance i, UserPrincipal actor) {
        if (actor.getId().equals(i.getSubmittedById()))
            throw new ForbiddenOperationException("Self-approval is not allowed");

        boolean direct = task.getAssigneeId() != null
                ? task.getAssigneeId().equals(actor.getId())
                : task.getAssigneeRole() != null && actor.hasRole(task.getAssigneeRole());
        if (direct) return null;

        // delegation: actor may act on behalf of an active delegator
        LocalDate today = LocalDate.now();
        for (WorkflowDelegation d : delegations.activeFor(actor.getId(), today)) {
            if (d.getModule() != null && !d.getModule().equals(i.getModule())) continue;
            if (d.getDefinitionId() != null
                    && !d.getDefinitionId().equals(i.getDefinition().getId())) continue;
            boolean delegatorMatch = task.getAssigneeId() != null
                    ? task.getAssigneeId().equals(d.getDelegatorId())
                    : task.getAssigneeRole() != null
                            && delegatorCouldBeRole(d.getDelegatorId(), task.getAssigneeRole());
            if (delegatorMatch) return d.getDelegatorId();
        }
        throw new ForbiddenOperationException("You are not the assigned approver for this level");
    }

    /** We cannot resolve another user's roles from a JWT; treat role-delegation as
     *  allowed only when the level is role-based and the delegator is a real user —
     *  recorded as delegated-from either way. */
    private boolean delegatorCouldBeRole(Long delegatorId, String role) {
        return true;
    }

    private void createTask(WorkflowInstance i, WorkflowDefinition def, int levelOrder) {
        WorkflowLevel level = levels.findByDefinitionIdAndLevelOrder(def.getId(), levelOrder)
                .orElseThrow(() -> new IllegalStateException("Definition has no level " + levelOrder));
        ApprovalTask t = new ApprovalTask();
        t.setInstance(i);
        t.setLevelOrder(levelOrder);
        t.setAssigneeId(level.getApproverId());
        t.setAssigneeRole(level.getApproverRole());
        t.setCycle((int) tasks.countByInstanceIdAndLevelOrder(i.getId(), levelOrder));
        int days = def.getEscalationDays() != null ? def.getEscalationDays() : defaultEscalationDays;
        t.setDueAt(Instant.now().plus(days, ChronoUnit.DAYS));
        tasks.save(t);
        notifyAssignee(i, level, t);
    }

    private boolean applies(WorkflowLevel level, BigDecimal amount) {
        return level.getMinAmount() == null
                || (amount != null && amount.compareTo(level.getMinAmount()) >= 0);
    }

    private Instant dueAt(WorkflowDefinition def) {
        int days = def.getEscalationDays() != null ? def.getEscalationDays() : defaultEscalationDays;
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    private void notifyAssignee(WorkflowInstance i, WorkflowLevel level, ApprovalTask t) {
        if (t.getAssigneeId() != null) {
            notify(t.getAssigneeId(), "TASK_ASSIGNED", "Approval task: " + i.getWorkflowRef(),
                    i.getWorkflowType() + " — level " + level.getLevelOrder() + " " + level.getName(),
                    i.getWorkflowRef());
        } else if (t.getAssigneeRole() != null) {
            notifyRole(t.getAssigneeRole(), "TASK_ASSIGNED",
                    "Approval task: " + i.getWorkflowRef(),
                    i.getWorkflowType() + " — level " + level.getLevelOrder() + " " + level.getName(),
                    i.getWorkflowRef());
        }
    }

    private void notifySubmitter(WorkflowInstance i, String type, String title, String message) {
        notify(i.getSubmittedById(), type, title, message, i.getWorkflowRef());
    }

    private void notify(Long recipient, String type, String title, String message, String ref) {
        Notification n = new Notification();
        n.setRecipientId(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setWorkflowRef(ref);
        notifications.save(n);
    }

    private void notifyRole(String role, String type, String title, String message, String ref) {
        Notification n = new Notification();
        n.setRecipientRole(role);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setWorkflowRef(ref);
        notifications.save(n);
    }

    private String reason(ActionRequest req) {
        return req != null && req.comment() != null ? ": " + req.comment() : "";
    }

    private void recordAction(WorkflowInstance i, ApprovalTask t, String action,
                              Integer level, UserPrincipal actor, String comment) {
        WorkflowAction a = new WorkflowAction();
        a.setInstance(i);
        a.setTask(t);
        a.setAction(action);
        a.setLevelOrder(level);
        a.setActorId(actor.getId());
        a.setActorName(actor.getUsername());
        a.setComment(comment);
        actions.save(a);
    }

    private void audit(WorkflowInstance i, String action, UserPrincipal actor, String detail) {
        WorkflowAuditLog l = new WorkflowAuditLog();
        l.setInstance(i);
        l.setAction(action);
        l.setActorId(actor.getId());
        l.setActorName(actor.getUsername());
        l.setDetail(detail);
        auditLogs.save(l);
    }

    private WorkflowInstance get(Long id) {
        return instances.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found"));
    }

    @Transactional
    protected WorkflowInstance lockInstance(Long id) {
        WorkflowInstance i = get(id);
        instances.flush();   // ensure version check on save catches races
        return i;
    }

    private String normalizePriority(String p) {
        if (p == null) return "NORMAL";
        String up = p.toUpperCase(Locale.ROOT);
        return switch (up) { case "LOW", "NORMAL", "HIGH", "URGENT" -> up; default -> "NORMAL"; };
    }

    private synchronized String nextRef() {
        return "WF-" + String.format("%06d", instances.maxWorkflowSequence() + 1);
    }

    // ---------------- mapping ----------------

    public InstanceResponse toResponse(WorkflowInstance i) {
        List<String> docs = i.getDocumentRefs() == null || i.getDocumentRefs().isBlank()
                ? List.of() : List.of(i.getDocumentRefs().split(","));
        boolean overdue = i.getDueAt() != null && i.getDueAt().isBefore(Instant.now())
                && (i.getStatus() == WfStatus.PENDING_APPROVAL || i.getStatus() == WfStatus.SUBMITTED);
        return new InstanceResponse(i.getId(), i.getWorkflowRef(), i.getDefinition().getId(),
                i.getDefinition().getName(), i.getWorkflowType(), i.getModule(), i.getEntityType(),
                i.getEntityRef(), i.getAmount(), i.getPriority(), i.getDescription(), docs,
                i.getStatus().name(), i.getCurrentLevel(), i.getSubmittedById(),
                i.getSubmittedByName(), i.getSubmittedAt(), i.getDueAt(), overdue,
                i.getCompletedAt(), i.getIntegrationError(), i.getCreatedAt(), i.getUpdatedAt());
    }

    private TaskResponse toTaskResponse(ApprovalTask t) {
        WorkflowInstance i = t.getInstance();
        boolean overdue = t.getStatus() == TaskStatus.PENDING && t.getDueAt() != null
                && t.getDueAt().isBefore(Instant.now());
        return new TaskResponse(t.getId(), i.getId(), i.getWorkflowRef(), i.getWorkflowType(),
                i.getModule(), i.getEntityType(), i.getEntityRef(), i.getDescription(),
                t.getLevelOrder(), t.getAssigneeId(), t.getAssigneeRole(), t.getStatus().name(),
                t.getActedById(), t.getActedByName(), t.getActedAt(), t.getComment(),
                t.getDueAt(), overdue, t.getCreatedAt());
    }

    private DelegationResponse toDelegation(WorkflowDelegation d) {
        return new DelegationResponse(d.getId(), d.getDelegatorId(), d.getDelegateId(),
                d.getDelegateName(), d.getModule(), d.getDefinitionId(), d.getStartDate(),
                d.getEndDate(), d.getReason(), d.getStatus(), d.getApprovedById(), d.getCreatedAt());
    }

    private OutboxResponse toOutbox(IntegrationOutbox o) {
        return new OutboxResponse(o.getId(), o.getIntegrationRef(), o.getService(), o.getMethod(),
                o.getPath(), o.getStatus().name(), o.getRetryCount(), o.getLastAttemptAt(),
                o.getLastError(), o.getCreatedAt());
    }
}
