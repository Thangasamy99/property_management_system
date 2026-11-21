package com.pcc.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record IncomeRequest(
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate incomeDate,
        @NotBlank @Size(max = 30) String methodCode,
        @Size(max = 100) String externalReference,
        @Size(max = 120) String documentReference,
        @Size(max = 150) String payerName
) {}
