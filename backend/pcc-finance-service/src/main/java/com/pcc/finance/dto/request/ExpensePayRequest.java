package com.pcc.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ExpensePayRequest(
        @NotBlank @Size(max = 30) String methodCode,
        @Size(max = 100) String paymentReference,
        @NotNull LocalDate paidDate,
        Long accountId
) {}
