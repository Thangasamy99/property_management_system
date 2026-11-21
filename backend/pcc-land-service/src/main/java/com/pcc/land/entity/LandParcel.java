package com.pcc.land.entity;

import com.pcc.land.enums.LandClassification;
import com.pcc.land.enums.LandStatus;
import com.pcc.land.enums.MeasurementUnit;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "land_parcels")
public class LandParcel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String parcelReference;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LandClassification classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LandStatus status = LandStatus.AVAILABLE;

    @Column(name = "area_sqm", nullable = false, precision = 15, scale = 3)
    private BigDecimal areaSqm;

    @Column(precision = 15, scale = 3)
    private BigDecimal originalArea;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private MeasurementUnit originalUnit;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(length = 100)
    private String division;

    @Column(length = 100)
    private String subdivision;

    @Column(length = 120)
    private String locality;

    private String address;

    private LocalDate acquisitionDate;

    private Long createdBy;

    private String createdByName;

    private Instant deletedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private Long version;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
