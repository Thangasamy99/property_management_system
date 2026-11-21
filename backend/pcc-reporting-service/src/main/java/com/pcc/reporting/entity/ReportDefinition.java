package com.pcc.reporting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "report_definitions")
public class ReportDefinition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 80) private String reportKey;
    @Column(nullable = false, length = 200) private String name;
    @Column(nullable = false, length = 20) private String domain;
    @Column(length = 500) private String description;
    @Column(nullable = false, length = 20) private String sourceService;
    @Column(nullable = false, length = 300) private String sourcePath;
    @Column(nullable = false, length = 60) private String permission;
    @Column(nullable = false, columnDefinition = "TEXT") private String columnsJson;
    @Column(nullable = false) private boolean exportable = true;
    @Column(nullable = false) private boolean active = true;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
