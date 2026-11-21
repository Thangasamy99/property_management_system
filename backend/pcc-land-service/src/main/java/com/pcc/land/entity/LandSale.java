package com.pcc.land.entity;

import com.pcc.land.enums.SaleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "land_sales")
public class LandSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @Column(nullable = false, unique = true, length = 30)
    private String saleReference;

    @Column(nullable = false, length = 160)
    private String buyerName;

    @Column(length = 80)
    private String buyerReference;

    @Column(length = 120)
    private String buyerContact;

    @Column(nullable = false, precision = 18)
    private BigDecimal proposedPrice;

    @Column(precision = 18)
    private BigDecimal agreedPrice;

    @Column(length = 120)
    private String agreementReference;

    private LocalDate agreementDate;

    @Column(length = 120)
    private String documentReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status = SaleStatus.DRAFT;

    @Column(nullable = false, precision = 18)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String financeStatus = "UNPAID";

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
