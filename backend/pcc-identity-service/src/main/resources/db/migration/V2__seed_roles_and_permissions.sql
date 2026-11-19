-- ============================================================
-- PCC Identity Service - Seed roles, permissions, role mappings
-- ============================================================

INSERT INTO roles (code, name, description) VALUES
('ADMIN',            'Admin',            'System administrator - full access'),
('MANAGER',          'Manager',          'Operations manager - visibility and approvals'),
('ACCOUNTANT',       'Accountant',       'Finance staff - payments and expenses'),
('PROPERTY_OFFICER', 'Property Officer', 'Buildings, apartments, tenants, rentals'),
('LAND_OFFICER',     'Land Officer',     'Land parcels, documents, acquisitions');

INSERT INTO permissions (code, name, module, description) VALUES
-- Authentication (baseline for every employee)
('AUTH_LOGIN',           'Login',                    'AUTH',     'Authenticate to the system'),
('AUTH_LOGOUT',          'Logout',                   'AUTH',     'Terminate session'),
('AUTH_CHANGE_PASSWORD', 'Change own password',      'AUTH',     'Change own password'),
-- User management
('USER_VIEW',            'View users',               'USER',     'View and search employee accounts'),
('USER_CREATE',          'Create users',             'USER',     'Create employee accounts'),
('USER_UPDATE',          'Update users',             'USER',     'Update employee accounts and reset passwords'),
('USER_DEACTIVATE',      'Deactivate users',         'USER',     'Activate / deactivate employee accounts'),
('USER_ASSIGN_ROLE',     'Assign roles',             'USER',     'Assign and remove user roles'),
-- Role management
('ROLE_VIEW',            'View roles',               'ROLE',     'View roles and their permissions'),
('ROLE_CREATE',          'Create roles',             'ROLE',     'Create roles'),
('ROLE_UPDATE',          'Update roles',             'ROLE',     'Update roles'),
('ROLE_ASSIGN_PERMISSION','Assign role permissions', 'ROLE',     'Change role-permission mapping'),
-- Property
('PROPERTY_VIEW',        'View properties',          'PROPERTY', 'View buildings, apartments and property assets'),
('PROPERTY_CREATE',      'Create properties',        'PROPERTY', 'Create property records'),
('PROPERTY_UPDATE',      'Update properties',        'PROPERTY', 'Update property records'),
('PROPERTY_DELETE',      'Delete properties',        'PROPERTY', 'Delete property records (sensitive)'),
('TENANT_VIEW',          'View tenants',             'PROPERTY', 'View tenants'),
('TENANT_MANAGE',        'Manage tenants',           'PROPERTY', 'Create and update tenants'),
('RENTAL_VIEW',          'View rentals',             'PROPERTY', 'View rental agreements'),
('RENTAL_MANAGE',        'Manage rentals',           'PROPERTY', 'Create and update rental agreements'),
('MAINTENANCE_VIEW',     'View maintenance',         'PROPERTY', 'View maintenance records'),
('MAINTENANCE_MANAGE',   'Manage maintenance',       'PROPERTY', 'Create and update maintenance records'),
-- Land
('LAND_VIEW',                'View land',                  'LAND', 'View land parcels and records'),
('LAND_CREATE',              'Create land records',        'LAND', 'Create land parcels and records'),
('LAND_UPDATE',              'Update land records',        'LAND', 'Update land parcels and records'),
('LAND_DELETE',              'Delete land records',        'LAND', 'Delete land records (sensitive)'),
('LAND_DOCUMENT_MANAGE',     'Manage land documents',      'LAND', 'Manage land ownership documents'),
('LAND_ACQUISITION_MANAGE',  'Manage land acquisitions',   'LAND', 'Manage land acquisition records'),
('LAND_SALE_REQUEST',        'Request land sale/transfer', 'LAND', 'Create land sale and transfer requests'),
('LAND_SALE_APPROVE',        'Approve land sale/transfer', 'LAND', 'Approve land sale and transfer requests (sensitive)'),
-- Finance
('PAYMENT_VIEW',         'View payments',            'FINANCE', 'View payments'),
('PAYMENT_CREATE',       'Create payments',          'FINANCE', 'Record payments'),
('PAYMENT_APPROVE',      'Approve payments',         'FINANCE', 'Approve payments (sensitive)'),
('EXPENSE_VIEW',         'View expenses',            'FINANCE', 'View expenses'),
('EXPENSE_CREATE',       'Create expenses',          'FINANCE', 'Record expenses'),
('EXPENSE_APPROVE',      'Approve expenses',         'FINANCE', 'Approve expenses (sensitive)'),
('FINANCIAL_REPORT_VIEW','View financial reports',   'FINANCE', 'View financial reports'),
-- Reports
('REPORT_VIEW',          'View reports',             'REPORT',  'View operational reports'),
-- System
('SETTINGS_MANAGE',      'Manage settings',          'SYSTEM',  'Manage system configuration'),
('AUDIT_LOG_VIEW',       'View audit logs',          'SYSTEM',  'View audit logs');

-- ---------- Role <-> permission mapping ----------
-- ADMIN: every permission
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.code = 'ADMIN';

-- MANAGER: visibility + approvals + reports (no user/role/security admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'AUTH_LOGIN','AUTH_LOGOUT','AUTH_CHANGE_PASSWORD',
  'USER_VIEW',
  'PROPERTY_VIEW','TENANT_VIEW','RENTAL_VIEW','MAINTENANCE_VIEW',
  'LAND_VIEW','LAND_SALE_APPROVE',
  'PAYMENT_VIEW','PAYMENT_APPROVE','EXPENSE_VIEW','EXPENSE_APPROVE','FINANCIAL_REPORT_VIEW',
  'REPORT_VIEW'
) WHERE r.code = 'MANAGER';

-- ACCOUNTANT: finance operations (creates, not approvals)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'AUTH_LOGIN','AUTH_LOGOUT','AUTH_CHANGE_PASSWORD',
  'PAYMENT_VIEW','PAYMENT_CREATE',
  'EXPENSE_VIEW','EXPENSE_CREATE',
  'FINANCIAL_REPORT_VIEW','REPORT_VIEW'
) WHERE r.code = 'ACCOUNTANT';

-- PROPERTY_OFFICER: property operations (no deletes, no approvals)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'AUTH_LOGIN','AUTH_LOGOUT','AUTH_CHANGE_PASSWORD',
  'PROPERTY_VIEW','PROPERTY_CREATE','PROPERTY_UPDATE',
  'TENANT_VIEW','TENANT_MANAGE',
  'RENTAL_VIEW','RENTAL_MANAGE',
  'MAINTENANCE_VIEW','MAINTENANCE_MANAGE'
) WHERE r.code = 'PROPERTY_OFFICER';

-- LAND_OFFICER: land operations (creates requests, does not approve)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r JOIN permissions p ON p.code IN (
  'AUTH_LOGIN','AUTH_LOGOUT','AUTH_CHANGE_PASSWORD',
  'LAND_VIEW','LAND_CREATE','LAND_UPDATE',
  'LAND_DOCUMENT_MANAGE','LAND_ACQUISITION_MANAGE','LAND_SALE_REQUEST'
) WHERE r.code = 'LAND_OFFICER';
