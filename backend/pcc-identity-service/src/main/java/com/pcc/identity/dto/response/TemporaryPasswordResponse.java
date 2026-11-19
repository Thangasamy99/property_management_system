package com.pcc.identity.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record TemporaryPasswordResponse(
        @Schema(description = "One-time temporary password. The user must change it at next login.")
        String temporaryPassword
) {
}
