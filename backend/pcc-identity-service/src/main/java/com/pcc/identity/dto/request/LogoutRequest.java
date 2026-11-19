package com.pcc.identity.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Logout request. The refresh token may be sent here or via the HttpOnly refresh cookie.")
public record LogoutRequest(
        String refreshToken
) {
}
