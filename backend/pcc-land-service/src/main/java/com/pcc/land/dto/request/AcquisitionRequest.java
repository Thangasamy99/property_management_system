package com.pcc.land.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AcquisitionRequest(
        @NotNull Long parcelId,
        @NotBlank String sellerName,
        String sellerReference,
        String sellerContact,
        LocalDate acquisitionDate,
        @Positive BigDecimal purchasePrice,
        String documentReference
) {}
