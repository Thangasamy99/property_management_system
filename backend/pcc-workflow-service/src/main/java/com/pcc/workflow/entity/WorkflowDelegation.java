package com.pcc.workflow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @Entity @Table(name = "workflow_delegations")
public class WorkflowDelegation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private Long delegatorId;
    @Column(nullable = false) private Long delegateId;
    @Column(nullable = false, length = 120) private String delegateName;
    @Column(length = 20) private String module;
    private Long definitionId;
    @Column(nullable = false) private LocalDate startDate;
    @Column(nullable = false) private LocalDate endDate;
    @Column(length = 300) private String reason;
    @Column(nullable = false, length = 15) private String status = "ACTIVE";
    private Long approvedById;
    @Column(nullable = false, updatable = false) private Instant createdAt = Instant.now();
}
