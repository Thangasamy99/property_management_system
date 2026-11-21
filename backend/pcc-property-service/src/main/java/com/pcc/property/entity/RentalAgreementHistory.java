package com.pcc.property.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "rental_agreement_history")
public class RentalAgreementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private RentalAgreement agreement;

    @Column(nullable = false, length = 40)
    private String event;

    private String fromStatus;
    private String toStatus;
    private String note;
    private Long actorId;
    private String actorUsername;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
