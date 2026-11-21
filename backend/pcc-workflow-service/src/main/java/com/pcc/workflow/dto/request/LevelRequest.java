package com.pcc.workflow.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record LevelRequest(@Min(1) int levelOrder, @NotBlank String name,
                           String approverRole, Long approverId,
                           BigDecimal minAmount, Boolean mandatory) {}
