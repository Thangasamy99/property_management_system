package com.pcc.workflow.dto.response;

import java.time.Instant;
import java.util.List;

public record DefinitionResponse(Long id, String code, String name, String description,
                                 String module, String workflowType, String entityType,
                                 String integrationMethod, String integrationPath,
                                 Integer escalationDays, boolean active,
                                 List<LevelDto> levels, Instant createdAt) {
    public record LevelDto(Long id, int levelOrder, String name, String approverRole,
                           Long approverId, java.math.BigDecimal minAmount, boolean mandatory) {}
}
