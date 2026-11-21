package com.pcc.land.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "land_surveys")
public class LandSurvey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @Column(nullable = false, unique = true, length = 30)
    private String surveyReference;

    @Column(nullable = false)
    private LocalDate surveyDate;

    @Column(length = 160)
    private String surveyorName;

    @Column(length = 160)
    private String surveyorOrganization;

    @Column(precision = 15, scale = 3)
    private BigDecimal surveyedArea;

    private String boundaryNorth;
    private String boundarySouth;
    private String boundaryEast;
    private String boundaryWest;

    @Column(columnDefinition = "TEXT")
    private String description;

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
