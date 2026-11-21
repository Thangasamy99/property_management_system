package com.pcc.land.entity;

import com.pcc.land.enums.OwnershipEventType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "land_ownership_history")
public class LandOwnershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ownership_id")
    private LandOwnership ownership;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OwnershipEventType eventType;

    @Column(length = 160)
    private String previousOwner;

    @Column(length = 160)
    private String newOwner;

    private LocalDate effectiveDate;

    private Long performedBy;

    private String performedByName;

    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
