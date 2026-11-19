package com.pcc.identity.service.impl;

import com.pcc.identity.config.SecurityProperties;
import com.pcc.identity.entity.RefreshToken;
import com.pcc.identity.entity.User;
import com.pcc.identity.exception.InvalidTokenException;
import com.pcc.identity.repository.RefreshTokenRepository;
import com.pcc.identity.security.JwtService;
import com.pcc.identity.service.IssuedRefreshToken;
import com.pcc.identity.service.RefreshTokenService;
import com.pcc.identity.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final SecurityProperties properties;

    @Override
    @Transactional
    public IssuedRefreshToken issue(User user, String ipAddress, String userAgent) {
        String raw = jwtService.generateRefreshTokenValue();
        RefreshToken entity = new RefreshToken();
        entity.setUser(user);
        entity.setTokenHash(TokenHashUtil.sha256(raw));
        entity.setExpiresAt(Instant.now().plus(jwtService.getRefreshTokenTtlDays(), ChronoUnit.DAYS));
        entity.setIpAddress(ipAddress);
        entity.setUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
        RefreshToken saved = refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(raw, saved.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken requireActive(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));
        if (!token.isActive()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }
        return token;
    }

    @Override
    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(TokenHashUtil.sha256(rawToken))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId);
    }
}
