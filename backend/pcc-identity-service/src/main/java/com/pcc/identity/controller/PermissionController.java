package com.pcc.identity.controller;

import com.pcc.identity.dto.response.PermissionResponse;
import com.pcc.identity.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "Permission Management", description = "Database-driven permission catalog")
@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "List all permissions", description = "Requires permission ROLE_VIEW.")
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<List<PermissionResponse>> list() {
        return ResponseEntity.ok(permissionService.listAll());
    }

    @Operation(summary = "Permissions grouped by module", description = "Requires permission ROLE_VIEW.")
    @GetMapping("/modules")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<Map<String, List<PermissionResponse>>> modules() {
        return ResponseEntity.ok(permissionService.groupedByModule());
    }
}
