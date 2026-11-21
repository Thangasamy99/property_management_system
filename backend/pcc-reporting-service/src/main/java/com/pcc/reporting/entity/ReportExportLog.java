package com.pcc.reporting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "report_export_logs")
public class ReportExportLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String reportKey;
    @Column(nullable = false, length = 10) private String format;
    @Column(nullable = false, length = 200) private String filename;
    @Column(nullable = false) private Long exportedById;
    @Column(nullable = false, length = 120) private String exportedBy;
    private Integer rowCount;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
