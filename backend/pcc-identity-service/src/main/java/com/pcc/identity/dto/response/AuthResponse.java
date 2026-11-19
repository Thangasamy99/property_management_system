package com.pcc.identity.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long accessTokenExpiresIn,
        String refreshToken,
        UserResponse user
) {
}
