package com.pcc.land.entity;

import com.pcc.land.enums.LandStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "land_status_history")
public class LandStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LandStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LandStatus newStatus;

    private String reason;

    private Long changedBy;

    private String changedByName;

    @Column(nullable = false, updatable = false)
    private Instant changedAt = Instant.now();
}
