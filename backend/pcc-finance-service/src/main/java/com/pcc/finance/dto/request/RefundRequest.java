package com.pcc.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RefundRequest(
        @NotNull Long paymentId,
        @NotNull @Positive BigDecimal amount,
        @NotBlank @Size(max = 255) String reason
) {}
