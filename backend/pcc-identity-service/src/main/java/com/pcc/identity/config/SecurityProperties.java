package com.pcc.identity.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pcc.security")
public class SecurityProperties {

    private Jwt jwt = new Jwt();
    private Lockout lockout = new Lockout();
    private Cors cors = new Cors();
    private Cookie cookie = new Cookie();
    private Bootstrap bootstrap = new Bootstrap();
    private long passwordResetTokenTtlMinutes = 30;

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        private String issuer = "pcc-identity-service";
        private String audience = "pcc-property-management";
        private long accessTokenTtlMinutes = 15;
        private long refreshTokenTtlDays = 7;
    }

    @Getter
    @Setter
    public static class Lockout {
        private int maxFailedAttempts = 5;
        private long lockDurationMinutes = 15;
    }

    @Getter
    @Setter
    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");
    }

    @Getter
    @Setter
    public static class Cookie {
        private String refreshName = "pcc_refresh_token";
        private boolean secure;
        private String sameSite = "Strict";
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String adminUsername = "admin";
        private String adminEmail = "admin@pcc-cameroon.local";
        private String adminPassword;
        private String adminEmployeeCode = "PCC-0001";
        private String adminFirstName = "System";
        private String adminLastName = "Administrator";
    }
}
