package com.pcc.workflow.dto.response;

import java.time.Instant;

public record ActionResponse(Long id, String action, Integer levelOrder, Long actorId,
                             String actorName, String comment, Instant createdAt) {}
