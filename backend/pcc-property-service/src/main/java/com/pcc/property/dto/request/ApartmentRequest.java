package com.pcc.property.dto.request;

import com.pcc.property.enums.ApartmentStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ApartmentRequest(
        @NotNull Long buildingId,
        @NotBlank @Size(max = 20) String apartmentNumber,
        @Min(0) Integer floorNumber,
        @Min(0) Integer bedrooms,
        @Min(0) Integer bathrooms,
        @Positive BigDecimal areaSqm,
        @PositiveOrZero BigDecimal monthlyRent,
        @Size(max = 4000) String description,
        ApartmentStatus status
) {}
