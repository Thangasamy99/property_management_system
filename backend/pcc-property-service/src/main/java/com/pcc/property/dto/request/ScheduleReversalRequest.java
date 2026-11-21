package com.pcc.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Finance Service contract: reverses part/all of a previously synced allocation. */
public record ScheduleReversalRequest(
        @NotBlank @Size(max = 60) String paymentRef,
        @NotBlank @Size(max = 60) String allocationRef,
        @NotBlank @Size(max = 60) String reversalRef,
        @NotNull @Positive BigDecimal amount,
        LocalDate reversalDate,
        @Size(max = 255) String reason,
        @Size(max = 120) String actor
) {}
