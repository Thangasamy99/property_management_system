package com.pcc.workflow.controller;

import com.pcc.workflow.dto.request.ActionRequest;
import com.pcc.workflow.dto.request.SubmitRequest;
import com.pcc.workflow.dto.response.*;
import com.pcc.workflow.enums.WfStatus;
import com.pcc.workflow.security.UserPrincipal;
import com.pcc.workflow.service.IntegrationService;
import com.pcc.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Tag(name = "Workflows")
@SecurityRequirement(name = "bearerAuth")
public class WorkflowController {

    private final WorkflowService service;
    private final IntegrationService integrationService;

    @PostMapping
    @PreAuthorize("hasAuthority('WORKFLOW_SUBMIT')")
    public InstanceResponse submit(@RequestBody @Valid SubmitRequest req,
                                   @AuthenticationPrincipal UserPrincipal actor) {
        return service.submit(req, actor);
    }

    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public InstanceResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public PageResponse<InstanceResponse> search(
            @RequestParam(required = false) WfStatus status,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long submitter,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        var parts = sort.split(",");
        Sort s = parts.length == 2 && parts[1].equalsIgnoreCase("asc")
                ? Sort.by(parts[0]).ascending() : Sort.by(parts[0]).descending();
        return PageResponse.of(service.search(status, module, type, submitter, priority,
                from, to, q, PageRequest.of(page, size, s)));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public PageResponse<InstanceResponse> mine(
            @RequestParam(required = false) WfStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal UserPrincipal actor) {
        return PageResponse.of(service.mySubmissions(actor, status,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/pending-approvals")
    @PreAuthorize("hasAuthority('WORKFLOW_APPROVE') or hasAuthority('WORKFLOW_REJECT') or hasAuthority('WORKFLOW_RETURN')")
    public PageResponse<TaskResponse> pendingApprovals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal UserPrincipal actor) {
        return PageResponse.of(service.myPendingApprovals(actor,
                PageRequest.of(page, size, Sort.by("dueAt").ascending())));
    }

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public PageResponse<InstanceResponse> overdue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return PageResponse.of(service.overdue(PageRequest.of(page, size)));
    }

    @PostMapping("/{id:\\d+}/approve")
    @PreAuthorize("hasAuthority('WORKFLOW_APPROVE')")
    public InstanceResponse approve(@PathVariable Long id,
                                    @RequestBody(required = false) ActionRequest req,
                                    @AuthenticationPrincipal UserPrincipal actor) {
        return service.approve(id, req, actor);
    }

    @PostMapping("/{id:\\d+}/reject")
    @PreAuthorize("hasAuthority('WORKFLOW_REJECT')")
    public InstanceResponse reject(@PathVariable Long id,
                                   @RequestBody @Valid ActionRequest req,
                                   @AuthenticationPrincipal UserPrincipal actor) {
        return service.reject(id, req, actor);
    }

    @PostMapping("/{id:\\d+}/return")
    @PreAuthorize("hasAuthority('WORKFLOW_RETURN')")
    public InstanceResponse returnForCorrection(@PathVariable Long id,
                                                @RequestBody @Valid ActionRequest req,
                                                @AuthenticationPrincipal UserPrincipal actor) {
        return service.returnForCorrection(id, req, actor);
    }

    @PostMapping("/{id:\\d+}/comment")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public InstanceResponse comment(@PathVariable Long id,
                                    @RequestBody @Valid ActionRequest req,
                                    @AuthenticationPrincipal UserPrincipal actor) {
        return service.comment(id, req, actor);
    }

    @PostMapping("/{id:\\d+}/resubmit")
    @PreAuthorize("hasAuthority('WORKFLOW_SUBMIT')")
    public InstanceResponse resubmit(@PathVariable Long id,
                                     @RequestBody(required = false) ActionRequest req,
                                     @AuthenticationPrincipal UserPrincipal actor) {
        return service.resubmit(id, req, actor);
    }

    @PostMapping("/{id:\\d+}/cancel")
    @PreAuthorize("hasAuthority('WORKFLOW_CANCEL')")
    public InstanceResponse cancel(@PathVariable Long id,
                                   @AuthenticationPrincipal UserPrincipal actor) {
        return service.cancel(id, actor);
    }

    @GetMapping("/{id:\\d+}/tasks")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public List<TaskResponse> tasks(@PathVariable Long id) {
        return service.instanceTasks(id);
    }

    @GetMapping("/{id:\\d+}/history")
    @PreAuthorize("hasAuthority('WORKFLOW_HISTORY_VIEW')")
    public List<ActionResponse> history(@PathVariable Long id) {
        return service.instanceHistory(id);
    }

    @GetMapping("/{id:\\d+}/audit")
    @PreAuthorize("hasAuthority('WORKFLOW_HISTORY_VIEW')")
    public List<AuditResponse> audit(@PathVariable Long id) {
        return service.instanceAudit(id);
    }

    @GetMapping("/{id:\\d+}/integrations")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public List<OutboxResponse> integrations(@PathVariable Long id) {
        return service.instanceIntegrations(id);
    }

    @PostMapping("/integrations/{outboxId}/retry")
    @PreAuthorize("hasAuthority('WORKFLOW_APPROVE') or hasAuthority('WORKFLOW_CONFIGURE')")
    public Map<String, Object> retryIntegration(@PathVariable Long outboxId) {
        return Map.of("success", integrationService.retry(outboxId));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public DashboardResponse dashboard(@AuthenticationPrincipal UserPrincipal actor) {
        return service.dashboard(actor);
    }
}
