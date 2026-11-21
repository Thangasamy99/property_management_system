package com.pcc.land.dto.response;

import com.pcc.land.entity.LandOwnershipHistory;

import java.time.Instant;
import java.time.LocalDate;

public record OwnershipHistoryResponse(
        Long id,
        String eventType,
        String previousOwner,
        String newOwner,
        LocalDate effectiveDate,
        String performedByName,
        String note,
        Instant createdAt
) {
    public static OwnershipHistoryResponse from(LandOwnershipHistory h) {
        return new OwnershipHistoryResponse(h.getId(), h.getEventType().name(), h.getPreviousOwner(),
                h.getNewOwner(), h.getEffectiveDate(), h.getPerformedByName(), h.getNote(), h.getCreatedAt());
    }
}
