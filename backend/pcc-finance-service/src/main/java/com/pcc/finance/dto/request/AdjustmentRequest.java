package com.pcc.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AdjustmentRequest(
        @NotNull Long accountId,
        @NotBlank String direction,        // CREDIT | DEBIT
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate transactionDate,
        @NotBlank @Size(max = 255) String description
) {}
