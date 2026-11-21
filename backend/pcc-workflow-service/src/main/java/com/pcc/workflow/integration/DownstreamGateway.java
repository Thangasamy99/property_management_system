package com.pcc.workflow.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Authenticated HTTP calls to owning services (relayed JWT). Result carries
 * permanence so the dispatcher knows whether to retry.
 */
@Slf4j
@Component
public class DownstreamGateway {

    private final RestClient restClient = RestClient.create();

    @Value("${pcc.integration.property-service-url}")
    private String propertyUrl;
    @Value("${pcc.integration.land-service-url}")
    private String landUrl;
    @Value("${pcc.integration.finance-service-url}")
    private String financeUrl;
    @Value("${pcc.integration.document-service-url}")
    private String documentUrl;

    public record CallResult(boolean success, boolean permanent, String error) {
        static CallResult ok() { return new CallResult(true, false, null); }
        static CallResult permanent(String e) { return new CallResult(false, true, e); }
        static CallResult transientError(String e) { return new CallResult(false, false, e); }
    }

    public String baseFor(String service) {
        return switch (service) {
            case "PROPERTY" -> propertyUrl;
            case "LAND" -> landUrl;
            case "FINANCE" -> financeUrl;
            case "DOCUMENT" -> documentUrl;
            default -> throw new IllegalArgumentException("Unknown service: " + service);
        };
    }

    public boolean hasRelayToken() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a
                && a.getRequest().getHeader("Authorization") != null;
    }

    public CallResult invoke(String service, String method, String path, String body) {
        if (!hasRelayToken()) {
            return CallResult.transientError("No JWT available to relay (offline dispatch)");
        }
        String base = baseFor(service);
        try {
            var spec = switch (method) {
                case "PATCH" -> restClient.patch().uri(base + path);
                case "PUT" -> restClient.put().uri(base + path);
                default -> restClient.post().uri(base + path);
            };
            spec.headers(h -> {
                if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a) {
                    String auth = a.getRequest().getHeader("Authorization");
                    if (auth != null) h.set("Authorization", auth);
                }
            });
            if (body != null && !body.isBlank())
                spec.contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(body);
            spec.retrieve().toBodilessEntity();
            return CallResult.ok();
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            String msg = service + " rejected: HTTP " + code;
            log.warn("Integration {} {}{} -> {}", method, base, path, msg);
            return code >= 400 && code < 500 && code != 408 && code != 429
                    ? CallResult.permanent(msg)
                    : CallResult.transientError(msg);
        } catch (Exception e) {
            String msg = service + " unavailable: " + e.getMessage();
            log.warn("Integration {} {}{} failed (transient)", method, base, path);
            return CallResult.transientError(msg);
        }
    }
}
