package com.pcc.land.dto.response;

import com.pcc.land.entity.LandParcel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ParcelResponse(
        Long id,
        String parcelReference,
        String name,
        String description,
        String classification,
        String status,
        BigDecimal areaSqm,
        BigDecimal originalArea,
        String originalUnit,
        String region,
        String division,
        String subdivision,
        String locality,
        String address,
        LocalDate acquisitionDate,
        String createdByName,
        Instant createdAt
) {
    public static ParcelResponse from(LandParcel p) {
        return new ParcelResponse(p.getId(), p.getParcelReference(), p.getName(), p.getDescription(),
                p.getClassification().name(), p.getStatus().name(), p.getAreaSqm(), p.getOriginalArea(),
                p.getOriginalUnit() != null ? p.getOriginalUnit().name() : null,
                p.getRegion(), p.getDivision(), p.getSubdivision(), p.getLocality(), p.getAddress(),
                p.getAcquisitionDate(), p.getCreatedByName(), p.getCreatedAt());
    }
}
