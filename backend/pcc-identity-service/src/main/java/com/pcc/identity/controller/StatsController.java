package com.pcc.identity.controller;

import com.pcc.identity.dto.response.DashboardStatsResponse;
import com.pcc.identity.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Dashboard", description = "Identity service statistics (USER_VIEW required)")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "Dashboard statistics",
            description = "Employee counts, role count, login statistics and recent registrations. Requires USER_VIEW.")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<DashboardStatsResponse> dashboard() {
        return ResponseEntity.ok(statsService.dashboard());
    }

    @Operation(summary = "Activity time series",
            description = "Daily successful/failed logins and employee registrations for the last N days (1-90). Requires USER_VIEW.")
    @GetMapping("/activity")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<java.util.List<DashboardStatsResponse.ActivityPoint>> activity(
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(statsService.activity(days));
    }
}
