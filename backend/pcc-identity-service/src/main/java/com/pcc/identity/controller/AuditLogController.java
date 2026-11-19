package com.pcc.identity.controller;

import com.pcc.identity.audit.AuditService;
import com.pcc.identity.dto.response.AuditLogResponse;
import com.pcc.identity.dto.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Audit Logs", description = "Security audit trail (AUDIT_LOG_VIEW required)")
@RestController
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditService auditService;

    @Operation(summary = "List audit logs",
            description = "Paginated audit trail. Optional filters: action (e.g. USER_CREATE) and entityType (USER, ROLE). Requires AUDIT_LOG_VIEW.")
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(auditService.search(action, entityType, pageable));
    }
}
