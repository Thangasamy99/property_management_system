package com.pcc.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "workflow_actions")
public class WorkflowAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id")
    private WorkflowInstance instance;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private ApprovalTask task;
    @Column(nullable = false, length = 30) private String action;
    private Integer levelOrder;
    @Column(nullable = false) private Long actorId;
    @Column(nullable = false, length = 120) private String actorName;
    @Column(length = 500) private String comment;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
