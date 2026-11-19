package com.pcc.identity.mapper;

import com.pcc.identity.dto.response.RoleResponse;
import com.pcc.identity.entity.Permission;
import com.pcc.identity.entity.Role;

import java.util.stream.Collectors;

public final class RoleMapper {

    private RoleMapper() {
    }

    public static RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()),
                role.getCreatedAt(),
                role.getUpdatedAt()
        );
    }
}
