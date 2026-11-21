package com.pcc.workflow.dto.response;

import java.time.Instant;

public record TaskResponse(Long id, Long instanceId, String workflowRef, String workflowType,
                           String module, String entityType, String entityRef, String description,
                           int levelOrder, Long assigneeId, String assigneeRole, String status,
                           Long actedById, String actedByName, Instant actedAt, String comment,
                           Instant dueAt, boolean overdue, Instant createdAt) {}
