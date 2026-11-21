package com.pcc.land.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TransferCompleteRequest(@NotNull LocalDate legalCompletionDate) {}
