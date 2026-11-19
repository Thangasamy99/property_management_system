package com.pcc.identity.dto.response;

import java.time.Instant;
import java.util.List;

public record DashboardStatsResponse(
        long totalEmployees,
        long activeEmployees,
        long inactiveEmployees,
        long totalRoles,
        long successfulLogins,
        long failedLogins,
        long lockedAccounts,
        List<RecentRegistration> recentRegistrations
) {
    public record RecentRegistration(
            Long id,
            String employeeCode,
            String username,
            String firstName,
            String lastName,
            Instant createdAt
    ) {
    }

    public record ActivityPoint(
            java.time.LocalDate date,
            long successfulLogins,
            long failedLogins,
            long registrations
    ) {
    }
}
