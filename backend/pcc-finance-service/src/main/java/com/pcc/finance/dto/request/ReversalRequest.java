package com.pcc.finance.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReversalRequest(
        @NotNull Long paymentId,
        @NotBlank @Size(max = 255) String reason
) {}
