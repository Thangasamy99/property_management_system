package com.pcc.property.dto.response;

import com.pcc.property.entity.Apartment;

import java.math.BigDecimal;
import java.time.Instant;

public record ApartmentResponse(
        Long id, String apartmentNumber, Integer floorNumber, Integer bedrooms, Integer bathrooms,
        BigDecimal areaSqm, BigDecimal monthlyRent, String status, String description,
        Long buildingId, String buildingName, Long propertyId, String propertyName, Instant createdAt
) {
    public static ApartmentResponse of(Apartment a) {
        var b = a.getBuilding();
        return new ApartmentResponse(a.getId(), a.getApartmentNumber(), a.getFloorNumber(),
                a.getBedrooms(), a.getBathrooms(), a.getAreaSqm(), a.getMonthlyRent(),
                a.getStatus().name(), a.getDescription(),
                b.getId(), b.getName(), b.getProperty().getId(), b.getProperty().getName(),
                a.getCreatedAt());
    }
}
