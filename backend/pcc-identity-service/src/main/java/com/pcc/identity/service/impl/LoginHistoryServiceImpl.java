package com.pcc.identity.service.impl;

import com.pcc.identity.dto.response.LoginHistoryResponse;
import com.pcc.identity.dto.response.PageResponse;
import com.pcc.identity.entity.LoginHistory;
import com.pcc.identity.enums.LoginEventType;
import com.pcc.identity.mapper.LoginHistoryMapper;
import com.pcc.identity.repository.LoginHistoryRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginHistoryServiceImpl implements LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String username, LoginEventType eventType,
                       String ipAddress, String userAgent, String failureReason) {
        LoginHistory entry = new LoginHistory();
        entry.setUserId(userId);
        entry.setUsername(username);
        entry.setEventType(eventType);
        entry.setIpAddress(ipAddress);
        entry.setUserAgent(userAgent != null && userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent);
        entry.setFailureReason(failureReason);
        loginHistoryRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<LoginHistoryResponse> search(String username, LoginEventType eventType,
                                                     Instant from, Instant to, Pageable pageable) {
        String pattern = (username == null || username.isBlank())
                ? null : "%" + username.trim().toLowerCase() + "%";
        var page = loginHistoryRepository.search(pattern, eventType, from, to, pageable);

        // Resolve display names for the users on this page (single lookup query)
        var ids = page.getContent().stream().map(LoginHistory::getUserId).filter(Objects::nonNull).distinct().toList();
        var names = userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getFirstName() + " " + u.getLastName()));

        return PageResponse.from(page, h -> {
            var r = LoginHistoryMapper.toResponse(h);
            return new LoginHistoryResponse(r.id(), r.username(), names.get(h.getUserId()),
                    r.eventType(), r.ipAddress(), r.userAgent(), r.failureReason(), r.createdAt());
        });
    }
}
