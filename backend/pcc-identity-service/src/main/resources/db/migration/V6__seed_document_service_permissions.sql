-- V6: Document Service permission set.
-- Least-privilege: officers get domain-scoped document access, ACCOUNTANT gets
-- finance-document access, MANAGER gets operational + archive, ADMIN gets all.
INSERT INTO permissions (code, name, module, description) VALUES
('DOCUMENT_VIEW',           'View documents',             'DOCUMENT', 'View document metadata and lists'),
('DOCUMENT_UPLOAD',         'Upload documents',           'DOCUMENT', 'Upload new documents'),
('DOCUMENT_UPDATE',         'Update document metadata',   'DOCUMENT', 'Edit document metadata and associations'),
('DOCUMENT_DOWNLOAD',       'Download documents',         'DOCUMENT', 'Download document files'),
('DOCUMENT_PREVIEW',        'Preview documents',          'DOCUMENT', 'Inline preview of supported formats'),
('DOCUMENT_ARCHIVE',        'Archive documents',          'DOCUMENT', 'Archive (soft-delete) documents'),
('DOCUMENT_RESTORE',        'Restore documents',          'DOCUMENT', 'Restore archived documents'),
('DOCUMENT_VERSION_VIEW',   'View document versions',     'DOCUMENT', 'View and download document version history'),
('DOCUMENT_VERSION_UPLOAD', 'Upload document versions',   'DOCUMENT', 'Upload new versions of existing documents'),
('DOCUMENT_ASSOCIATE',      'Associate documents',        'DOCUMENT', 'Link documents to business records'),
('DOCUMENT_CATEGORY_VIEW',  'View document categories',   'DOCUMENT', 'View document categories'),
('DOCUMENT_CATEGORY_MANAGE','Manage document categories', 'DOCUMENT', 'Create and maintain document categories'),
('DOCUMENT_AUDIT_VIEW',     'View document audit',        'DOCUMENT', 'View document access and audit history'),
('DOCUMENT_EXPIRY_VIEW',    'View document expiry',       'DOCUMENT', 'View expiry tracking and dashboard summaries');

-- ADMIN: everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'DOCUMENT_VIEW','DOCUMENT_UPLOAD','DOCUMENT_UPDATE','DOCUMENT_DOWNLOAD','DOCUMENT_PREVIEW',
  'DOCUMENT_ARCHIVE','DOCUMENT_RESTORE','DOCUMENT_VERSION_VIEW','DOCUMENT_VERSION_UPLOAD',
  'DOCUMENT_ASSOCIATE','DOCUMENT_CATEGORY_VIEW','DOCUMENT_CATEGORY_MANAGE',
  'DOCUMENT_AUDIT_VIEW','DOCUMENT_EXPIRY_VIEW')
WHERE r.code = 'ADMIN';

-- MANAGER: operational + archive/restore + audit (no category admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'DOCUMENT_VIEW','DOCUMENT_UPLOAD','DOCUMENT_UPDATE','DOCUMENT_DOWNLOAD','DOCUMENT_PREVIEW',
  'DOCUMENT_ARCHIVE','DOCUMENT_RESTORE','DOCUMENT_VERSION_VIEW','DOCUMENT_VERSION_UPLOAD',
  'DOCUMENT_ASSOCIATE','DOCUMENT_CATEGORY_VIEW','DOCUMENT_AUDIT_VIEW','DOCUMENT_EXPIRY_VIEW')
WHERE r.code = 'MANAGER';

-- ACCOUNTANT: finance documents — view/download/preview/upload/associate/expiry
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'DOCUMENT_VIEW','DOCUMENT_UPLOAD','DOCUMENT_UPDATE','DOCUMENT_DOWNLOAD','DOCUMENT_PREVIEW',
  'DOCUMENT_VERSION_VIEW','DOCUMENT_VERSION_UPLOAD','DOCUMENT_ASSOCIATE',
  'DOCUMENT_CATEGORY_VIEW','DOCUMENT_EXPIRY_VIEW')
WHERE r.code = 'ACCOUNTANT';

-- PROPERTY_OFFICER: property/rental docs — view/download/upload/version/associate
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'DOCUMENT_VIEW','DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD','DOCUMENT_PREVIEW',
  'DOCUMENT_VERSION_VIEW','DOCUMENT_VERSION_UPLOAD','DOCUMENT_ASSOCIATE',
  'DOCUMENT_CATEGORY_VIEW','DOCUMENT_EXPIRY_VIEW')
WHERE r.code = 'PROPERTY_OFFICER';

-- LAND_OFFICER: land docs — same operational scope
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'DOCUMENT_VIEW','DOCUMENT_UPLOAD','DOCUMENT_DOWNLOAD','DOCUMENT_PREVIEW',
  'DOCUMENT_VERSION_VIEW','DOCUMENT_VERSION_UPLOAD','DOCUMENT_ASSOCIATE',
  'DOCUMENT_CATEGORY_VIEW','DOCUMENT_EXPIRY_VIEW')
WHERE r.code = 'LAND_OFFICER';
