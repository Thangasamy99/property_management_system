-- Allocation-level Finance payment records for acquisitions and sales.
-- Enables validated, idempotent payment reversals.
CREATE TABLE land_finance_payments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    target_type     VARCHAR(20)  NOT NULL,           -- ACQUISITION | SALE
    target_id       BIGINT       NOT NULL,
    payment_ref     VARCHAR(60)  NOT NULL,
    allocation_ref  VARCHAR(60)  NOT NULL,
    amount          DECIMAL(18,0) NOT NULL,
    reversed_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_land_finance_payments PRIMARY KEY (id),
    CONSTRAINT uk_lfp_target_alloc UNIQUE (target_type, target_id, allocation_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE land_finance_payment_reversals (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    finance_payment_id BIGINT     NOT NULL,
    reversal_ref     VARCHAR(60)  NOT NULL,
    amount           DECIMAL(18,0) NOT NULL,
    reversal_date    DATE         NULL,
    reason           VARCHAR(255) NULL,
    actor            VARCHAR(120) NULL,
    created_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_lf_payment_reversals PRIMARY KEY (id),
    CONSTRAINT fk_lfpr_payment FOREIGN KEY (finance_payment_id)
        REFERENCES land_finance_payments (id),
    CONSTRAINT uk_lfpr_reversal_ref UNIQUE (reversal_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_lfp_target ON land_finance_payments (target_type, target_id);
