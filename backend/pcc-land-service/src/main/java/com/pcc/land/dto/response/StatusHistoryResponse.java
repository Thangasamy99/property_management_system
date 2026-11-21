package com.pcc.land.dto.response;

import com.pcc.land.entity.LandStatusHistory;

import java.time.Instant;

public record StatusHistoryResponse(
        Long id,
        String previousStatus,
        String newStatus,
        String reason,
        String changedByName,
        Instant changedAt
) {
    public static StatusHistoryResponse from(LandStatusHistory h) {
        return new StatusHistoryResponse(h.getId(),
                h.getPreviousStatus() != null ? h.getPreviousStatus().name() : null,
                h.getNewStatus().name(), h.getReason(), h.getChangedByName(), h.getChangedAt());
    }
}
