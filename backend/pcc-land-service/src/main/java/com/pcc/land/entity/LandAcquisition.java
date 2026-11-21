package com.pcc.land.entity;

import com.pcc.land.enums.AcquisitionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "land_acquisitions")
public class LandAcquisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @Column(nullable = false, unique = true, length = 30)
    private String acquisitionReference;

    @Column(nullable = false, length = 160)
    private String sellerName;

    @Column(length = 80)
    private String sellerReference;

    @Column(length = 120)
    private String sellerContact;

    private LocalDate acquisitionDate;

    @Column(precision = 18)
    private BigDecimal purchasePrice;

    @Column(length = 120)
    private String documentReference;

    @Column(length = 80)
    private String approvalReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AcquisitionStatus status = AcquisitionStatus.PENDING;

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
