package com.pcc.finance.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Persistent sync work item: an idempotent call to Property/Land, retried until confirmed. */
@Getter
@Setter
@Entity
@Table(name = "sync_outbox")
public class SyncOutbox {

    public enum Kind { PAYMENT, REVERSAL }
    public enum Status { PENDING, SUCCESS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable dedupe reference — e.g. PAY-0001-A1 (payment) or RFD-0002-PAY-0001-A1 (reversal). */
    @Column(nullable = false, length = 60)
    private String syncRef;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Kind syncKind;

    /** PROPERTY or LAND. */
    @Column(nullable = false, length = 20)
    private String externalService;

    @Column(nullable = false, length = 20)
    private String targetType;

    @Column(nullable = false)
    private Long targetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.PENDING;

    /** True when the downstream service rejected the payload — do not auto-retry. */
    @Column(nullable = false)
    private boolean permanent = false;

    @Column(nullable = false)
    private int retryCount = 0;

    private Instant lastAttemptAt;

    @Column(length = 500)
    private String lastError;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
