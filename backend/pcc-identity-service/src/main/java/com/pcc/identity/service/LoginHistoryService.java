package com.pcc.identity.service;

import com.pcc.identity.dto.response.LoginHistoryResponse;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.enums.LoginEventType;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface LoginHistoryService {

    void record(Long userId, String username, LoginEventType eventType,
                String ipAddress, String userAgent, String failureReason);

    PageResponse<LoginHistoryResponse> search(String username, LoginEventType eventType,
                                              Instant from, Instant to, Pageable pageable);
}
