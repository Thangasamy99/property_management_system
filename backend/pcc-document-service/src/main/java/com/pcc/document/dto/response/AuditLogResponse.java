package com.pcc.document.dto.response;

import java.time.Instant;

public record AuditLogResponse(Long id, String action, Long actorId, String actorName,
                               String detail, Instant createdAt) {}
