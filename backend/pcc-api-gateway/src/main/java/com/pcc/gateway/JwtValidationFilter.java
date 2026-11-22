package com.pcc.gateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Validates the shared PCC JWT (HS512, issuer/audience, exp) before proxying.
 * The Authorization header is forwarded untouched — downstream services
 * still enforce their own permission checks. Unauthenticated routes:
 * identity auth endpoints and actuator health.
 */
@Component
public class JwtValidationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PREFIXES = List.of(
            "/api/identity/api/v1/auth/login",
            "/api/identity/api/v1/auth/refresh",
            "/actuator");

    private final SecretKey key;
    private final String issuer;
    private final String audience;

    public JwtValidationFilter(@Value("${pcc.security.jwt.secret}") String secret,
                               @Value("${pcc.security.jwt.issuer}") String issuer,
                               @Value("${pcc.security.jwt.audience}") String audience) {
        if (secret == null || secret.length() < 64) {
            throw new IllegalStateException(
                    "JWT_SECRET must be at least 64 characters for HS512.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    public int getOrder() {
        return -100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS
                || PUBLIC_PREFIXES.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .build()
                    .parseSignedClaims(auth.substring(7))
                    .getPayload();
            if (!"access".equals(claims.get("type", String.class))) {
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token type");
            }
        } catch (Exception e) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse res = exchange.getResponse();
        res.setStatusCode(status);
        res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"timestamp\":\"" + java.time.Instant.now()
                + "\",\"status\":" + status.value()
                + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"message\":\"" + message
                + "\",\"path\":\"" + exchange.getRequest().getPath().value()
                + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buf = res.bufferFactory().wrap(body);
        return res.writeWith(Mono.just(buf));
    }
}
