package com.pcc.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "finance_audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String entityType;

    @Column(nullable = false)
    private Long entityId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(length = 500)
    private String details;

    private Long actorId;

    @Column(length = 150)
    private String actorName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
