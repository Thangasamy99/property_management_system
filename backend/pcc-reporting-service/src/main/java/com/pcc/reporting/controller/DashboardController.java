package com.pcc.reporting.controller;

import com.pcc.reporting.service.DashboardService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/v1/dashboard", produces = "application/json")
@RequiredArgsConstructor
@Tag(name = "Reporting Dashboards")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/executive")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public String executive(@RequestParam(defaultValue = "month") String period,
                                @RequestParam(required = false) String from,
                                @RequestParam(required = false) String to) {
        return String.valueOf(service.executive(period, from, to));
    }

    @GetMapping("/property")
    @PreAuthorize("hasAuthority('PROPERTY_REPORT_VIEW')")
    public String property() {
        return String.valueOf(service.domain("PROPERTY", null, null, null));
    }

    @GetMapping("/land")
    @PreAuthorize("hasAuthority('LAND_REPORT_VIEW')")
    public String land() {
        return String.valueOf(service.domain("LAND", null, null, null));
    }

    @GetMapping("/finance")
    @PreAuthorize("hasAuthority('FINANCIAL_REPORT_VIEW')")
    public String finance(@RequestParam(defaultValue = "month") String period,
                              @RequestParam(required = false) String from,
                              @RequestParam(required = false) String to) {
        return String.valueOf(service.domain("FINANCE", period, from, to));
    }

    @GetMapping("/workflow")
    @PreAuthorize("hasAuthority('WORKFLOW_REPORT_VIEW')")
    public String workflow() {
        return String.valueOf(service.domain("WORKFLOW", null, null, null));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('DOCUMENT_REPORT_VIEW')")
    public String documents() {
        return String.valueOf(service.domain("DOCUMENT", null, null, null));
    }
}
