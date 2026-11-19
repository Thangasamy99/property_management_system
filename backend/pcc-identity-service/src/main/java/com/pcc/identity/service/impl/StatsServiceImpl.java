package com.pcc.identity.service.impl;

import com.pcc.identity.dto.response.DashboardStatsResponse;
import com.pcc.identity.enums.AccountStatus;
import com.pcc.identity.enums.LoginEventType;
import com.pcc.identity.repository.LoginHistoryRepository;
import com.pcc.identity.repository.RoleRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final LoginHistoryRepository loginHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboard() {
        long total = userRepository.count();
        long active = userRepository.countByAccountStatus(AccountStatus.ACTIVE);
        var recent = userRepository.findTop5ByOrderByCreatedAtDesc().stream()
                .map(u -> new DashboardStatsResponse.RecentRegistration(
                        u.getId(), u.getEmployeeCode(), u.getUsername(),
                        u.getFirstName(), u.getLastName(), u.getCreatedAt()))
                .toList();
        return new DashboardStatsResponse(
                total,
                active,
                total - active,
                roleRepository.count(),
                loginHistoryRepository.countByEventType(LoginEventType.LOGIN_SUCCESS),
                loginHistoryRepository.countByEventType(LoginEventType.LOGIN_FAILED),
                loginHistoryRepository.countByEventType(LoginEventType.ACCOUNT_LOCKED),
                recent);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<DashboardStatsResponse.ActivityPoint> activity(int days) {
        int window = Math.min(Math.max(days, 1), 90);
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        java.time.LocalDate start = today.minusDays(window - 1L);
        var since = start.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();

        java.util.Map<java.time.LocalDate, long[]> logins = new java.util.HashMap<>();
        for (Object[] row : loginHistoryRepository.countByDayAndType(since)) {
            java.time.LocalDate day = (java.time.LocalDate) row[0];
            LoginEventType type = (LoginEventType) row[1];
            long[] bucket = logins.computeIfAbsent(day, d -> new long[2]);
            if (type == LoginEventType.LOGIN_SUCCESS) bucket[0] += (Long) row[2];
            if (type == LoginEventType.LOGIN_FAILED) bucket[1] += (Long) row[2];
        }
        java.util.Map<java.time.LocalDate, Long> regs = new java.util.HashMap<>();
        for (Object[] row : userRepository.countByDaySince(since)) {
            regs.put((java.time.LocalDate) row[0], (Long) row[1]);
        }

        java.util.List<DashboardStatsResponse.ActivityPoint> points = new java.util.ArrayList<>();
        for (java.time.LocalDate d = start; !d.isAfter(today); d = d.plusDays(1)) {
            long[] b = logins.getOrDefault(d, new long[2]);
            points.add(new DashboardStatsResponse.ActivityPoint(d, b[0], b[1], regs.getOrDefault(d, 0L)));
        }
        return points;
    }
}
