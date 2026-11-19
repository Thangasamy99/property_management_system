package com.pcc.identity.controller;

import com.pcc.identity.dto.request.UserCreateRequest;
import com.pcc.identity.dto.request.UserRolesUpdateRequest;
import com.pcc.identity.dto.request.UserStatusUpdateRequest;
import com.pcc.identity.dto.request.UserUpdateRequest;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.dto.response.TemporaryPasswordResponse;
import com.pcc.identity.dto.response.UserCreatedResponse;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User Management", description = "Employee account administration (ADMIN permissions required)")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create employee", description = "Requires permission USER_CREATE. Returns a temporary password when none is supplied.")
    @PostMapping
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<UserCreatedResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @Operation(summary = "List / search employees (paginated)", description = "Requires permission USER_VIEW.")
    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<PageResponse<UserResponse>> list(
            @RequestParam(required = false) String search,
            @ParameterObject @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(userService.list(search, pageable));
    }

    @Operation(summary = "View employee", description = "Requires permission USER_VIEW.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @Operation(summary = "Update employee", description = "Requires permission USER_UPDATE.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @Operation(summary = "Activate / deactivate employee",
            description = "Requires permission USER_DEACTIVATE. Deactivation also revokes all sessions. Employees are never physically deleted.")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    public ResponseEntity<UserResponse> updateStatus(@PathVariable Long id,
                                                     @Valid @RequestBody UserStatusUpdateRequest request) {
        return ResponseEntity.ok(userService.updateStatus(id, request));
    }

    @Operation(summary = "Assign roles to employee",
            description = "Requires permission USER_ASSIGN_ROLE. A user cannot grant themselves the ADMIN role.")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('USER_ASSIGN_ROLE')")
    public ResponseEntity<UserResponse> updateRoles(@PathVariable Long id,
                                                    @Valid @RequestBody UserRolesUpdateRequest request) {
        return ResponseEntity.ok(userService.updateRoles(id, request));
    }

    @Operation(summary = "Reset employee password",
            description = "Requires permission USER_UPDATE. Generates a temporary password, forces change at next login and revokes sessions.")
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<TemporaryPasswordResponse> resetPassword(@PathVariable Long id) {
        return ResponseEntity.ok(new TemporaryPasswordResponse(userService.resetPassword(id)));
    }
}
