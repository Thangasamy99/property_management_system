-- V2: allocation-level reversal tracking + persistent downstream sync outbox.
-- Finance remains the source of truth; Property/Land receive idempotent
-- allocation/reversal records instead of best-effort absolute totals.

ALTER TABLE payment_allocations
    ADD COLUMN allocation_ref  VARCHAR(60) NULL AFTER target_id,
    ADD COLUMN reversed_amount DECIMAL(18,0) NOT NULL DEFAULT 0 AFTER amount;

-- Backfill a stable, unique allocation reference for existing rows:
-- <payment_ref>-A<allocation_id>
UPDATE payment_allocations a
JOIN payments p ON p.id = a.payment_id
SET a.allocation_ref = CONCAT(p.payment_ref, '-A', a.id);

ALTER TABLE payment_allocations
    MODIFY COLUMN allocation_ref VARCHAR(60) NOT NULL;

CREATE UNIQUE INDEX uk_alloc_ref ON payment_allocations (allocation_ref);
CREATE INDEX idx_alloc_payment ON payment_allocations (payment_id);

CREATE TABLE sync_outbox (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    sync_ref        VARCHAR(60)  NOT NULL,
    sync_kind       VARCHAR(10)  NOT NULL,             -- PAYMENT | REVERSAL
    external_service VARCHAR(20) NOT NULL,             -- PROPERTY | LAND
    target_type     VARCHAR(20)  NOT NULL,             -- RENT_SCHEDULE | LAND_ACQUISITION | LAND_SALE
    target_id       BIGINT       NOT NULL,
    payload         TEXT         NOT NULL,
    status          VARCHAR(10)  NOT NULL DEFAULT 'PENDING', -- PENDING | SUCCESS | FAILED
    permanent       TINYINT(1)   NOT NULL DEFAULT 0,   -- FAILED + permanent => no auto-retry
    retry_count     INT          NOT NULL DEFAULT 0,
    last_attempt_at DATETIME(6)  NULL,
    last_error      VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_sync_outbox PRIMARY KEY (id),
    CONSTRAINT uk_sync_ref UNIQUE (sync_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_outbox_status ON sync_outbox (status, permanent, retry_count);
CREATE INDEX idx_outbox_target ON sync_outbox (external_service, target_type, target_id);
