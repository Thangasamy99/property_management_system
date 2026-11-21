-- ============================================================
-- PCC Finance Service - V1 schema (pcc_finance_db)
-- Consolidated design: cash_accounts + bank_accounts merged into
-- finance_accounts; external_transaction_references merged into
-- payment_targets; expense_approvals folded onto expenses (with
-- audit via finance_audit_logs + ledger).
-- XAF amounts: DECIMAL(18,0). No floating point for money.
-- ============================================================

CREATE TABLE payment_methods (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    code        VARCHAR(30)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO payment_methods (code, name, description) VALUES
('CASH',          'Cash',           'Physical cash received at cashier'),
('BANK_TRANSFER', 'Bank Transfer',  'Bank-to-bank transfer'),
('CHEQUE',        'Cheque',         'Bank cheque'),
('MOBILE_MONEY',  'Mobile Money',   'MTN/Orange mobile money'),
('OTHER',         'Other',          'Other payment method');

CREATE TABLE finance_accounts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_type    VARCHAR(10)  NOT NULL,            -- CASH | BANK
    code            VARCHAR(30)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    description     VARCHAR(255),
    opening_balance DECIMAL(18,0) NOT NULL DEFAULT 0,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_fa_type (account_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO finance_accounts (account_type, code, name, description) VALUES
('CASH', 'CASH-MAIN', 'Main Cash Account', 'Primary cashier till'),
('BANK', 'BANK-MAIN', 'Main Bank Account', 'Primary company bank account');

-- External payment targets: one row per payable external object
-- (rent schedule, land acquisition, land sale). Row is locked
-- pessimistically while allocating to serialize concurrent payers.
CREATE TABLE payment_targets (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type        VARCHAR(30)   NOT NULL,        -- RENT_SCHEDULE | LAND_ACQUISITION | LAND_SALE
    external_id        BIGINT        NOT NULL,        -- id inside the owning service's DB (external ref, not FK)
    external_reference VARCHAR(60)   NOT NULL,        -- human ref e.g. schedule id, ACQ-0001, SAL-0001
    parent_reference   VARCHAR(60),                   -- agreement number / parcel reference
    description        VARCHAR(255),
    total_amount       DECIMAL(18,0) NOT NULL,        -- authoritative total from owning service
    snapshot           TEXT,                          -- JSON snapshot of relevant external details
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uq_target (target_type, external_id),
    INDEX idx_pt_ref (external_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payments (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_ref       VARCHAR(20)   NOT NULL UNIQUE,  -- PAY-0001
    idempotency_key   VARCHAR(80)   UNIQUE,           -- client retry key
    payment_type      VARCHAR(30)   NOT NULL,         -- RENT | LAND_ACQUISITION | LAND_SALE | OTHER_INCOME | EXPENSE_PAYOUT
    payer_name        VARCHAR(150)  NOT NULL,
    payer_reference   VARCHAR(80),
    amount            DECIMAL(18,0) NOT NULL,
    refunded_amount   DECIMAL(18,0) NOT NULL DEFAULT 0,
    payment_date      DATE          NOT NULL,
    payment_method_id BIGINT        NOT NULL,
    payment_reference VARCHAR(100),                    -- cheque no, momo txn id, bank ref
    verification_detail VARCHAR(255),
    notes             VARCHAR(500),
    account_id        BIGINT,                          -- credited/debited finance account
    status            VARCHAR(20)   NOT NULL,          -- RECORDED | POSTED | PARTIALLY_REFUNDED | REFUNDED | REVERSED | VOID
    received_by_id    BIGINT,
    received_by_name  VARCHAR(150),
    posted_by_id      BIGINT,
    posted_by_name    VARCHAR(150),
    posted_at         DATETIME(6),
    created_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_pay_method  FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    CONSTRAINT fk_pay_account FOREIGN KEY (account_id)        REFERENCES finance_accounts(id),
    INDEX idx_pay_type   (payment_type),
    INDEX idx_pay_status (status),
    INDEX idx_pay_date   (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_allocations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_id  BIGINT        NOT NULL,
    target_id   BIGINT        NOT NULL,
    amount      DECIMAL(18,0) NOT NULL,
    note        VARCHAR(255),
    created_at  DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_alloc_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_alloc_target  FOREIGN KEY (target_id)  REFERENCES payment_targets(id),
    INDEX idx_alloc_target (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE other_incomes (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    income_ref         VARCHAR(20)   NOT NULL UNIQUE,  -- INC-0001
    category           VARCHAR(60)   NOT NULL,
    description        VARCHAR(255)  NOT NULL,
    amount             DECIMAL(18,0) NOT NULL,
    income_date        DATE          NOT NULL,
    payment_method_id  BIGINT        NOT NULL,
    external_reference VARCHAR(100),
    document_reference VARCHAR(120),
    payer_name         VARCHAR(150),
    status             VARCHAR(20)   NOT NULL,         -- RECORDED | APPROVED | REJECTED | POSTED
    rejection_reason   VARCHAR(255),
    payment_id         BIGINT,                          -- populated once posted
    created_by_id      BIGINT,
    created_by_name    VARCHAR(150),
    approved_by_id     BIGINT,
    approved_by_name   VARCHAR(150),
    approved_at        DATETIME(6),
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inc_method  FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    CONSTRAINT fk_inc_payment FOREIGN KEY (payment_id)        REFERENCES payments(id),
    INDEX idx_inc_status (status),
    INDEX idx_inc_date   (income_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE expenses (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_ref        VARCHAR(20)   NOT NULL UNIQUE,  -- EXP-0001
    category           VARCHAR(60)   NOT NULL,
    description        VARCHAR(255)  NOT NULL,
    amount             DECIMAL(18,0) NOT NULL,
    expense_date       DATE          NOT NULL,
    department         VARCHAR(80),                       -- department / cost centre
    vendor_name        VARCHAR(150),
    vendor_reference   VARCHAR(80),
    document_reference VARCHAR(120),
    payment_method_id  BIGINT,
    payment_reference  VARCHAR(100),
    paid_date          DATE,
    status             VARCHAR(20)   NOT NULL,         -- PENDING_APPROVAL | APPROVED | REJECTED | PAID | CANCELLED
    rejection_reason   VARCHAR(255),
    payment_id         BIGINT,                          -- payout payment once PAID
    created_by_id      BIGINT,
    created_by_name    VARCHAR(150),
    approved_by_id     BIGINT,
    approved_by_name   VARCHAR(150),
    approved_at        DATETIME(6),
    paid_by_id         BIGINT,
    paid_by_name       VARCHAR(150),
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_exp_method  FOREIGN KEY (payment_method_id) REFERENCES payment_methods(id),
    CONSTRAINT fk_exp_payment FOREIGN KEY (payment_id)        REFERENCES payments(id),
    INDEX idx_exp_status   (status),
    INDEX idx_exp_category (category),
    INDEX idx_exp_date     (expense_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Immutable receipt snapshot; issued only when payment is POSTED.
CREATE TABLE receipts (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    receipt_no      VARCHAR(20)   NOT NULL UNIQUE,     -- RCP-0001
    payment_id      BIGINT        NOT NULL UNIQUE,
    payer_name      VARCHAR(150)  NOT NULL,
    amount          DECIMAL(18,0) NOT NULL,
    amount_in_words VARCHAR(500),
    payment_date    DATE          NOT NULL,
    payment_method  VARCHAR(30)   NOT NULL,
    payment_reference VARCHAR(100),
    transaction_type VARCHAR(30)  NOT NULL,
    related_reference VARCHAR(120),                    -- agreement / parcel / income / expense ref
    received_by_name VARCHAR(150),
    organization    VARCHAR(150)  NOT NULL DEFAULT 'Presbyterian Church in Cameroon (PCC)',
    voided          BOOLEAN       NOT NULL DEFAULT FALSE,
    issued_by_id    BIGINT,
    issued_by_name  VARCHAR(150),
    issued_at       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_rcpt_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_rcpt_date (payment_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refunds (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    refund_ref      VARCHAR(20)   NOT NULL UNIQUE,     -- RFD-0001
    payment_id      BIGINT        NOT NULL,
    amount          DECIMAL(18,0) NOT NULL,
    reason          VARCHAR(255)  NOT NULL,
    status          VARCHAR(20)   NOT NULL,            -- REQUESTED | APPROVED | REJECTED | POSTED
    requested_by_id BIGINT,
    requested_by_name VARCHAR(150),
    requested_at    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    decided_by_id   BIGINT,
    decided_by_name VARCHAR(150),
    decided_at      DATETIME(6),
    decision_note   VARCHAR(255),
    CONSTRAINT fk_ref_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_ref_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Immutable ledger; corrections via REVERSAL entries only.
CREATE TABLE financial_ledger_entries (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    entry_ref        VARCHAR(20)   NOT NULL UNIQUE,    -- LED-0001
    transaction_type VARCHAR(40)   NOT NULL,           -- RECEIPT | REFUND | REVERSAL | EXPENSE_PAYOUT | ADJUSTMENT
    direction        VARCHAR(10)   NOT NULL,           -- CREDIT | DEBIT
    amount           DECIMAL(18,0) NOT NULL,
    transaction_date DATE          NOT NULL,
    source_type      VARCHAR(30),                      -- PAYMENT | REFUND | EXPENSE | ADJUSTMENT
    source_id        BIGINT,
    source_ref       VARCHAR(60),
    payment_id       BIGINT,
    account_id       BIGINT,
    payment_method   VARCHAR(30),
    posting_status   VARCHAR(15)   NOT NULL,           -- POSTED | REVERSED
    reversal_of_id   BIGINT,
    description      VARCHAR(255),
    created_by_id    BIGINT,
    created_by_name  VARCHAR(150),
    posted_at        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_led_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_led_account FOREIGN KEY (account_id) REFERENCES finance_accounts(id),
    INDEX idx_led_date  (transaction_date),
    INDEX idx_led_type  (transaction_type),
    INDEX idx_led_acct  (account_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE finance_audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    entity_type VARCHAR(40)  NOT NULL,
    entity_id   BIGINT       NOT NULL,
    action      VARCHAR(60)  NOT NULL,
    details     VARCHAR(500),
    actor_id    BIGINT,
    actor_name  VARCHAR(150),
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
