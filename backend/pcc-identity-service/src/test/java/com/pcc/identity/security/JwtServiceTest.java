package com.pcc.identity.security;

import com.pcc.identity.config.SecurityProperties;
import com.pcc.identity.entity.Permission;
import com.pcc.identity.entity.Role;
import com.pcc.identity.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef!!";

    private JwtService jwtService;
    private SecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SecurityProperties();
        properties.getJwt().setSecret(SECRET);
        properties.getJwt().setIssuer("pcc-identity-service");
        properties.getJwt().setAudience("pcc-property-management");
        properties.getJwt().setAccessTokenTtlMinutes(15);
        jwtService = new JwtService(properties);
        jwtService.init();
    }

    private User sampleUser() {
        Permission p = new Permission();
        p.setCode("USER_VIEW");
        Role role = new Role();
        role.setCode("ADMIN");
        role.setPermissions(Set.of(p));
        User user = new User();
        user.setId(42L);
        user.setUsername("jdoe");
        user.setEmployeeCode("PCC-0042");
        user.setRoles(Set.of(role));
        return user;
    }

    @Test
    void generatesValidAccessTokenWithClaims() {
        String token = jwtService.generateAccessToken(sampleUser());

        Claims claims = jwtService.parseAndValidate(token);
        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo("pcc-identity-service");
        assertThat(claims.getAudience()).contains("pcc-property-management");
        assertThat(claims.get("username", String.class)).isEqualTo("jdoe");
        assertThat(jwtService.isAccessToken(claims)).isTrue();
        assertThat(jwtService.getUserId(claims)).isEqualTo(42L);
        assertThat(claims.getExpiration()).isAfter(Date.from(Instant.now()));
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-key-that-is-also-64-characters-long-for-hs512!!!".getBytes(StandardCharsets.UTF_8));
        String foreign = Jwts.builder()
                .subject("42")
                .issuer("pcc-identity-service")
                .audience().add("pcc-property-management").and()
                .claim("type", "access")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(otherKey)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAndValidate(foreign))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsExpiredToken() {
        String expired = Jwts.builder()
                .subject("42")
                .issuer("pcc-identity-service")
                .audience().add("pcc-property-management").and()
                .claim("type", "access")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseAndValidate(expired))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsWrongIssuer() {
        String wrongIssuer = Jwts.builder()
                .subject("42")
                .issuer("somebody-else")
                .audience().add("pcc-property-management").and()
                .claim("type", "access")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.parseAndValidate(wrongIssuer))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void refreshTokensAreOpaqueAndUnique() {
        String t1 = jwtService.generateRefreshTokenValue();
        String t2 = jwtService.generateRefreshTokenValue();
        assertThat(t1).isNotEqualTo(t2);
        assertThat(t1).hasSizeGreaterThan(40);
    }

    @Test
    void failsFastWhenSecretMissing() {
        SecurityProperties empty = new SecurityProperties();
        empty.getJwt().setSecret("");
        JwtService service = new JwtService(empty);
        assertThatThrownBy(service::init).isInstanceOf(IllegalStateException.class);
    }
}
