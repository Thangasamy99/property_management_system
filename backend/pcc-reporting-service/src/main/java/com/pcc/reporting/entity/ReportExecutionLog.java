package com.pcc.reporting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "report_execution_logs")
public class ReportExecutionLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80) private String reportKey;
    @Column(nullable = false, length = 200) private String reportName;
    @Column(nullable = false) private Long executedById;
    @Column(nullable = false, length = 120) private String executedBy;
    @Column(columnDefinition = "TEXT") private String paramsJson;
    private Integer rowCount;
    @Column(length = 20) private String sourceHealth;
    private Long durationMs;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
