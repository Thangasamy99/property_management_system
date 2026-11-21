package com.pcc.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Shared create-payment payload for rent, land acquisition and land sale collections. */
public record PaymentRequest(
        @NotNull Long externalId,          // schedule id / acquisition id / sale id in the owning service
        Long parentId,                     // optional: agreement id / parcel id for verification context
        @NotBlank @Size(max = 150) String payerName,
        @Size(max = 80) String payerReference,
        @NotNull @Positive BigDecimal amount,
        @NotNull LocalDate paymentDate,
        @NotBlank @Size(max = 30) String methodCode,
        @Size(max = 100) String paymentReference,
        @Size(max = 255) String verificationDetail,
        @Size(max = 500) String notes,
        Long accountId,
        @Size(max = 80) String idempotencyKey
) {}
