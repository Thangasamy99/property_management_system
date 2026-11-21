package com.pcc.property.dto.response;

import java.time.Instant;

public record HistoryEntryResponse(
        String event, String fromStatus, String toStatus, String note,
        String actorUsername, Instant createdAt
) {}
