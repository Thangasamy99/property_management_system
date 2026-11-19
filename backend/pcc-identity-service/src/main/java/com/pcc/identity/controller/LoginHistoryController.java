package com.pcc.identity.controller;

import com.pcc.identity.dto.response.LoginHistoryResponse;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.enums.LoginEventType;
import com.pcc.identity.service.LoginHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Tag(name = "Login History", description = "Organization-wide login/security events (AUDIT_LOG_VIEW required)")
@RestController
@RequestMapping("/api/v1/login-history")
@RequiredArgsConstructor
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @Operation(summary = "List all users' login history",
            description = "Paginated security events across all employees. Optional filters: username (partial) and eventType. Requires AUDIT_LOG_VIEW.")
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
    public ResponseEntity<PageResponse<LoginHistoryResponse>> list(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) LoginEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20) Pageable pageable) {
        Instant fromInstant = from == null ? null : from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to == null ? null : to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        return ResponseEntity.ok(loginHistoryService.search(username, eventType, fromInstant, toInstant, pageable));
    }
}
