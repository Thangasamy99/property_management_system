package com.pcc.land.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Finance Service contract: records one payment allocation against a land target. */
public record FinanceAllocationRequest(
        @NotBlank @Size(max = 60) String paymentRef,
        @NotBlank @Size(max = 60) String allocationRef,
        @NotNull @Positive BigDecimal amount
) {}
