package com.pcc.identity.dto.response;

import com.pcc.identity.enums.AccountStatus;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String employeeCode,
        String username,
        String firstName,
        String lastName,
        String email,
        String phone,
        AccountStatus accountStatus,
        boolean mustChangePassword,
        boolean locked,
        Instant lastLoginAt,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        Set<RoleSummary> roles,
        Set<String> permissions
) {
}
