package com.pcc.workflow.dto.response;

import java.time.Instant;

public record OutboxResponse(Long id, String integrationRef, String service, String method,
                             String path, String status, int retryCount, Instant lastAttemptAt,
                             String lastError, Instant createdAt) {}
