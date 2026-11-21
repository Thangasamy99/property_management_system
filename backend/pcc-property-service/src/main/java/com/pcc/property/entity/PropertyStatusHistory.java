package com.pcc.property.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "property_status_history")
public class PropertyStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id")
    private Property property;

    private String fromStatus;

    @Column(nullable = false)
    private String toStatus;

    private String reason;
    private Long actorId;
    private String actorUsername;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
