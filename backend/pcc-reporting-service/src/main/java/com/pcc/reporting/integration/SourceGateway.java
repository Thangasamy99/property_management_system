package com.pcc.reporting.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Authenticated HTTP reads against owning services (relayed JWT).
 * Every result carries freshness + health so callers never confuse
 * partial data with complete data.
 */
@Slf4j
@Component
public class SourceGateway {

    private final RestClient restClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final Map<String, String> bases;

    public record SourceResult(String service, boolean ok, JsonNode data,
                               String error, Instant fetchedAt, boolean cached) {
        static SourceResult ok(String s, JsonNode d) {
            return new SourceResult(s, true, d, null, Instant.now(), false);
        }
        static SourceResult cachedOk(String s, JsonNode d, Instant at) {
            return new SourceResult(s, true, d, null, at, true);
        }
        static SourceResult down(String s, String e) {
            return new SourceResult(s, false, null, e, Instant.now(), false);
        }
    }

    public SourceGateway(
            @Value("${pcc.reporting.identity-url}") String identity,
            @Value("${pcc.reporting.property-url}") String property,
            @Value("${pcc.reporting.land-url}") String land,
            @Value("${pcc.reporting.finance-url}") String finance,
            @Value("${pcc.reporting.document-url}") String document,
            @Value("${pcc.reporting.workflow-url}") String workflow,
            @Value("${pcc.reporting.connect-timeout-ms}") int connectMs,
            @Value("${pcc.reporting.read-timeout-ms}") int readMs) {
        this.bases = Map.of("IDENTITY", identity, "PROPERTY", property, "LAND", land,
                "FINANCE", finance, "DOCUMENT", document, "WORKFLOW", workflow);
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectMs));
        factory.setReadTimeout(Duration.ofMillis(readMs));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /** Fetch one path; never throws — health is carried in the result. */
    public SourceResult get(String service, String path) {
        return get(service, path, relayToken());
    }

    private SourceResult get(String service, String path, String auth) {
        String base = bases.get(service);
        if (base == null) return SourceResult.down(service, "unknown service");
        try {
            var spec = restClient.get().uri(base + path);
            if (auth != null) spec.header("Authorization", auth);
            // Jackson 2 read: Spring's converters only know Jackson 3 types.
            JsonNode node = mapper.readTree(spec.retrieve().body(String.class));
            return SourceResult.ok(service, node);
        } catch (Exception e) {
            String msg = e.getMessage();
            log.warn("Source {} {} failed: {}", service, path, msg);
            return SourceResult.down(service,
                    msg != null && msg.length() > 160 ? msg.substring(0, 160) : String.valueOf(msg));
        }
    }

    /** Fetch several calls in bounded parallel; order preserved.
     *  The JWT is captured on the calling thread — pool threads don't see
     *  RequestContextHolder. */
    public List<SourceResult> getAll(List<Spec> specs) {
        String auth = relayToken();
        List<Callable<SourceResult>> calls = specs.stream()
                .<Callable<SourceResult>>map(s -> () -> get(s.service(), s.path(), auth))
                .toList();
        try {
            return pool.invokeAll(calls).stream().map(f -> {
                try { return f.get(); } catch (Exception e) {
                    return SourceResult.down("?", e.getMessage());
                }
            }).toList();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        }
    }

    public record Spec(String service, String path) {}

    /**
     * Auto-paginate a paged endpoint: fetches pages until exhausted or the
     * row cap is hit. Returns a synthetic array node of all `content` rows,
     * or a failure result. `truncated=true` in error text when capped.
     */
    public SourceResult getAllRows(String service, String path, int pageSize, int maxRows) {
        var rows = mapper.createArrayNode();
        int page = 0;
        while (rows.size() < maxRows) {
            String sep = path.contains("?") ? "&" : "?";
            var r = get(service, path + sep + "page=" + page + "&size=" + pageSize);
            if (!r.ok()) return r;
            if (r.data().isArray() || (r.data().isObject()
                    && !r.data().path("content").isArray())) {
                // Finance summary/report endpoints may return a list or a
                // summary DTO rather than a Spring Page. Preserve that shape.
                return r;
            }
            JsonNode content = r.data().path("content");
            content.forEach(rows::add);
            if (page + 1 >= r.data().path("totalPages").asInt(1)) break;
            page++;
        }
        var result = SourceResult.ok(service, rows);
        if (rows.size() >= maxRows) {
            log.warn("Source {} {} truncated at {} rows", service, path, maxRows);
        }
        return result;
    }

    private String relayToken() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
            return a.getRequest().getHeader("Authorization");
        }
        return null;
    }
}
