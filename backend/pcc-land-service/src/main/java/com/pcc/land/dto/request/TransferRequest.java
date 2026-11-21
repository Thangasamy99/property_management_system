package com.pcc.land.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotNull Long parcelId,
        @NotBlank String newOwnerName,
        String newOwnerReference,
        Long saleId,
        String titleReference,
        String documentReference
) {}
