package com.pcc.land.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Finance Service contract: reports the total amount received. */
public record FinanceStatusRequest(
        @NotNull @PositiveOrZero BigDecimal paidAmount
) {}
