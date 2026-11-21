package com.pcc.finance.entity;

import com.pcc.finance.enums.LedgerDirection;
import com.pcc.finance.enums.LedgerTxType;
import com.pcc.finance.enums.PostingStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "financial_ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String entryRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private LedgerTxType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerDirection direction;

    @Column(nullable = false, precision = 18)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(length = 30)
    private String sourceType;

    private Long sourceId;

    @Column(length = 60)
    private String sourceRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private FinanceAccount account;

    @Column(length = 30)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private PostingStatus postingStatus = PostingStatus.POSTED;

    private Long reversalOfId;

    private String description;

    private Long createdById;

    @Column(length = 150)
    private String createdByName;

    @Column(nullable = false, updatable = false)
    private Instant postedAt = Instant.now();
}
