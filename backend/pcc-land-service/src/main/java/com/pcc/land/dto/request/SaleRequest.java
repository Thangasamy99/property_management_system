package com.pcc.land.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleRequest(
        @NotNull Long parcelId,
        @NotBlank String buyerName,
        String buyerReference,
        String buyerContact,
        @NotNull @Positive BigDecimal proposedPrice,
        String agreementReference,
        LocalDate agreementDate,
        String documentReference
) {}
