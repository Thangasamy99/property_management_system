package com.pcc.property.dto.request;

import com.pcc.property.enums.PropertyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PropertyStatusRequest(
        @NotNull PropertyStatus status,
        @Size(max = 500) String reason
) {}
