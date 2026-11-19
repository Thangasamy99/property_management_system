package com.pcc.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Employee login request. 'username' accepts either the username or the email address.")
public record LoginRequest(
        @Schema(example = "admin")
        @NotBlank(message = "Username is required")
        String username,

        @Schema(example = "ChangeMe!2026")
        @NotBlank(message = "Password is required")
        String password
) {
}
