package com.pcc.property.entity;

import com.pcc.property.enums.ApartmentStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "apartments")
public class Apartment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "building_id")
    private Building building;

    @Column(nullable = false, length = 20)
    private String apartmentNumber;

    private Integer floorNumber;
    private Integer bedrooms;
    private Integer bathrooms;

    @Column(precision = 15, scale = 3)
    private BigDecimal areaSqm;

    @Column(precision = 18)
    private BigDecimal monthlyRent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApartmentStatus status = ApartmentStatus.AVAILABLE;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
