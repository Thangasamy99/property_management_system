package com.pcc.workflow.entity;

import com.pcc.workflow.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter @Setter @Entity @Table(name = "workflow_approval_tasks")
public class ApprovalTask {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instance_id")
    private WorkflowInstance instance;
    @Column(nullable = false) private int levelOrder;
    private Long assigneeId;
    @Column(length = 40) private String assigneeRole;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15) private TaskStatus status = TaskStatus.PENDING;
    private Long actedById;
    @Column(length = 120) private String actedByName;
    private Instant actedAt;
    @Column(length = 500) private String comment;
    private Instant dueAt;
    private Long delegatedFromId;
    @Column(nullable = false) private int cycle;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Version private long version;
}
