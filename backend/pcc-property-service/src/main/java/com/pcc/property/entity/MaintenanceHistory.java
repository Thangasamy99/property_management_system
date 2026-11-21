package com.pcc.property.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "maintenance_history")
public class MaintenanceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id")
    private MaintenanceRequest request;

    @Column(nullable = false, length = 40)
    private String event;

    private String note;
    private Long actorId;
    private String actorUsername;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
