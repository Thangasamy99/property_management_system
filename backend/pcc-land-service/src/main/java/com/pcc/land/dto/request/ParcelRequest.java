package com.pcc.land.dto.request;

import com.pcc.land.enums.LandClassification;
import com.pcc.land.enums.MeasurementUnit;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ParcelRequest(
        @NotBlank String name,
        String description,
        @NotNull LandClassification classification,
        @NotNull @Positive BigDecimal areaValue,
        @NotNull MeasurementUnit unit,
        @NotBlank String region,
        String division,
        String subdivision,
        String locality,
        String address,
        LocalDate acquisitionDate
) {}
