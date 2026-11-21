package com.pcc.property.dto.request;

import com.pcc.property.enums.PaymentFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RentalAgreementRequest(
        @NotNull Long apartmentId,
        @NotNull Long tenantId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull @Positive BigDecimal monthlyRent,
        @PositiveOrZero BigDecimal deposit,
        @NotNull PaymentFrequency paymentFrequency,
        @NotNull @Min(1) @Max(28) Integer rentDueDay,
        List<@NotBlank String> terms
) {}
