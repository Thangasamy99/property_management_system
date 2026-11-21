package com.pcc.property.entity;

import com.pcc.property.enums.PropertyStatus;
import com.pcc.property.enums.PropertyType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 30)
    private PropertyType propertyType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate acquisitionDate;

    @Column(precision = 18)
    private BigDecimal acquisitionCost;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PropertyStatus status = PropertyStatus.ACTIVE;

    private Long createdBy;

    private String createdByName;

    private Instant deletedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToOne(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PropertyAddress address;

    public void setAddress(PropertyAddress address) {
        this.address = address;
        if (address != null) address.setProperty(this);
    }

    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }
}
