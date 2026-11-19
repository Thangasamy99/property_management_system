package com.pcc.identity.service;

import com.pcc.identity.entity.RefreshToken;
import com.pcc.identity.entity.User;

public interface RefreshTokenService {

    /** Creates and persists a new refresh token; returns the raw value and row id. */
    IssuedRefreshToken issue(User user, String ipAddress, String userAgent);

    /** Looks up a stored token by its raw value; throws InvalidTokenException if missing. */
    RefreshToken requireActive(String rawToken);

    void revoke(String rawToken);

    void revokeAllForUser(Long userId);
}
