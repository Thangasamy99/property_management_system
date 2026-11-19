package com.pcc.identity.dto.request;

import com.pcc.identity.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UserStatusUpdateRequest(
        @NotNull(message = "Status is required (ACTIVE or INACTIVE)")
        AccountStatus status
) {
}
