package com.pcc.property.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "rent_schedule_payments")
public class RentSchedulePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private RentSchedule schedule;

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
