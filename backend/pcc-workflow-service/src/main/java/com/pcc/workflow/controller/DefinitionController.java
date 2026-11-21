package com.pcc.workflow.controller;

import com.pcc.workflow.dto.request.DefinitionRequest;
import com.pcc.workflow.dto.response.DefinitionResponse;
import com.pcc.workflow.security.UserPrincipal;
import com.pcc.workflow.service.DefinitionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflow-definitions")
@RequiredArgsConstructor
@Tag(name = "Workflow Definitions")
@SecurityRequirement(name = "bearerAuth")
public class DefinitionController {

    private final DefinitionService service;

    @GetMapping
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public List<DefinitionResponse> list(@RequestParam(required = false) String module) {
        return service.list(module);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_VIEW')")
    public DefinitionResponse detail(@PathVariable Long id) {
        return service.detail(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('WORKFLOW_CONFIGURE')")
    public DefinitionResponse create(@RequestBody @Valid DefinitionRequest req,
                                     @AuthenticationPrincipal UserPrincipal actor) {
        return service.create(req, actor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('WORKFLOW_CONFIGURE')")
    public DefinitionResponse update(@PathVariable Long id,
                                     @RequestBody @Valid DefinitionRequest req,
                                     @AuthenticationPrincipal UserPrincipal actor) {
        return service.update(id, req, actor);
    }
}
