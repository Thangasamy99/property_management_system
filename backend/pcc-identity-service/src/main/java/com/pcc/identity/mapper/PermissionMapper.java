package com.pcc.identity.mapper;

import com.pcc.identity.dto.response.PermissionResponse;
import com.pcc.identity.entity.Permission;

public final class PermissionMapper {

    private PermissionMapper() {
    }

    public static PermissionResponse toResponse(Permission permission) {
        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getName(),
                permission.getModule(),
                permission.getDescription()
        );
    }
}
