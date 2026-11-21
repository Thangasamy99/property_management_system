package com.pcc.reporting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "report_cache_entries")
public class ReportCacheEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 200) private String cacheKey;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT") private String payload;
    @Column(nullable = false, length = 20) private String sourceHealth;
    @Column(nullable = false) private Instant fetchedAt;
    @Column(nullable = false) private Instant expiresAt;
}
