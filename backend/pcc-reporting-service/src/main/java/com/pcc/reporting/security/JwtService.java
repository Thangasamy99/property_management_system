package com.pcc.reporting.security;

import com.pcc.reporting.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/** Validates access tokens issued by pcc-identity-service (shared HS512 secret). */
@Slf4j
@Service
public class JwtService {

    private final SecurityProperties properties;
    private SecretKey signingKey;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void init() {
        String secret = properties.getJwt().getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is not configured. Set the JWT_SECRET environment variable (min 64 characters).");
        }
        if (secret.length() < 64) {
            throw new IllegalStateException("JWT_SECRET must be at least 64 characters for HS512.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Validates signature, issuer, audience and expiration. Throws on invalid tokens. */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.getJwt().getIssuer())
                .requireAudience(properties.getJwt().getAudience())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }
}
