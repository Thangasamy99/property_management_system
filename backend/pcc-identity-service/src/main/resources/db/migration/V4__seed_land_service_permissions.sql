-- ============================================================
-- Land Service granular permissions (surveys, ownership,
-- acquisition create/approve split, land reporting).
-- Existing codes reused by Land Service: LAND_VIEW/CREATE/
-- UPDATE/DELETE, LAND_SALE_REQUEST (sale+transfer create),
-- LAND_SALE_APPROVE (sale+transfer approval),
-- LAND_ACQUISITION_MANAGE, LAND_DOCUMENT_MANAGE.
-- ============================================================

INSERT INTO permissions (code, name, module, description) VALUES
('LAND_SURVEY_VIEW',        'View land surveys',        'LAND', 'View survey records'),
('LAND_SURVEY_CREATE',      'Create land surveys',      'LAND', 'Record surveys and boundaries'),
('LAND_SURVEY_UPDATE',      'Update land surveys',      'LAND', 'Edit survey records'),
('LAND_OWNERSHIP_VIEW',     'View land ownership',      'LAND', 'View ownership records and history'),
('LAND_OWNERSHIP_CREATE',   'Create land ownership',    'LAND', 'Register ownership records'),
('LAND_ACQUISITION_CREATE', 'Create land acquisitions', 'LAND', 'Register land acquisition requests'),
('LAND_ACQUISITION_APPROVE','Approve land acquisitions','LAND', 'Approve and complete acquisitions (sensitive)'),
('LAND_REPORT_VIEW',        'View land reports',        'LAND', 'View land dashboards and summaries');

-- ADMIN: every permission (matches V2/V3 pattern)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'LAND_SURVEY_VIEW','LAND_SURVEY_CREATE','LAND_SURVEY_UPDATE',
  'LAND_OWNERSHIP_VIEW','LAND_OWNERSHIP_CREATE',
  'LAND_ACQUISITION_CREATE','LAND_ACQUISITION_APPROVE','LAND_REPORT_VIEW')
WHERE r.code = 'ADMIN';

-- MANAGER: visibility + acquisition approval (sale/transfer approval already granted via LAND_SALE_APPROVE)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'LAND_SURVEY_VIEW','LAND_OWNERSHIP_VIEW','LAND_ACQUISITION_APPROVE','LAND_REPORT_VIEW')
WHERE r.code = 'MANAGER';

-- ACCOUNTANT: read access to land records, ownership and reports
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('LAND_VIEW','LAND_OWNERSHIP_VIEW','LAND_REPORT_VIEW')
WHERE r.code = 'ACCOUNTANT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- LAND_OFFICER: registration, surveys, ownership, request creation (no approvals)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'LAND_SURVEY_VIEW','LAND_SURVEY_CREATE','LAND_SURVEY_UPDATE',
  'LAND_OWNERSHIP_VIEW','LAND_OWNERSHIP_CREATE','LAND_ACQUISITION_CREATE','LAND_REPORT_VIEW')
WHERE r.code = 'LAND_OFFICER';
