package com.pcc.finance.entity;

import com.pcc.finance.enums.PaymentStatus;
import com.pcc.finance.enums.PaymentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String paymentRef;

    @Column(unique = true, length = 80)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentType paymentType;

    @Column(nullable = false, length = 150)
    private String payerName;

    @Column(length = 80)
    private String payerReference;

    @Column(nullable = false, precision = 18)
    private BigDecimal amount;

    @Column(nullable = false, precision = 18)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String paymentReference;

    private String verificationDetail;

    @Column(length = 500)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private FinanceAccount account;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.RECORDED;

    private Long receivedById;

    @Column(length = 150)
    private String receivedByName;

    private Long postedById;

    @Column(length = 150)
    private String postedByName;

    private Instant postedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
