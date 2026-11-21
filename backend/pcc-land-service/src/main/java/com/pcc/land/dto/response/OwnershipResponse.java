package com.pcc.land.dto.response;

import com.pcc.land.entity.LandOwnership;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record OwnershipResponse(
        Long id,
        Long parcelId,
        String parcelReference,
        String parcelName,
        String ownershipReference,
        String ownerName,
        String ownerReference,
        String titleReference,
        BigDecimal sharePercent,
        LocalDate acquisitionDate,
        LocalDate effectiveDate,
        LocalDate endDate,
        String status,
        String documentReference,
        String createdByName,
        Instant createdAt
) {
    public static OwnershipResponse from(LandOwnership o) {
        return new OwnershipResponse(o.getId(), o.getParcel().getId(), o.getParcel().getParcelReference(),
                o.getParcel().getName(), o.getOwnershipReference(), o.getOwnerName(), o.getOwnerReference(),
                o.getTitleReference(), o.getSharePercent(), o.getAcquisitionDate(), o.getEffectiveDate(),
                o.getEndDate(), o.getStatus().name(), o.getDocumentReference(), o.getCreatedByName(), o.getCreatedAt());
    }
}
