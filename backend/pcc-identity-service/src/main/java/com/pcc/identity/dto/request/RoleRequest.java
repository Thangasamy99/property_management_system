package com.pcc.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RoleRequest(
        @NotBlank(message = "Role code is required")
        @Size(max = 50)
        String code,

        @NotBlank(message = "Role name is required")
        @Size(max = 100)
        String name,

        @Size(max = 255)
        String description
) {
}
