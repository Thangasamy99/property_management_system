package com.pcc.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "receipts")
public class Receipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String receiptNo;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @Column(nullable = false, length = 150)
    private String payerName;

    @Column(nullable = false, precision = 18)
    private BigDecimal amount;

    @Column(length = 500)
    private String amountInWords;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false, length = 30)
    private String paymentMethod;

    @Column(length = 100)
    private String paymentReference;

    @Column(nullable = false, length = 30)
    private String transactionType;

    @Column(length = 120)
    private String relatedReference;

    @Column(length = 150)
    private String receivedByName;

    @Column(nullable = false, length = 150)
    private String organization = "Presbyterian Church in Cameroon (PCC)";

    @Column(nullable = false)
    private boolean voided = false;

    private Long issuedById;

    @Column(length = 150)
    private String issuedByName;

    @Column(nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();
}
