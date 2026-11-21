package com.pcc.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @Entity @Table(name = "workflow_levels")
public class WorkflowLevel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "definition_id")
    private WorkflowDefinition definition;
    @Column(nullable = false) private int levelOrder;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 40) private String approverRole;
    private Long approverId;
    private BigDecimal minAmount;
    @Column(nullable = false) private boolean mandatory = true;
}
