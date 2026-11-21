package com.pcc.reporting;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

public final class JwtTestHelper {

    public static final String SECRET =
            "test-secret-key-for-pcc-reporting-service-must-be-64-plus-chars!!";

    private JwtTestHelper() {}

    public static String token(long userId, String username, Set<String> roles,
                               Set<String> permissions) {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer("pcc-identity-service")
                .audience().add("pcc-property-management").and()
                .claim("username", username)
                .claim("employeeCode", "PCC-" + userId)
                .claim("roles", List.copyOf(roles))
                .claim("permissions", List.copyOf(permissions))
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(15, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }
}
