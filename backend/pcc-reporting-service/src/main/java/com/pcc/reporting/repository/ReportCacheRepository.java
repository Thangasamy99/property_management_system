package com.pcc.reporting.repository;

import com.pcc.reporting.entity.ReportCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReportCacheRepository extends JpaRepository<ReportCacheEntry, Long> {
    Optional<ReportCacheEntry> findByCacheKey(String key);
    void deleteByCacheKeyStartingWith(String prefix);
}
