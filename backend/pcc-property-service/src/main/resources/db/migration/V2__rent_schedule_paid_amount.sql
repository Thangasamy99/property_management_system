-- Finance integration contract: tracks money received per schedule.
-- Status derivation: 0 -> PENDING (or OVERDUE handled elsewhere),
-- < amount -> PARTIAL, >= amount -> PAID. Updated only via the
-- PATCH /api/v1/rent-schedules/{id}/payment endpoint guarded by
-- RENT_SCHEDULE_PAYMENT_SYNC.
ALTER TABLE rent_schedules
    ADD COLUMN paid_amount DECIMAL(18,0) NOT NULL DEFAULT 0 AFTER amount;
