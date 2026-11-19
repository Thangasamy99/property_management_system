package com.pcc.identity.dto.response;

import java.time.Instant;
import java.util.Set;

public record RoleResponse(
        Long id,
        String code,
        String name,
        String description,
        Set<String> permissions,
        Instant createdAt,
        Instant updatedAt
) {
}
