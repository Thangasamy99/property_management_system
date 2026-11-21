package com.pcc.land.entity;

import com.pcc.land.enums.OwnershipStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "land_ownership")
public class LandOwnership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @Column(nullable = false, unique = true, length = 30)
    private String ownershipReference;

    @Column(nullable = false, length = 160)
    private String ownerName;

    @Column(length = 80)
    private String ownerReference;

    @Column(length = 120)
    private String titleReference;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal sharePercent = new BigDecimal("100.00");

    private LocalDate acquisitionDate;

    @Column(nullable = false)
    private LocalDate effectiveDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OwnershipStatus status = OwnershipStatus.CURRENT;

    @Column(length = 120)
    private String documentReference;

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
