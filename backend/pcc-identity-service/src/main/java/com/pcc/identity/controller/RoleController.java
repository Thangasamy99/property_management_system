package com.pcc.identity.controller;

import com.pcc.identity.dto.request.RolePermissionsUpdateRequest;
import com.pcc.identity.dto.request.RoleRequest;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.RoleResponse;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Tag(name = "Role Management", description = "Roles and role-permission mapping (ROLE_* permissions required)")
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Create role", description = "Requires permission ROLE_CREATE.")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_CREATE')")
    public ResponseEntity<RoleResponse> create(@Valid @RequestBody RoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.create(request));
    }

    @Operation(summary = "List roles", description = "Requires permission ROLE_VIEW.")
    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<List<RoleResponse>> list() {
        return ResponseEntity.ok(roleService.list());
    }

    @Operation(summary = "View role", description = "Requires permission ROLE_VIEW.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<RoleResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getById(id));
    }

    @Operation(summary = "Update role", description = "Requires permission ROLE_UPDATE.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_UPDATE')")
    public ResponseEntity<RoleResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody RoleRequest request) {
        return ResponseEntity.ok(roleService.update(id, request));
    }

    @Operation(summary = "List role permissions", description = "Requires permission ROLE_VIEW.")
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<Set<String>> getPermissions(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getPermissions(id));
    }

    @Operation(summary = "Set role permissions",
            description = "Requires permission ROLE_ASSIGN_PERMISSION. Configurable authorization - no code change needed.")
    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN_PERMISSION')")
    public ResponseEntity<RoleResponse> updatePermissions(@PathVariable Long id,
                                                          @Valid @RequestBody RolePermissionsUpdateRequest request) {
        return ResponseEntity.ok(roleService.updatePermissions(id, request));
    }

    @Operation(summary = "List users with this role", description = "Requires permission ROLE_VIEW.")
    @GetMapping("/{id}/users")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<PageResponse<UserResponse>> listUsers(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(roleService.listUsers(id, pageable));
    }
}
