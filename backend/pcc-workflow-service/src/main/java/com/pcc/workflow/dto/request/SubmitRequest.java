package com.pcc.workflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

public record SubmitRequest(
        @NotNull Long definitionId,
        @NotBlank String entityRef,
        BigDecimal amount,
        String priority,
        String description,
        List<String> documentRefs) {}
