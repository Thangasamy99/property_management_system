package com.pcc.property.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TerminateAgreementRequest(
        @NotBlank @Size(max = 500) String reason
) {}
