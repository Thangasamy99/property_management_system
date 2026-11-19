package com.pcc.identity.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UserRolesUpdateRequest(
        @NotNull(message = "Role codes are required")
        Set<String> roleCodes
) {
}
