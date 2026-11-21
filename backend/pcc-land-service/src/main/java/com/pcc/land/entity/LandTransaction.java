package com.pcc.land.entity;

import com.pcc.land.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "land_transactions")
public class LandTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcel_id", nullable = false)
    private LandParcel parcel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionType transactionType;

    private Long referenceId;

    @Column(length = 40)
    private String referenceCode;

    @Column(precision = 18)
    private BigDecimal amount;

    @Column(length = 30)
    private String status;

    private Long actorId;

    private String actorName;

    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
