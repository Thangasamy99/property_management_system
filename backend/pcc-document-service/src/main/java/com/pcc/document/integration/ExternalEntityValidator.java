package com.pcc.document.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.document.enums.EntityType;
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
 * Validates external entity references by calling the owning service over
 * authenticated HTTP (relayed JWT). No database coupling. Entities not backed
 * by a reachable endpoint return Optional.empty() — associations are allowed
 * as free-form references.
 */
@Slf4j
@Component
public class ExternalEntityValidator {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${pcc.integration.property-service-url}")
    private String propertyUrl;
    @Value("${pcc.integration.land-service-url}")
    private String landUrl;
    @Value("${pcc.integration.finance-service-url}")
    private String financeUrl;

    /**
     * Returns the validated entity summary node, or Optional.empty() when the
     * entity type has no validation endpoint (free-form ref allowed).
     * Throws ExternalServiceException on a definitive 404 / service failure.
     */
    public Optional<JsonNode> validate(EntityType type, String ref) {
        String target = lookupPath(type, ref);
        if (target == null) return Optional.empty();   // no validation available
        String base = baseFor(type);
        try {
            String body = restClient.get().uri(base + target)
                    .headers(h -> {
                        if (RequestContextHolder.getRequestAttributes()
                                instanceof ServletRequestAttributes attrs) {
                            String auth = attrs.getRequest().getHeader("Authorization");
                            if (auth != null) h.set("Authorization", auth);
                        }
                    })
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 404) throw new NotFound();
                        if (res.getStatusCode().value() == 401 || res.getStatusCode().value() == 403)
                            throw new NotFound();   // treat as not found for association validation
                        throw new RuntimeException("HTTP " + res.getStatusCode().value());
                    })
                    .body(String.class);
            return Optional.ofNullable(body == null ? null : mapper.readTree(body));
        } catch (NotFound nf) {
            throw new IllegalArgumentException("Unknown " + type + " reference: " + ref);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.warn("External validation GET {}{} failed: {}", base, target, e.getMessage());
            throw new IllegalStateException("Owning service unavailable — cannot validate " + type);
        }
    }

    private String baseFor(EntityType type) {
        return switch (type) {
            case PROPERTY, BUILDING, APARTMENT, RENTAL_AGREEMENT, TENANT -> propertyUrl;
            case LAND_PARCEL, LAND_SURVEY, LAND_ACQUISITION, LAND_SALE, LAND_TRANSFER -> landUrl;
            case FINANCE_PAYMENT, EXPENSE, RECEIPT -> financeUrl;
            case OTHER -> null;
        };
    }

    private String lookupPath(EntityType type, String ref) {
        return switch (type) {
            case PROPERTY -> "/api/v1/properties/" + ref;
            case BUILDING -> "/api/v1/buildings/" + ref;
            case APARTMENT -> "/api/v1/apartments/" + ref;
            case RENTAL_AGREEMENT -> "/api/v1/rental-agreements/" + ref;
            case TENANT -> "/api/v1/tenants/" + ref;
            case LAND_PARCEL -> "/api/v1/land-parcels/" + ref;
            case LAND_SURVEY -> "/api/v1/land-surveys/" + ref;
            case LAND_ACQUISITION -> "/api/v1/land-acquisitions/" + ref;
            case LAND_SALE -> "/api/v1/land-sales/" + ref;
            case FINANCE_PAYMENT -> "/api/v1/payments/" + ref;
            case EXPENSE -> "/api/v1/expenses/" + ref;
            case RECEIPT -> "/api/v1/receipts/" + ref;
            case LAND_TRANSFER, OTHER -> null;   // no lookup contract yet
        };
    }

    private static final class NotFound extends RuntimeException {}
}
