package com.pcc.workflow.dto.response;

import java.time.Instant;
import java.time.LocalDate;

public record DelegationResponse(Long id, Long delegatorId, Long delegateId, String delegateName,
                                 String module, Long definitionId, LocalDate startDate,
                                 LocalDate endDate, String reason, String status,
                                 Long approvedById, Instant createdAt) {}
