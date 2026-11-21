package com.pcc.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "workflow_audit_logs")
public class WorkflowAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id")
    private WorkflowInstance instance;
    @Column(nullable = false, length = 30)
    private String action;
    @Column(nullable = false)
    private Long actorId;
    @Column(nullable = false, length = 120)
    private String actorName;
    @Column(length = 500)
    private String detail;
    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
