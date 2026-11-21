package com.pcc.finance.entity;

import com.pcc.finance.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "refunds")
public class Refund {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String refundRef;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false, precision = 18)
    private BigDecimal amount;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status = RefundStatus.REQUESTED;

    private Long requestedById;

    @Column(length = 150)
    private String requestedByName;

    @Column(nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    private Long decidedById;

    @Column(length = 150)
    private String decidedByName;

    private Instant decidedAt;

    private String decisionNote;
}
