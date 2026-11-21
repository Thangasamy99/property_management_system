package com.pcc.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.workflow.entity.IntegrationOutbox;
import com.pcc.workflow.entity.Notification;
import com.pcc.workflow.entity.WorkflowDefinition;
import com.pcc.workflow.entity.WorkflowInstance;
import com.pcc.workflow.enums.OutboxStatus;
import com.pcc.workflow.enums.WfStatus;
import com.pcc.workflow.integration.DownstreamGateway;
import com.pcc.workflow.repository.IntegrationOutboxRepository;
import com.pcc.workflow.repository.NotificationRepository;
import com.pcc.workflow.repository.WorkflowInstanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Durable downstream integration. Outbox rows are created in the same
 * transaction as the workflow approval; dispatch confirms success before
 * marking COMPLETED. Failures retry on a schedule plus manual retry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationService {

    private final IntegrationOutboxRepository outbox;
    private final WorkflowInstanceRepository instances;
    private final NotificationRepository notifications;
    private final DownstreamGateway gateway;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${pcc.workflow.max-retries:10}")
    private int maxRetries;

    /** Same-transaction enqueue + immediate best-effort dispatch. */
    @Transactional
    public void enqueue(WorkflowInstance i, WorkflowDefinition def) {
        IntegrationOutbox o = new IntegrationOutbox();
        o.setInstance(i);
        o.setIntegrationRef(i.getWorkflowRef() + "-I1");
        o.setService(i.getModule());
        o.setMethod(def.getIntegrationMethod());
        o.setPath(def.getIntegrationPath().replace("{id}", i.getEntityRef()));
        o.setPayload(payload(i));
        outbox.save(o);
        dispatch(o);   // best effort inside the approval request's JWT context
    }

    /** Attempt delivery; updates outbox + instance status. Safe to call repeatedly. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean dispatch(IntegrationOutbox o) {
        WorkflowInstance i = o.getInstance();
        if (o.getStatus() == OutboxStatus.SUCCESS) return true;
        if (o.getStatus() == OutboxStatus.PERMANENT) return false;

        var result = gateway.invoke(o.getService(), o.getMethod(), o.getPath(), o.getPayload());
        o.setLastAttemptAt(Instant.now());
        if (result.success()) {
            o.setStatus(OutboxStatus.SUCCESS);
            o.setLastError(null);
            o.setUpdatedAt(Instant.now());
            outbox.save(o);
            i.setStatus(WfStatus.COMPLETED);
            i.setCompletedAt(Instant.now());
            i.setIntegrationError(null);
            i.setUpdatedAt(Instant.now());
            instances.save(i);
            notify(i.getSubmittedById(), "COMPLETED", "Workflow completed",
                    "Request " + i.getWorkflowRef() + " approved and applied in " + i.getModule());
            return true;
        }

        o.setLastError(result.error());
        if (result.permanent()) {
            o.setStatus(OutboxStatus.PERMANENT);
            i.setStatus(WfStatus.INTEGRATION_FAILED);
        } else {
            o.setRetryCount(o.getRetryCount() + 1);
            if (o.getRetryCount() >= maxRetries) {
                o.setStatus(OutboxStatus.PERMANENT);
                i.setStatus(WfStatus.INTEGRATION_FAILED);
            } else {
                o.setStatus(OutboxStatus.FAILED);
            }
        }
        o.setUpdatedAt(Instant.now());
        outbox.save(o);
        i.setIntegrationError(result.error());
        i.setUpdatedAt(Instant.now());
        instances.save(i);
        return false;
    }

    /** Scheduled retry for PENDING/FAILED rows. */
    @Scheduled(fixedDelayString = "${pcc.workflow.retry-interval-ms:60000}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void retryPending() {
        for (IntegrationOutbox o : outbox.findByStatusInOrderByCreatedAt(
                List.of(OutboxStatus.PENDING, OutboxStatus.FAILED))) {
            try {
                if (!gateway.hasRelayToken()) {
                    // leave for request-driven/manual dispatch — don't burn retries
                    if (o.getStatus() == OutboxStatus.FAILED) {
                        o.setStatus(OutboxStatus.PENDING);
                        outbox.save(o);
                    }
                    continue;
                }
                dispatch(o);
            } catch (Exception e) {
                log.warn("Outbox {} dispatch error: {}", o.getIntegrationRef(), e.getMessage());
            }
        }
    }

    /** Manual retry by an authorized user (carries their JWT downstream).
     *  Unlike the scheduler, a human retry overrides PERMANENT — the user may
     *  have corrected the downstream cause (e.g. permissions, payload). */
    @Transactional
    public boolean retry(Long outboxId) {
        IntegrationOutbox o = outbox.findById(outboxId)
                .orElseThrow(() -> new com.pcc.workflow.exception.ResourceNotFoundException(
                        "Integration record not found"));
        if (o.getStatus() == OutboxStatus.PERMANENT) {
            o.setStatus(OutboxStatus.FAILED);
            o.setRetryCount(0);
            outbox.save(o);
        }
        return dispatch(o);
    }

    private String payload(WorkflowInstance i) {
        try {
            return mapper.writeValueAsString(Map.of(
                    "workflowRef", i.getWorkflowRef(),
                    "entityRef", i.getEntityRef(),
                    "approvedBy", i.getSubmittedByName(),
                    "source", "pcc-workflow-service"));
        } catch (Exception e) {
            return "{}";
        }
    }

    private void notify(Long recipient, String type, String title, String message) {
        Notification n = new Notification();
        n.setRecipientId(recipient);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        notifications.save(n);
    }
}
