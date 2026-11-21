package com.pcc.workflow.dto.response;

import java.time.Instant;

public record AuditResponse(Long id, String action, Long actorId, String actorName,
                            String detail, Instant createdAt) {}
