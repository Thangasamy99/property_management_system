package com.pcc.identity.mapper;

import com.pcc.identity.dto.response.LoginHistoryResponse;
import com.pcc.identity.entity.LoginHistory;

public final class LoginHistoryMapper {

    private LoginHistoryMapper() {
    }

    public static LoginHistoryResponse toResponse(LoginHistory entry) {
        return new LoginHistoryResponse(
                entry.getId(),
                entry.getUsername(),
                null,
                entry.getEventType(),
                entry.getIpAddress(),
                entry.getUserAgent(),
                entry.getFailureReason(),
                entry.getCreatedAt()
        );
    }
}
