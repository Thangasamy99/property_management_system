package com.pcc.workflow.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DelegationRequest(@NotNull Long delegateId, String delegateName,
                                String module, Long definitionId,
                                @NotNull LocalDate startDate, @NotNull LocalDate endDate,
                                String reason) {}
