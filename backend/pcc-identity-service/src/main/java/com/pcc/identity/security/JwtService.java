package com.pcc.identity.security;

import com.pcc.identity.config.SecurityProperties;
import com.pcc.identity.entity.Permission;
import com.pcc.identity.entity.Role;
import com.pcc.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JwtService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getJwt().getAccessTokenTtlMinutes(), ChronoUnit.MINUTES);

        Set<String> roleCodes = user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet());
        Set<String> permissionCodes = user.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))            // stable identifier
                .issuer(properties.getJwt().getIssuer())
                .audience().add(properties.getJwt().getAudience()).and()
                .claim("username", user.getUsername())
                .claim("employeeCode", user.getEmployeeCode())
                .claim("roles", roleCodes)
                .claim("permissions", permissionCodes)
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public long getAccessTokenTtlSeconds() {
        return properties.getJwt().getAccessTokenTtlMinutes() * 60;
    }

    public long getRefreshTokenTtlDays() {
        return properties.getJwt().getRefreshTokenTtlDays();
    }

    /** Opaque, cryptographically random refresh token (stored hashed in DB). */
    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[64];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Validates signature, issuer, audience and expiration. Returns claims or throws JwtException. */
    public Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getJwt().getIssuer())
                    .requireAudience(properties.getJwt().getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw e;
        }
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    public Long getUserId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
