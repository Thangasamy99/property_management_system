package com.pcc.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "pcc.security.jwt.secret=gateway-only-test-signing-key-0123456789abcdefghijklmnopqrstuvwxyz",
        "pcc.security.jwt.issuer=pcc-identity-service",
        "pcc.security.jwt.audience=pcc-property-management"
})
class GatewaySecurityTest {

    private static final String SECRET =
            "gateway-only-test-signing-key-0123456789abcdefghijklmnopqrstuvwxyz";

    @Autowired
    private JwtValidationFilter filter;

    @Autowired
    private RouteLocator routes;

    @Test
    void allSevenServiceRoutesAreConfigured() {
        Set<String> ids = routes.getRoutes().map(r -> r.getId()).collect(Collectors.toSet()).block();
        assertThat(ids).contains("identity", "property", "land", "finance",
                "documents", "workflows", "reporting");
    }

    @Test
    void missingJwtIsRejected401() {
        var exchange = exchange("/api/property/api/v1/properties", null);
        AtomicBoolean forwarded = new AtomicBoolean();
        filter.filter(exchange, markForwarded(forwarded)).block();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(forwarded).isFalse();
    }

    @Test
    void invalidJwtIsRejected401() {
        var exchange = exchange("/api/property/api/v1/properties", "Bearer not-a-jwt");
        AtomicBoolean forwarded = new AtomicBoolean();
        filter.filter(exchange, markForwarded(forwarded)).block();
        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(forwarded).isFalse();
    }

    @Test
    void validJwtIsForwardedWithoutReplacingAuthorization() {
        String token = token();
        var exchange = exchange("/api/property/api/v1/properties", "Bearer " + token);
        AtomicBoolean forwarded = new AtomicBoolean();
        filter.filter(exchange, markForwarded(forwarded)).block();
        assertThat(forwarded).isTrue();
        assertThat(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + token);
    }

    @Test
    void identityLoginIsPublic() {
        var exchange = exchange("/api/identity/api/v1/auth/login", null);
        AtomicBoolean forwarded = new AtomicBoolean();
        filter.filter(exchange, markForwarded(forwarded)).block();
        assertThat(forwarded).isTrue();
    }

    @Test
    void healthIsPublicButGatewayActuatorRoutesAreNot() {
        var health = exchange("/actuator/health", null);
        AtomicBoolean healthForwarded = new AtomicBoolean();
        filter.filter(health, markForwarded(healthForwarded)).block();
        assertThat(healthForwarded).isTrue();

        var routes = exchange("/actuator/gateway/routes", null);
        AtomicBoolean routesForwarded = new AtomicBoolean();
        filter.filter(routes, markForwarded(routesForwarded)).block();
        assertThat(routes.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(routesForwarded).isFalse();
    }

    private ServerWebExchange exchange(String path, String authorization) {
        var request = MockServerHttpRequest.get(path);
        if (authorization != null) request.header(HttpHeaders.AUTHORIZATION, authorization);
        return MockServerWebExchange.from(request);
    }

    private GatewayFilterChain markForwarded(AtomicBoolean called) {
        return exchange -> {
            called.set(true);
            return reactor.core.publisher.Mono.empty();
        };
    }

    private String token() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        return Jwts.builder()
                .subject("1")
                .issuer("pcc-identity-service")
                .audience().add("pcc-property-management").and()
                .claim("username", "test")
                .claim("type", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(key)
                .compact();
    }
}
