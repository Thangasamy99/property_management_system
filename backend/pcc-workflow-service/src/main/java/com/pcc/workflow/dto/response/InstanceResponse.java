package com.pcc.workflow.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record InstanceResponse(Long id, String workflowRef, Long definitionId, String definitionName,
                               String workflowType, String module, String entityType, String entityRef,
                               BigDecimal amount, String priority, String description,
                               java.util.List<String> documentRefs, String status, int currentLevel,
                               Long submittedById, String submittedByName, Instant submittedAt,
                               Instant dueAt, boolean overdue, Instant completedAt,
                               String integrationError, Instant createdAt, Instant updatedAt) {}
