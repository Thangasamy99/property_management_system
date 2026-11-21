package com.pcc.land.dto.request;

import com.pcc.land.enums.LandStatus;
import jakarta.validation.constraints.NotNull;

public record ParcelStatusRequest(
        @NotNull LandStatus status,
        String reason
) {}
