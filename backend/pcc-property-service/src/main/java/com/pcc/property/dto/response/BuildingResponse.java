package com.pcc.property.dto.response;

import com.pcc.property.entity.Building;

import java.time.Instant;

public record BuildingResponse(
        Long id, String code, String name, Long propertyId, String propertyName, String propertyCode,
        Integer numberOfFloors, Integer constructionYear, String description,
        String status, Instant createdAt, Long apartmentCount
) {
    public static BuildingResponse of(Building b) {
        return of(b, null);
    }

    public static BuildingResponse of(Building b, Long apartmentCount) {
        return new BuildingResponse(b.getId(), b.getCode(), b.getName(),
                b.getProperty().getId(), b.getProperty().getName(), b.getProperty().getCode(),
                b.getNumberOfFloors(), b.getConstructionYear(), b.getDescription(),
                b.getStatus().name(), b.getCreatedAt(), apartmentCount);
    }
}
