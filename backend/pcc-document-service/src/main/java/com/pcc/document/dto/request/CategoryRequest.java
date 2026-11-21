package com.pcc.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CategoryRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]{2,40}$") String code,
        @NotBlank String name,
        @NotBlank @Pattern(regexp = "PROPERTY|LAND|FINANCE|ADMINISTRATION") String domain,
        String description,
        boolean requiresExpiry,
        boolean active) {}
