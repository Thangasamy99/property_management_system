package com.pcc.land.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OwnershipRequest(
        @NotNull Long parcelId,
        @NotBlank String ownerName,
        String ownerReference,
        String titleReference,
        @DecimalMin("0.01") @DecimalMax("100") BigDecimal sharePercent,
        LocalDate acquisitionDate,
        @NotNull LocalDate effectiveDate,
        String documentReference
) {}
