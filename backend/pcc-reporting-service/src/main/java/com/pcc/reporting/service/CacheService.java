package com.pcc.reporting.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.reporting.entity.ReportCacheEntry;
import com.pcc.reporting.repository.ReportCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/** DB-backed cache for expensive aggregations. Entries carry the fetch
 *  timestamp so callers can label data as fresh or stale. */
@Service
@RequiredArgsConstructor
public class CacheService {

    private final ReportCacheRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${pcc.reporting.cache-ttl-seconds:120}")
    private long ttlSeconds;

    public record Cached(JsonNode payload, String health, Instant fetchedAt, boolean stale) {}

    @Transactional(readOnly = true)
    public Optional<Cached> get(String key) {
        return repo.findByCacheKey(key).map(e -> {
            try {
                boolean stale = e.getExpiresAt().isBefore(Instant.now());
                return new Cached(mapper.readTree(e.getPayload()), e.getSourceHealth(),
                        e.getFetchedAt(), stale);
            } catch (Exception ex) {
                return null;
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void put(String key, JsonNode payload, String health) {
        ReportCacheEntry e = repo.findByCacheKey(key).orElseGet(ReportCacheEntry::new);
        e.setCacheKey(key);
        try {
            e.setPayload(mapper.writeValueAsString(payload));
        } catch (Exception ex) {
            return;
        }
        e.setSourceHealth(health);
        e.setFetchedAt(Instant.now());
        e.setExpiresAt(Instant.now().plus(ttlSeconds, ChronoUnit.SECONDS));
        repo.save(e);
    }

    @Transactional
    public void invalidate(String prefix) {
        repo.deleteByCacheKeyStartingWith(prefix);
    }
}
