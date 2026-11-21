package com.pcc.property.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

/** Finance Service contract: reports the total amount received for this schedule. */
public record SchedulePaymentRequest(
        @NotNull @PositiveOrZero BigDecimal paidAmount
) {}
