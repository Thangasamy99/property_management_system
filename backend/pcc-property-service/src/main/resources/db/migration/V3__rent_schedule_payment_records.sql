-- Allocation-level payment records for Finance synchronization.
-- Enables per-allocation reversals with validation and idempotency.
CREATE TABLE rent_schedule_payments (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    schedule_id     BIGINT       NOT NULL,
    payment_ref     VARCHAR(60)  NOT NULL,
    allocation_ref  VARCHAR(60)  NOT NULL,
    amount          DECIMAL(18,0) NOT NULL,
    reversed_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_rent_schedule_payments PRIMARY KEY (id),
    CONSTRAINT fk_rsp_schedule FOREIGN KEY (schedule_id)
        REFERENCES rent_schedules (id),
    CONSTRAINT uk_rsp_schedule_alloc UNIQUE (schedule_id, allocation_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE rent_schedule_payment_reversals (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    schedule_id      BIGINT       NOT NULL,
    schedule_payment_id BIGINT    NOT NULL,
    reversal_ref     VARCHAR(60)  NOT NULL,
    amount           DECIMAL(18,0) NOT NULL,
    reversal_date    DATE         NULL,
    reason           VARCHAR(255) NULL,
    actor            VARCHAR(120) NULL,
    created_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_rs_payment_reversals PRIMARY KEY (id),
    CONSTRAINT fk_rspr_schedule FOREIGN KEY (schedule_id)
        REFERENCES rent_schedules (id),
    CONSTRAINT fk_rspr_payment FOREIGN KEY (schedule_payment_id)
        REFERENCES rent_schedule_payments (id),
    CONSTRAINT uk_rspr_reversal_ref UNIQUE (reversal_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_rsp_schedule ON rent_schedule_payments (schedule_id);
CREATE INDEX idx_rspr_schedule ON rent_schedule_payment_reversals (schedule_id);
