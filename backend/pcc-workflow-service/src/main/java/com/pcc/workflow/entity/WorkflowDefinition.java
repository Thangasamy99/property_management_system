package com.pcc.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.List;

@Getter @Setter @Entity @Table(name = "workflow_definitions")
public class WorkflowDefinition {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 500) private String description;
    @Column(nullable = false, length = 20) private String module;   // PROPERTY|LAND|FINANCE|DOCUMENT
    @Column(nullable = false, length = 50) private String workflowType;
    @Column(nullable = false, length = 40) private String entityType;
    @Column(length = 8) private String integrationMethod;           // POST|PATCH|PUT|null
    @Column(length = 200) private String integrationPath;           // {id} placeholder
    private Integer escalationDays;
    @Column(nullable = false) private boolean active = true;
    private Long createdById;
    private Long updatedById;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(nullable = false) private Instant updatedAt = Instant.now();
    @OneToMany(mappedBy = "definition", fetch = FetchType.LAZY)
    @OrderBy("levelOrder")
    private List<WorkflowLevel> levels;
}
