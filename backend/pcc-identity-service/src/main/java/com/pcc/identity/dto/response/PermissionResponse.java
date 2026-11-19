package com.pcc.identity.dto.response;

public record PermissionResponse(
        Long id,
        String code,
        String name,
        String module,
        String description
) {
}
