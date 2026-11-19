package com.pcc.identity.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record RolePermissionsUpdateRequest(
        @NotNull(message = "Permission codes are required")
        Set<String> permissionCodes
) {
}
