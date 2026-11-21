package com.pcc.finance.dto.response;

import com.pcc.finance.entity.SyncOutbox;

import java.time.Instant;

public record SyncRecordResponse(
        Long id,
        String syncRef,
        String syncKind,
        String externalService,
        String targetType,
        Long targetId,
        String payload,
        String status,
        boolean permanent,
        int retryCount,
        Instant lastAttemptAt,
        String lastError,
        Instant createdAt,
        Instant updatedAt
) {
    public static SyncRecordResponse of(SyncOutbox o) {
        return new SyncRecordResponse(
                o.getId(), o.getSyncRef(), o.getSyncKind().name(), o.getExternalService(),
                o.getTargetType(), o.getTargetId(), o.getPayload(), o.getStatus().name(),
                o.isPermanent(), o.getRetryCount(), o.getLastAttemptAt(), o.getLastError(),
                o.getCreatedAt(), o.getUpdatedAt());
    }
}
