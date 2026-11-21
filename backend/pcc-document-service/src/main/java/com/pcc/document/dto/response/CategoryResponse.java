package com.pcc.document.dto.response;

public record CategoryResponse(Long id, String code, String name, String domain,
                               String description, boolean requiresExpiry, boolean active) {}
