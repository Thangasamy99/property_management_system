-- V7: Workflow Service permission set.
-- Least-privilege: officers submit requests in their domain, MANAGER approves,
-- ACCOUNTANT submits finance requests, ADMIN manages definitions + all.
INSERT INTO permissions (code, name, module, description) VALUES
('WORKFLOW_VIEW',          'View workflows',           'WORKFLOW', 'View workflow requests and tasks'),
('WORKFLOW_CREATE',        'Create workflow requests', 'WORKFLOW', 'Draft workflow requests'),
('WORKFLOW_SUBMIT',        'Submit workflow requests', 'WORKFLOW', 'Submit requests for approval'),
('WORKFLOW_APPROVE',       'Approve workflows',        'WORKFLOW', 'Approve assigned approval tasks (sensitive)'),
('WORKFLOW_REJECT',        'Reject workflows',         'WORKFLOW', 'Reject assigned approval tasks (sensitive)'),
('WORKFLOW_RETURN',        'Return workflows',         'WORKFLOW', 'Return requests for correction (sensitive)'),
('WORKFLOW_CANCEL',        'Cancel workflows',         'WORKFLOW', 'Cancel own submitted requests'),
('WORKFLOW_CONFIGURE',     'Configure workflows',      'WORKFLOW', 'Manage workflow definitions and levels (admin)'),
('WORKFLOW_ASSIGN',        'Assign approvers',         'WORKFLOW', 'Reassign approval tasks (admin)'),
('WORKFLOW_DELEGATE',      'Delegate approvals',       'WORKFLOW', 'Create approval delegations'),
('WORKFLOW_HISTORY_VIEW',  'View workflow history',    'WORKFLOW', 'View approval history and audit'),
('WORKFLOW_REPORT_VIEW',   'View workflow reports',    'WORKFLOW', 'View workflow summaries and overdue reports'),
('NOTIFICATION_VIEW',      'View notifications',       'WORKFLOW', 'View own notifications');

-- ADMIN: everything
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'WORKFLOW_VIEW','WORKFLOW_CREATE','WORKFLOW_SUBMIT','WORKFLOW_APPROVE','WORKFLOW_REJECT',
  'WORKFLOW_RETURN','WORKFLOW_CANCEL','WORKFLOW_CONFIGURE','WORKFLOW_ASSIGN','WORKFLOW_DELEGATE',
  'WORKFLOW_HISTORY_VIEW','WORKFLOW_REPORT_VIEW','NOTIFICATION_VIEW')
WHERE r.code = 'ADMIN';

-- MANAGER: approvals + oversight + delegation (no definition admin)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'WORKFLOW_VIEW','WORKFLOW_CREATE','WORKFLOW_SUBMIT','WORKFLOW_APPROVE','WORKFLOW_REJECT',
  'WORKFLOW_RETURN','WORKFLOW_CANCEL','WORKFLOW_DELEGATE','WORKFLOW_HISTORY_VIEW',
  'WORKFLOW_REPORT_VIEW','NOTIFICATION_VIEW')
WHERE r.code = 'MANAGER';

-- ACCOUNTANT: submit + view + approve where assigned
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'WORKFLOW_VIEW','WORKFLOW_CREATE','WORKFLOW_SUBMIT','WORKFLOW_APPROVE','WORKFLOW_CANCEL',
  'WORKFLOW_HISTORY_VIEW','NOTIFICATION_VIEW')
WHERE r.code = 'ACCOUNTANT';

-- PROPERTY_OFFICER: submit and view own-domain requests
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'WORKFLOW_VIEW','WORKFLOW_CREATE','WORKFLOW_SUBMIT','WORKFLOW_CANCEL',
  'WORKFLOW_HISTORY_VIEW','NOTIFICATION_VIEW')
WHERE r.code = 'PROPERTY_OFFICER';

-- LAND_OFFICER: submit and view own-domain requests
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r
JOIN permissions p ON p.code IN (
  'WORKFLOW_VIEW','WORKFLOW_CREATE','WORKFLOW_SUBMIT','WORKFLOW_CANCEL',
  'WORKFLOW_HISTORY_VIEW','NOTIFICATION_VIEW')
WHERE r.code = 'LAND_OFFICER';
