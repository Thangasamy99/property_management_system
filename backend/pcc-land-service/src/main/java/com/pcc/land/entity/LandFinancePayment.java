package com.pcc.land.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "land_finance_payments")
public class LandFinancePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String targetType; // ACQUISITION | SALE

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, length = 60)
    private String paymentRef;

    @Column(nullable = false, length = 60)
    private String allocationRef;

    @Column(nullable = false, precision = 18)
    private BigDecimal amount;

    @Column(nullable = false, precision = 18)
    private BigDecimal reversedAmount = BigDecimal.ZERO;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
