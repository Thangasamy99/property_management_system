-- V8: Reporting & Analytics Service permission set.
-- Reuses existing codes: LAND_REPORT_VIEW (V4), WORKFLOW_REPORT_VIEW (V7),
-- FINANCIAL_REPORT_VIEW (V2). Only genuinely new codes are inserted.
-- Least-privilege: officers see their domain reports only; sensitive
-- employee/financial detail stays with ADMIN/MANAGER/ACCOUNTANT.
INSERT INTO permissions (code, name, module, description) VALUES
('REPORTING_VIEW',        'Reporting view',          'REPORTING', 'Access the reporting service'),
('DASHBOARD_VIEW',        'Executive dashboard',     'REPORTING', 'View executive analytics dashboard'),
('PROPERTY_REPORT_VIEW',  'Property reports',        'REPORTING', 'View property reports'),
('DOCUMENT_REPORT_VIEW',  'Document reports',        'REPORTING', 'View document reports'),
('EMPLOYEE_REPORT_VIEW',  'Employee reports',        'REPORTING', 'View employee activity reports'),
('AUDIT_REPORT_VIEW',     'Audit reports',           'REPORTING', 'View audit reports'),
('REPORT_EXPORT',         'Report export',           'REPORTING', 'Export reports to CSV/XLSX/PDF'),
('REPORT_REFRESH',        'Report refresh',          'REPORTING', 'Force-refresh report caches'),
('REPORT_CONFIGURE',      'Report configure',        'REPORTING', 'Manage report definitions');

-- ADMIN: everything (new + reused domain codes)
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'ADMIN' AND p.code IN
('REPORTING_VIEW','DASHBOARD_VIEW','PROPERTY_REPORT_VIEW','LAND_REPORT_VIEW',
 'FINANCIAL_REPORT_VIEW','WORKFLOW_REPORT_VIEW','DOCUMENT_REPORT_VIEW',
 'EMPLOYEE_REPORT_VIEW','AUDIT_REPORT_VIEW','REPORT_EXPORT','REPORT_REFRESH',
 'REPORT_CONFIGURE');

-- MANAGER: dashboards + operational reports + export/refresh
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'MANAGER' AND p.code IN
('REPORTING_VIEW','DASHBOARD_VIEW','PROPERTY_REPORT_VIEW','LAND_REPORT_VIEW',
 'FINANCIAL_REPORT_VIEW','WORKFLOW_REPORT_VIEW','DOCUMENT_REPORT_VIEW',
 'AUDIT_REPORT_VIEW','REPORT_EXPORT','REPORT_REFRESH');

-- ACCOUNTANT: finance + workflow/document reports + export
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'ACCOUNTANT' AND p.code IN
('REPORTING_VIEW','FINANCIAL_REPORT_VIEW','WORKFLOW_REPORT_VIEW',
 'DOCUMENT_REPORT_VIEW','REPORT_EXPORT');

-- PROPERTY_OFFICER: property reports only
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'PROPERTY_OFFICER' AND p.code IN
('REPORTING_VIEW','PROPERTY_REPORT_VIEW','DOCUMENT_REPORT_VIEW');

-- LAND_OFFICER: land reports only
INSERT IGNORE INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p
WHERE r.code = 'LAND_OFFICER' AND p.code IN
('REPORTING_VIEW','LAND_REPORT_VIEW','DOCUMENT_REPORT_VIEW');
