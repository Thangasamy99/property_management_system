package com.pcc.property.dto.response;

import com.pcc.property.entity.Property;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record PropertyResponse(
        Long id, String code, String name, String propertyType, String description,
        AddressResponse address, LocalDate acquisitionDate, BigDecimal acquisitionCost,
        String status, String createdByName, Instant createdAt, Long buildingCount
) {
    public static PropertyResponse of(Property p) {
        return of(p, null);
    }

    public static PropertyResponse of(Property p, Long buildingCount) {
        AddressResponse addr = null;
        if (p.getAddress() != null) {
            var a = p.getAddress();
            addr = new AddressResponse(a.getLine1(), a.getLine2(), a.getCity(), a.getRegion(), a.getCountry());
        }
        return new PropertyResponse(p.getId(), p.getCode(), p.getName(), p.getPropertyType().name(),
                p.getDescription(), addr, p.getAcquisitionDate(), p.getAcquisitionCost(),
                p.getStatus().name(), p.getCreatedByName(), p.getCreatedAt(), buildingCount);
    }
}
