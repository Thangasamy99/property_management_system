package com.pcc.workflow.entity;

import com.pcc.workflow.enums.OutboxStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "workflow_integration_outbox")
public class IntegrationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id")
    private WorkflowInstance instance;
    @Column(nullable = false, length = 60)
    private String integrationRef;
    @Column(nullable = false, length = 20)
    private String service;
    @Column(nullable = false, length = 8)
    private String method;
    @Column(nullable = false, length = 200)
    private String path;
    @Column(columnDefinition = "TEXT")
    private String payload;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OutboxStatus status = OutboxStatus.PENDING;
    @Column(nullable = false)
    private int retryCount = 0;
    private Instant lastAttemptAt;
    @Column(length = 500)
    private String lastError;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
