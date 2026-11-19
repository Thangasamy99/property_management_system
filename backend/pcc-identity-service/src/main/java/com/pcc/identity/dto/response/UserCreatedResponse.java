package com.pcc.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserCreatedResponse(
        UserResponse user,

        @Schema(description = "Temporary password - present only when no password was supplied at creation")
        String temporaryPassword
) {
}
