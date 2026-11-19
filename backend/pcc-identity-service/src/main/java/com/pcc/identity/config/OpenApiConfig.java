package com.pcc.identity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pccOpenApi() {
        String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("PCC Identity Service API")
                        .version("v1")
                        .description("""
                                Authentication & User Management service for the \
                                PCC Cameroon Property & Land Management System.

                                - Login with **username or email** + password.
                                - Access token: Bearer JWT (short-lived).
                                - Refresh token: HttpOnly cookie `pcc_refresh_token` (also accepted in request body).
                                - Permissions enforced server-side via Spring Security authorities.""")
                        .contact(new Contact().name("PCC Cameroon IT")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
