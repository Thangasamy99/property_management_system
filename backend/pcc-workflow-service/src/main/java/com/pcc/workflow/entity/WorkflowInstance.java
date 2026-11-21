package com.pcc.workflow.entity;

import com.pcc.workflow.enums.WfStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "workflow_instances")
public class WorkflowInstance {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20) private String workflowRef;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id")
    private WorkflowDefinition definition;
    @Column(nullable = false, length = 50) private String workflowType;
    @Column(nullable = false, length = 20) private String module;
    @Column(nullable = false, length = 40) private String entityType;
    @Column(nullable = false, length = 80) private String entityRef;
    private BigDecimal amount;
    @Column(nullable = false, length = 10) private String priority = "NORMAL";
    @Column(length = 500) private String description;
    @Column(length = 500) private String documentRefs;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25) private WfStatus status = WfStatus.DRAFT;
    @Column(nullable = false) private int currentLevel = 0;
    @Column(nullable = false) private Long submittedById;
    @Column(nullable = false, length = 120) private String submittedByName;
    private Instant submittedAt;
    private Instant dueAt;
    private Instant completedAt;
    @Column(length = 500) private String integrationError;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();
    @Version private long version;
}
