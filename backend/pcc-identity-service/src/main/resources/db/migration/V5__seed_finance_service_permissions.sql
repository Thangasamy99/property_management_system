-- ============================================================
-- Finance Service granular permissions.
-- Existing codes reused by Finance Service: PAYMENT_VIEW,
-- PAYMENT_CREATE, PAYMENT_APPROVE, EXPENSE_VIEW, EXPENSE_CREATE,
-- EXPENSE_APPROVE, FINANCIAL_REPORT_VIEW (seeded in V2).
-- New codes add: dashboard, rent-collection, land-payment,
-- income, expense-pay, receipt, refund, ledger, cash-account
-- and cross-service payment-sync contracts.
-- ============================================================

INSERT INTO permissions (code, name, module, description) VALUES
('FINANCE_DASHBOARD_VIEW',    'View finance dashboard',    'FINANCE', 'View financial summary dashboard'),
('RENT_COLLECTION_VIEW',      'View rent collections',     'FINANCE', 'View rent payments and outstanding balances'),
('RENT_COLLECTION_CREATE',    'Record rent payments',      'FINANCE', 'Record full or partial rent payments'),
('RENT_COLLECTION_APPROVE',   'Approve rent payments',     'FINANCE', 'Post and verify rent payments (sensitive)'),
('LAND_PAYMENT_VIEW',         'View land payments',        'FINANCE', 'View land acquisition and sale payments'),
('LAND_PAYMENT_CREATE',       'Record land payments',      'FINANCE', 'Record land acquisition and sale payments'),
('LAND_PAYMENT_APPROVE',      'Approve land payments',     'FINANCE', 'Post and verify land payments (sensitive)'),
('INCOME_VIEW',               'View other income',         'FINANCE', 'View non-rent, non-land income records'),
('INCOME_CREATE',             'Record other income',       'FINANCE', 'Record other company income'),
('INCOME_APPROVE',            'Approve other income',      'FINANCE', 'Approve other income records (sensitive)'),
('EXPENSE_PAY',               'Pay approved expenses',     'FINANCE', 'Execute payment for approved expenses'),
('RECEIPT_VIEW',              'View receipts',             'FINANCE', 'View payment receipts'),
('RECEIPT_PRINT',             'Print receipts',            'FINANCE', 'Print or export receipts'),
('REFUND_VIEW',               'View refunds',              'FINANCE', 'View refund and reversal records'),
('REFUND_CREATE',             'Request refunds',           'FINANCE', 'Request refunds against posted payments'),
('REFUND_APPROVE',            'Approve refunds/reversals', 'FINANCE', 'Approve refunds and post reversals (sensitive)'),
('LEDGER_VIEW',               'View financial ledger',     'FINANCE', 'View ledger entries and posting history'),
('LEDGER_ADJUST',             'Adjust ledger',             'FINANCE', 'Create adjustment ledger entries (sensitive)'),
('CASH_ACCOUNT_VIEW',         'View cash/bank accounts',   'FINANCE', 'View cashbook and bank balances'),
('CASH_ACCOUNT_MANAGE',       'Manage cash/bank accounts', 'FINANCE', 'Create and maintain cash and bank accounts'),
('RENT_SCHEDULE_PAYMENT_SYNC','Sync rent schedule payment','PROPERTY','Update rent schedule paid status (finance integration)'),
('LAND_PAYMENT_SYNC',         'Sync land payment status',  'LAND',    'Update land payment status (finance integration)');

-- ADMIN: every new permission (matches V2-V4 pattern)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'FINANCE_DASHBOARD_VIEW','RENT_COLLECTION_VIEW','RENT_COLLECTION_CREATE','RENT_COLLECTION_APPROVE',
  'LAND_PAYMENT_VIEW','LAND_PAYMENT_CREATE','LAND_PAYMENT_APPROVE',
  'INCOME_VIEW','INCOME_CREATE','INCOME_APPROVE','EXPENSE_PAY',
  'RECEIPT_VIEW','RECEIPT_PRINT','REFUND_VIEW','REFUND_CREATE','REFUND_APPROVE',
  'LEDGER_VIEW','LEDGER_ADJUST','CASH_ACCOUNT_VIEW','CASH_ACCOUNT_MANAGE',
  'RENT_SCHEDULE_PAYMENT_SYNC','LAND_PAYMENT_SYNC')
WHERE r.code = 'ADMIN';

-- MANAGER: oversight, approvals, reports (no payment execution)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'FINANCE_DASHBOARD_VIEW','RENT_COLLECTION_VIEW','RENT_COLLECTION_APPROVE',
  'LAND_PAYMENT_VIEW','LAND_PAYMENT_APPROVE','INCOME_VIEW','INCOME_APPROVE',
  'RECEIPT_VIEW','RECEIPT_PRINT','REFUND_VIEW','REFUND_APPROVE',
  'LEDGER_VIEW','LEDGER_ADJUST','CASH_ACCOUNT_VIEW')
WHERE r.code = 'MANAGER';

-- ACCOUNTANT: financial operations + cross-module view needed by
-- Finance's token-relay verification against Property/Land.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'FINANCE_DASHBOARD_VIEW','RENT_COLLECTION_VIEW','RENT_COLLECTION_CREATE',
  'LAND_PAYMENT_VIEW','LAND_PAYMENT_CREATE','INCOME_VIEW','INCOME_CREATE',
  'EXPENSE_PAY','RECEIPT_VIEW','RECEIPT_PRINT','REFUND_VIEW','REFUND_CREATE',
  'LEDGER_VIEW','CASH_ACCOUNT_VIEW',
  'RENT_SCHEDULE_PAYMENT_SYNC','LAND_PAYMENT_SYNC',
  'PROPERTY_VIEW','TENANT_VIEW','RENTAL_VIEW')
WHERE r.code = 'ACCOUNTANT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- PROPERTY_OFFICER: permitted rent collection operations (no approvals)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('RENT_COLLECTION_VIEW','RENT_COLLECTION_CREATE','RECEIPT_VIEW')
WHERE r.code = 'PROPERTY_OFFICER';

-- LAND_OFFICER: land payment visibility (no approvals, no posting)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('LAND_PAYMENT_VIEW')
WHERE r.code = 'LAND_OFFICER';
