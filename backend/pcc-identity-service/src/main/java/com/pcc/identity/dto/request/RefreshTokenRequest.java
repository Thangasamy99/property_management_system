package com.pcc.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Refresh request. The token may be sent here or via the HttpOnly refresh cookie.")
public record RefreshTokenRequest(
        @Schema(example = "8f3c2a1b...")
        String refreshToken
) {
}
