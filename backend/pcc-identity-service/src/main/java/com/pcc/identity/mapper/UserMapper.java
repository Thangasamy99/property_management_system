package com.pcc.identity.mapper;

import com.pcc.identity.dto.response.RoleSummary;
import com.pcc.identity.dto.response.UserResponse;
import com.pcc.identity.entity.Permission;
import com.pcc.identity.entity.Role;
import com.pcc.identity.entity.User;

import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmployeeCode(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getAccountStatus(),
                user.isMustChangePassword(),
                user.isLocked(),
                user.getLastLoginAt(),
                user.getCreatedBy(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getRoles().stream()
                        .map(r -> new RoleSummary(r.getId(), r.getCode(), r.getName()))
                        .collect(Collectors.toSet()),
                user.getRoles().stream()
                        .flatMap(r -> r.getPermissions().stream())
                        .map(Permission::getCode)
                        .collect(Collectors.toSet())
        );
    }

    public static boolean hasRole(User user, String roleCode) {
        return user.getRoles().stream().map(Role::getCode).anyMatch(roleCode::equals);
    }
}
