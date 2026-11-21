package com.pcc.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank @Size(max = 60) String category,
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate expenseDate,
        @Size(max = 80) String department,
        @Size(max = 150) String vendorName,
        @Size(max = 80) String vendorReference,
        @Size(max = 120) String documentReference
) {}
