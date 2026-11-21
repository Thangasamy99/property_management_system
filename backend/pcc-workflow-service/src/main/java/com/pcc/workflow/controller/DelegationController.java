package com.pcc.workflow.controller;

import com.pcc.workflow.dto.request.DelegationRequest;
import com.pcc.workflow.dto.response.DelegationResponse;
import com.pcc.workflow.security.UserPrincipal;
import com.pcc.workflow.service.WorkflowService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/delegations")
@RequiredArgsConstructor
@Tag(name = "Delegations")
@SecurityRequirement(name = "bearerAuth")
public class DelegationController {

    private final WorkflowService service;

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('WORKFLOW_DELEGATE') or hasAuthority('WORKFLOW_VIEW')")
    public List<DelegationResponse> mine(@AuthenticationPrincipal UserPrincipal actor) {
        return service.myDelegations(actor);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORKFLOW_DELEGATE')")
    public DelegationResponse create(@RequestBody @Valid DelegationRequest req,
                                     @AuthenticationPrincipal UserPrincipal actor) {
        return service.delegate(req, actor);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_DELEGATE')")
    public void revoke(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        service.revokeDelegation(id, actor);
    }
}
