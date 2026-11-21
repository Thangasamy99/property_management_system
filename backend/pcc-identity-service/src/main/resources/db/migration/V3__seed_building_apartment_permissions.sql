-- ============================================================
-- Building & Apartment permissions (Property Service)
-- ============================================================

INSERT INTO permissions (code, name, module, description) VALUES
('BUILDING_VIEW',   'View buildings',   'PROPERTY', 'View buildings'),
('BUILDING_CREATE', 'Create buildings', 'PROPERTY', 'Create buildings'),
('BUILDING_UPDATE', 'Update buildings', 'PROPERTY', 'Update buildings'),
('APARTMENT_VIEW',   'View apartments',   'PROPERTY', 'View apartments'),
('APARTMENT_CREATE', 'Create apartments', 'PROPERTY', 'Create apartments'),
('APARTMENT_UPDATE', 'Update apartments', 'PROPERTY', 'Update apartments');

-- ADMIN: every permission (matches V2 pattern)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'BUILDING_VIEW','BUILDING_CREATE','BUILDING_UPDATE',
  'APARTMENT_VIEW','APARTMENT_CREATE','APARTMENT_UPDATE')
WHERE r.code = 'ADMIN';

-- MANAGER: read-only on buildings/apartments
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('BUILDING_VIEW','APARTMENT_VIEW')
WHERE r.code = 'MANAGER';

-- ACCOUNTANT: read-only
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN ('BUILDING_VIEW','APARTMENT_VIEW','PROPERTY_VIEW','RENTAL_VIEW')
WHERE r.code = 'ACCOUNTANT'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp WHERE rp.role_id = r.id AND rp.permission_id = p.id);

-- PROPERTY_OFFICER: full building/apartment management
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'BUILDING_VIEW','BUILDING_CREATE','BUILDING_UPDATE',
  'APARTMENT_VIEW','APARTMENT_CREATE','APARTMENT_UPDATE')
WHERE r.code = 'PROPERTY_OFFICER';
