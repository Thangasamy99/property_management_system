package com.pcc.reporting.controller;

import com.pcc.reporting.exception.BusinessRuleException;
import com.pcc.reporting.exception.ForbiddenOperationException;
import com.pcc.reporting.exception.ResourceNotFoundException;
import com.pcc.reporting.report.ReportRegistry;
import com.pcc.reporting.repository.ReportDefinitionRepository;
import com.pcc.reporting.repository.ReportExecutionLogRepository;
import com.pcc.reporting.security.UserPrincipal;
import com.pcc.reporting.service.CacheService;
import com.pcc.reporting.service.ExportService;
import com.pcc.reporting.service.ReportService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;
    private final ExportService exportService;
    private final CacheService cache;
    private final ReportDefinitionRepository definitions;
    private final ReportExecutionLogRepository execLogs;

    /** Definitions the caller's permissions allow them to see. */
    @GetMapping("/definitions")
    @PreAuthorize("hasAuthority('REPORTING_VIEW')")
    public List<Map<String, Object>> definitions(@RequestParam(required = false) String domain,
                                                 @AuthenticationPrincipal UserPrincipal actor) {
        var defs = domain == null
                ? ReportRegistry.DEFS : ReportRegistry.byDomain(domain);
        return defs.stream()
                .filter(d -> has(actor, d.permission()))
                .map(d -> {
                    Map<String, Object> m = new HashMap<String, Object>();
                    m.put("key", d.key()); m.put("name", d.name());
                    m.put("domain", d.domain()); m.put("exportable", d.exportable());
                    m.put("columns", d.columns());
                    return m;
                }).toList();
    }

    /** Run a report: /reports/{domain}/{key} or /reports/run?key=... */
    @GetMapping("/{domain}/{key}")
    @PreAuthorize("hasAuthority('REPORTING_VIEW')")
    public ReportService.ReportResult run(@PathVariable String domain,
                                          @PathVariable String key,
                                          @RequestParam Map<String, String> params,
                                          @AuthenticationPrincipal UserPrincipal actor) {
        var def = ReportRegistry.find(key)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown report: " + key));
        if (!has(actor, def.permission()))
            throw new ForbiddenOperationException("You do not have access to this report");
        params = new HashMap<>(params);
        params.remove("domain"); params.remove("key");
        return reportService.run(key, params, actor);
    }

    @GetMapping("/{key}/export")
    @PreAuthorize("hasAuthority('REPORT_EXPORT')")
    public ResponseEntity<byte[]> export(@PathVariable String key,
                                         @RequestParam(defaultValue = "CSV") String format,
                                         @RequestParam Map<String, String> params,
                                         @AuthenticationPrincipal UserPrincipal actor) {
        var def = ReportRegistry.find(key)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown report: " + key));
        if (!def.exportable()) throw new BusinessRuleException("Report is not exportable");
        if (!has(actor, def.permission()))
            throw new ForbiddenOperationException("You do not have access to this report");
        params = new HashMap<>(params);
        params.remove("format");
        var result = reportService.run(key, params, actor);
        var out = exportService.export(result, format, params, actor);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + out.filename() + "\"")
                .contentType(MediaType.parseMediaType(out.contentType()))
                .body(out.bytes());
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAuthority('REPORT_REFRESH')")
    public Map<String, String> refresh() {
        cache.invalidate("");
        return Map.of("status", "cache cleared");
    }

    @GetMapping("/execution-history")
    @PreAuthorize("hasAuthority('REPORTING_VIEW')")
    public Object executionHistory(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return execLogs.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
    }

    private boolean has(UserPrincipal actor, String perm) {
        return actor.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(perm));
    }
}
