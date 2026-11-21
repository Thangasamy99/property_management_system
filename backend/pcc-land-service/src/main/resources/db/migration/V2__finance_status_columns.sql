-- Finance integration contract: payment status tracked by PCC Finance
-- Service. Updated only via PATCH .../finance-status endpoints guarded
-- by LAND_PAYMENT_SYNC. finance_status: UNPAID | PARTIALLY_PAID | PAID.
ALTER TABLE land_acquisitions
    ADD COLUMN paid_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
    ADD COLUMN finance_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';

ALTER TABLE land_sales
    ADD COLUMN paid_amount DECIMAL(18,0) NOT NULL DEFAULT 0,
    ADD COLUMN finance_status VARCHAR(20) NOT NULL DEFAULT 'UNPAID';
