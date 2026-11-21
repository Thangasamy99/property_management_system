package com.pcc.property.dto.request;

import com.pcc.property.enums.PropertyType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PropertyRequest(
        @NotBlank @Size(max = 150) String name,
        @NotNull PropertyType propertyType,
        @Size(max = 4000) String description,
        @PastOrPresent LocalDate acquisitionDate,
        @PositiveOrZero BigDecimal acquisitionCost,
        @Size(max = 200) String addressLine1,
        @Size(max = 200) String addressLine2,
        @Size(max = 100) String city,
        @Size(max = 100) String region,
        @Size(max = 100) String country
) {}
