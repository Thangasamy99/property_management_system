package com.pcc.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.reporting.entity.ReportDefinition;
import com.pcc.reporting.entity.ReportExecutionLog;
import com.pcc.reporting.exception.BusinessRuleException;
import com.pcc.reporting.exception.ResourceNotFoundException;
import com.pcc.reporting.integration.SourceGateway;
import com.pcc.reporting.report.ReportRegistry;
import com.pcc.reporting.repository.ReportDefinitionRepository;
import com.pcc.reporting.repository.ReportExecutionLogRepository;
import com.pcc.reporting.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Runs registered reports against owning services and projects their rows.
 * Never fabricates data — a failed source produces an empty report with
 * sourceHealth=DOWN, not silently zero totals.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SourceGateway gateway;
    private final ReportDefinitionRepository definitions;
    private final ReportExecutionLogRepository execLogs;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final int MAX_ROWS = 1000;

    public record SourceHealth(String service, String status, Instant fetchedAt,
                               String error, boolean cached) {}
    public record ColumnMeta(String key, String label, String type) {}
    public record ReportResult(String reportKey, String name, String domain,
                               List<ColumnMeta> columns, List<Map<String, Object>> rows,
                               Map<String, BigDecimal> totals, boolean truncated,
                               List<SourceHealth> sources, Instant generatedAt) {}

    @Transactional
    public ReportResult run(String key, Map<String, String> params, UserPrincipal actor) {
        long t0 = System.currentTimeMillis();
        ReportRegistry.Def def = ReportRegistry.find(key)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown report: " + key));

        String path = withParams(def.path(), params);
        var src = def.paged()
                ? gateway.getAllRows(def.service(), path, 100, MAX_ROWS)
                : gateway.get(def.service(), path);

        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, BigDecimal> totals = new HashMap<>();
        boolean truncated = false;

        if (src.ok() && src.data() != null) {
            JsonNode items = src.data().isArray() ? src.data()
                    : src.data().path("content").isArray() ? src.data().path("content")
                    : src.data();
            if (items.isArray()) {
                for (JsonNode n : items) rows.add(project(n, def.columns()));
            } else if (items.isObject()) {
                // summary endpoint → single row from numeric/string leaves
                rows.add(project(items, def.columns()));
            }
            truncated = rows.size() >= MAX_ROWS;
            totals = computeTotals(rows, def.columns());
        }

        String health = src.ok() ? (truncated ? "PARTIAL" : "OK") : "DOWN";
        logExec(def, actor, params, rows.size(), health, System.currentTimeMillis() - t0);

        var cols = def.columns().stream().map(c -> new ColumnMeta(c.key(), c.label(), c.type())).toList();
        var sources = List.of(new SourceHealth(def.service(), health, src.fetchedAt(),
                src.error(), src.cached()));
        return new ReportResult(def.key(), def.name(), def.domain(), cols, rows, totals,
                truncated, sources, Instant.now());
    }

    private String withParams(String path, Map<String, String> params) {
        if (params == null) return path;
        StringBuilder sb = new StringBuilder(path);
        params.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                sb.append(sb.indexOf("?") >= 0 ? "&" : "?").append(k).append("=").append(v);
            }
        });
        return sb.toString();
    }

    private Map<String, Object> project(JsonNode n, List<ReportRegistry.Col> cols) {
        Map<String, Object> row = new LinkedHashMap<>();
        for (var c : cols) {
            JsonNode v = n.get(c.key());
            if (v == null || v.isNull()) {
                row.put(c.key(), null);
            } else if (v.isValueNode()) {
                row.put(c.key(), v.isNumber() ? v.numberValue() : v.asText());
            } else {
                row.put(c.key(), v.toString());
            }
        }
        return row;
    }

    private Map<String, BigDecimal> computeTotals(List<Map<String, Object>> rows,
                                                  List<ReportRegistry.Col> cols) {
        Map<String, BigDecimal> totals = new HashMap<>();
        for (var c : cols) {
            if (!"money".equals(c.type()) && !"int".equals(c.type())) continue;
            BigDecimal sum = BigDecimal.ZERO;
            boolean any = false;
            for (var r : rows) {
                Object v = r.get(c.key());
                if (v instanceof Number num) { sum = sum.add(new BigDecimal(num.toString())); any = true; }
            }
            if (any) totals.put(c.key(), sum);
        }
        return totals;
    }

    private void logExec(ReportRegistry.Def def, UserPrincipal actor,
                         Map<String, String> params, int rows, String health, long ms) {
        try {
            ReportExecutionLog l = new ReportExecutionLog();
            l.setReportKey(def.key());
            l.setReportName(def.name());
            l.setExecutedById(actor.getId());
            l.setExecutedBy(actor.getUsername());
            l.setParamsJson(params == null ? null : mapper.writeValueAsString(params));
            l.setRowCount(rows);
            l.setSourceHealth(health);
            l.setDurationMs(ms);
            execLogs.save(l);
        } catch (Exception ignored) { /* never break a report over logging */ }
    }
}
