package com.pcc.finance.entity;

import com.pcc.finance.enums.IncomeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "other_incomes")
public class OtherIncome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String incomeRef;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 18)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate incomeDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(length = 100)
    private String externalReference;

    @Column(length = 120)
    private String documentReference;

    @Column(length = 150)
    private String payerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncomeStatus status = IncomeStatus.RECORDED;

    private String rejectionReason;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    private Long createdById;

    @Column(length = 150)
    private String createdByName;

    private Long approvedById;

    @Column(length = 150)
    private String approvedByName;

    private Instant approvedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
