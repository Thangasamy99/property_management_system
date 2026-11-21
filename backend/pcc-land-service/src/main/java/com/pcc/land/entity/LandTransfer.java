package com.pcc.land.entity;

import com.pcc.land.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "land_transfers")
public class LandTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @Column(nullable = false, unique = true, length = 30)
    private String transferReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_ownership_id")
    private LandOwnership previousOwnership;

    @Column(length = 160)
    private String previousOwnerName;

    @Column(nullable = false, length = 160)
    private String newOwnerName;

    @Column(length = 80)
    private String newOwnerReference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private LandSale sale;

    @Column(length = 120)
    private String titleReference;

    @Column(length = 120)
    private String documentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransferStatus status = TransferStatus.DRAFT;

    private LocalDate legalCompletionDate;

    private Instant submittedAt;

    private Long decidedBy;

    private String decidedByName;

    private Instant decidedAt;

    private String decisionNote;

    private Long createdBy;

    private String createdByName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
