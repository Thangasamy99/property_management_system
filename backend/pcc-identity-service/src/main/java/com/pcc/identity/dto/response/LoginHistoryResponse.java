package com.pcc.identity.dto.response;

import com.pcc.identity.enums.LoginEventType;

import java.time.Instant;

public record LoginHistoryResponse(
        Long id,
        String username,
        String employeeName,
        LoginEventType eventType,
        String ipAddress,
        String userAgent,
        String failureReason,
        Instant createdAt
) {
}
