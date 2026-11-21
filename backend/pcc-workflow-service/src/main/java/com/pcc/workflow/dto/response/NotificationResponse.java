package com.pcc.workflow.dto.response;

import java.time.Instant;

public record NotificationResponse(Long id, String type, String title, String message,
                                   String workflowRef, boolean readFlag, Instant createdAt) {}
