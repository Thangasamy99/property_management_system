package com.pcc.property.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record RenewAgreementRequest(
        @NotNull @Future LocalDate newEndDate
) {}
