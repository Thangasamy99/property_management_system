package com.pcc.finance.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.finance.exception.ExternalServiceException;
import com.pcc.finance.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;

/**
 * Calls Property and Land services, relaying the current user's JWT so the
 * upstream service applies its own permission checks. Integration failures are
 * explicit: a 404 becomes "invalid external reference", everything else a 503.
 */
@Slf4j
@Component
public class ServiceGateway {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${pcc.integration.property-service-url:http://localhost:8082}")
    private String propertyUrl;

    @Value("${pcc.integration.land-service-url:http://localhost:8083}")
    private String landUrl;

    public Optional<JsonNode> propertyGet(String path) {
        return get(propertyUrl, path, "Property Service");
    }

    public Optional<JsonNode> landGet(String path) {
        return get(landUrl, path, "Land Service");
    }

    /** Best-effort downstream status sync; failures are logged, never thrown. */
    public void propertyPatchQuietly(String path, Map<String, Object> body) {
        patchQuietly(propertyUrl, path, body, "Property Service");
    }

    public void landPatchQuietly(String path, Map<String, Object> body) {
        patchQuietly(landUrl, path, body, "Land Service");
    }

    private Optional<JsonNode> get(String baseUrl, String path, String serviceName) {
        try {
            String body = restClient.get()
                    .uri(baseUrl + path)
                    .headers(h -> relayAuthorization(h))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 404) {
                            throw new NotFoundSignal();
                        }
                        throw new ExternalServiceException(serviceName + " rejected request: HTTP "
                                + res.getStatusCode().value());
                    })
                    .body(String.class);
            return Optional.ofNullable(body == null ? null : mapper.readTree(body));
        } catch (NotFoundSignal nf) {
            return Optional.empty();
        } catch (ExternalServiceException e) {
            throw e;
        } catch (Exception e) {
            log.warn("{} call failed: GET {}{} -> {}", serviceName, baseUrl, path, e.getMessage());
            throw new ExternalServiceException(serviceName + " is unavailable");
        }
    }

    private void patchQuietly(String baseUrl, String path, Map<String, Object> body, String serviceName) {
        try {
            restClient.patch()
                    .uri(baseUrl + path)
                    .headers(h -> relayAuthorization(h))
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("{} sync failed: PATCH {}{} -> {}", serviceName, baseUrl, path, e.getMessage());
        }
    }

    private void relayAuthorization(org.springframework.http.HttpHeaders headers) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            String auth = attrs.getRequest().getHeader("Authorization");
            if (auth != null) headers.set("Authorization", auth);
        }
    }

    private static final class NotFoundSignal extends RuntimeException {}
}
