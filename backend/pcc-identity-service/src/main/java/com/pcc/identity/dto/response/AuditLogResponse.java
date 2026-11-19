package com.pcc.identity.dto.response;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        String details,
        Instant createdAt
) {
}
