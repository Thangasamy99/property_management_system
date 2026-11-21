package com.pcc.property.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "rent_schedule_payment_reversals")
public class RentSchedulePaymentReversal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_id")
    private RentSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_payment_id")
    private RentSchedulePayment schedulePayment;

    @Column(nullable = false, length = 60)
    private String reversalRef;

    @Column(nullable = false, precision = 18)
    private BigDecimal amount;

    private LocalDate reversalDate;

    @Column(length = 255)
    private String reason;

    @Column(length = 120)
    private String actor;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
