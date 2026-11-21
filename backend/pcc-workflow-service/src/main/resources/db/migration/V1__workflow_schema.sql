-- V1: Workflow & Approval schema.
-- Business records stay owned by their services; only external refs stored here.

CREATE TABLE workflow_definitions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    code            VARCHAR(40)  NOT NULL,
    name            VARCHAR(160) NOT NULL,
    description     VARCHAR(500) NULL,
    module          VARCHAR(20)  NOT NULL,        -- PROPERTY | LAND | FINANCE | DOCUMENT
    workflow_type   VARCHAR(50)  NOT NULL,        -- e.g. RENTAL_AGREEMENT_APPROVAL, EXPENSE_APPROVAL
    entity_type     VARCHAR(40)  NOT NULL,        -- external entity type
    integration_method VARCHAR(8) NULL,           -- POST | PATCH | PUT | null = no downstream call
    integration_path   VARCHAR(200) NULL,         -- e.g. /api/v1/expenses/{id}/approve
    escalation_days INT          NULL,
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    created_by_id   BIGINT       NULL,
    updated_by_id   BIGINT       NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_wf_definitions PRIMARY KEY (id),
    CONSTRAINT uk_wfdef_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE workflow_levels (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    definition_id   BIGINT       NOT NULL,
    level_order     INT          NOT NULL,
    name            VARCHAR(120) NOT NULL,
    approver_role   VARCHAR(40)  NULL,            -- role code, e.g. MANAGER
    approver_id     BIGINT       NULL,            -- specific employee (overrides role)
    min_amount      DECIMAL(19,2) NULL,           -- level applies only when amount >= min
    mandatory       TINYINT(1)   NOT NULL DEFAULT 1,
    CONSTRAINT pk_wf_levels PRIMARY KEY (id),
    CONSTRAINT fk_wflevel_def FOREIGN KEY (definition_id) REFERENCES workflow_definitions (id),
    CONSTRAINT uk_wflevel UNIQUE (definition_id, level_order)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE workflow_instances (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    workflow_ref    VARCHAR(20)  NOT NULL,
    definition_id   BIGINT       NOT NULL,
    workflow_type   VARCHAR(50)  NOT NULL,
    module          VARCHAR(20)  NOT NULL,
    entity_type     VARCHAR(40)  NOT NULL,
    entity_ref      VARCHAR(80)  NOT NULL,
    amount          DECIMAL(19,2) NULL,
    priority        VARCHAR(10)  NOT NULL DEFAULT 'NORMAL', -- LOW|NORMAL|HIGH|URGENT
    description     VARCHAR(500) NULL,
    document_refs   VARCHAR(500) NULL,            -- comma-separated DOC-###### refs
    status          VARCHAR(25)  NOT NULL,        -- DRAFT|SUBMITTED|PENDING_APPROVAL|APPROVED|REJECTED|
                                                  -- RETURNED_FOR_CORRECTION|CANCELLED|INTEGRATION_PENDING|
                                                  -- INTEGRATION_FAILED|COMPLETED
    current_level   INT          NOT NULL DEFAULT 0,
    submitted_by_id   BIGINT     NOT NULL,
    submitted_by_name VARCHAR(120) NOT NULL,
    submitted_at    DATETIME(6)  NULL,
    due_at          DATETIME(6)  NULL,
    completed_at    DATETIME(6)  NULL,
    integration_error VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,        -- optimistic lock
    CONSTRAINT pk_wf_instances PRIMARY KEY (id),
    CONSTRAINT uk_wf_ref UNIQUE (workflow_ref),
    CONSTRAINT fk_wfinst_def FOREIGN KEY (definition_id) REFERENCES workflow_definitions (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_wfinst_status_level ON workflow_instances (status, current_level);
CREATE INDEX idx_wfinst_entity ON workflow_instances (entity_type, entity_ref);
CREATE INDEX idx_wfinst_submitter ON workflow_instances (submitted_by_id);
CREATE INDEX idx_wfinst_module ON workflow_instances (module, status);

CREATE TABLE workflow_approval_tasks (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       NOT NULL,
    level_order     INT          NOT NULL,
    assignee_id     BIGINT       NULL,            -- resolved employee id; null = role-based
    assignee_role   VARCHAR(40)  NULL,
    status          VARCHAR(15)  NOT NULL,        -- PENDING|APPROVED|REJECTED|RETURNED|SKIPPED|CANCELLED
    acted_by_id     BIGINT       NULL,            -- actual actor (may be delegate)
    acted_by_name   VARCHAR(120) NULL,
    acted_at        DATETIME(6)  NULL,
    comment         VARCHAR(500) NULL,
    due_at          DATETIME(6)  NULL,
    delegated_from_id BIGINT     NULL,
    cycle           INT          NOT NULL DEFAULT 0, -- resubmission round (return-for-correction increments)
    created_at      DATETIME(6)  NOT NULL,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT pk_wf_tasks PRIMARY KEY (id),
    CONSTRAINT fk_wftask_inst FOREIGN KEY (instance_id) REFERENCES workflow_instances (id),
    CONSTRAINT uk_wftask UNIQUE (instance_id, level_order, cycle)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_wftask_assignee ON workflow_approval_tasks (assignee_id, status);
CREATE INDEX idx_wftask_role ON workflow_approval_tasks (assignee_role, status);
CREATE INDEX idx_wftask_due ON workflow_approval_tasks (status, due_at);

CREATE TABLE workflow_actions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       NOT NULL,
    task_id         BIGINT       NULL,
    action          VARCHAR(30)  NOT NULL,        -- SUBMIT|APPROVE|REJECT|RETURN|COMMENT|RESUBMIT|CANCEL|INFO_REQUEST
    level_order     INT          NULL,
    actor_id        BIGINT       NOT NULL,
    actor_name      VARCHAR(120) NOT NULL,
    comment         VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_wf_actions PRIMARY KEY (id),
    CONSTRAINT fk_wfact_inst FOREIGN KEY (instance_id) REFERENCES workflow_instances (id),
    CONSTRAINT fk_wfact_task FOREIGN KEY (task_id) REFERENCES workflow_approval_tasks (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_wfact_inst ON workflow_actions (instance_id, created_at);

CREATE TABLE workflow_delegations (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    delegator_id    BIGINT       NOT NULL,        -- original approver
    delegate_id     BIGINT       NOT NULL,
    delegate_name   VARCHAR(120) NOT NULL,
    module          VARCHAR(20)  NULL,            -- scope: null = all, else domain
    definition_id   BIGINT       NULL,            -- scope: specific definition
    start_date      DATE         NOT NULL,
    end_date        DATE         NOT NULL,
    reason          VARCHAR(300) NULL,
    status          VARCHAR(15)  NOT NULL,        -- ACTIVE|REVOKED|EXPIRED
    approved_by_id  BIGINT       NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_wf_delegations PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_wfdeleg ON workflow_delegations (delegate_id, status, start_date, end_date);

CREATE TABLE notifications (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    recipient_id    BIGINT       NULL,
    recipient_role  VARCHAR(40)  NULL,          -- role-targeted notification
    type            VARCHAR(30)  NOT NULL,        -- TASK_ASSIGNED|APPROVED|REJECTED|RETURNED|CANCELLED|OVERDUE|COMPLETED|DELEGATED
    title           VARCHAR(200) NOT NULL,
    message         VARCHAR(500) NULL,
    workflow_ref    VARCHAR(20)  NULL,
    read_flag       TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_notifications PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_notif_recipient ON notifications (recipient_id, read_flag, created_at);

CREATE TABLE workflow_audit_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    actor_id        BIGINT       NOT NULL,
    actor_name      VARCHAR(120) NOT NULL,
    detail          VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_wf_audit PRIMARY KEY (id),
    CONSTRAINT fk_wfaudit_inst FOREIGN KEY (instance_id) REFERENCES workflow_instances (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_wfaudit_inst ON workflow_audit_logs (instance_id);

CREATE TABLE workflow_integration_outbox (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id     BIGINT       NOT NULL,
    integration_ref VARCHAR(60)  NOT NULL,        -- WF-<ref>-I1
    service         VARCHAR(20)  NOT NULL,        -- PROPERTY|LAND|FINANCE|DOCUMENT
    method          VARCHAR(8)   NOT NULL,
    path            VARCHAR(200) NOT NULL,
    payload         TEXT         NULL,
    status          VARCHAR(15)  NOT NULL,        -- PENDING|SUCCESS|FAILED|PERMANENT
    retry_count     INT          NOT NULL DEFAULT 0,
    last_attempt_at DATETIME(6)  NULL,
    last_error      VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_wf_outbox PRIMARY KEY (id),
    CONSTRAINT fk_wfout_inst FOREIGN KEY (instance_id) REFERENCES workflow_instances (id),
    CONSTRAINT uk_wfout_ref UNIQUE (integration_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_wfout_status ON workflow_integration_outbox (status, retry_count);

-- Seed definitions (levels seeded below). integration_path uses {id} placeholder.
INSERT INTO workflow_definitions
(code, name, module, workflow_type, entity_type, integration_method, integration_path, escalation_days, active, created_at, updated_at) VALUES
('PROP_REGISTRATION',   'Property registration approval',   'PROPERTY', 'PROPERTY_REGISTRATION',   'PROPERTY',          'PATCH', '/api/v1/properties/{id}/status',          3, 1, NOW(6), NOW(6)),
('RENTAL_APPROVAL',     'Rental agreement approval',        'PROPERTY', 'RENTAL_AGREEMENT_APPROVAL','RENTAL_AGREEMENT', NULL, NULL, 3, 1, NOW(6), NOW(6)),
('RENTAL_TERMINATION',  'Rental termination approval',      'PROPERTY', 'RENTAL_TERMINATION',      'RENTAL_AGREEMENT',  NULL, NULL, 3, 1, NOW(6), NOW(6)),
('LAND_ACQ_APPROVAL',   'Land acquisition approval',        'LAND',     'LAND_ACQUISITION',        'LAND_ACQUISITION',  'POST', '/api/v1/land-acquisitions/{id}/approve',   3, 1, NOW(6), NOW(6)),
('LAND_SALE_APPROVAL',  'Land sale approval',               'LAND',     'LAND_SALE',               'LAND_SALE',         'POST', '/api/v1/land-sales/{id}/approve',          3, 1, NOW(6), NOW(6)),
('LAND_TRANSFER_APPROVAL','Ownership transfer approval',    'LAND',     'LAND_TRANSFER',           'LAND_TRANSFER',     NULL, NULL, 3, 1, NOW(6), NOW(6)),
('EXPENSE_APPROVAL',    'Expense approval',                 'FINANCE',  'EXPENSE',                 'EXPENSE',           'POST', '/api/v1/expenses/{id}/approve',            3, 1, NOW(6), NOW(6)),
('REFUND_APPROVAL',     'Refund approval',                  'FINANCE',  'REFUND',                  'FINANCE_PAYMENT',   'POST', '/api/v1/refunds/{id}/approve',             3, 1, NOW(6), NOW(6)),
('DOC_VERIFICATION',    'Document verification',            'DOCUMENT', 'DOCUMENT_VERIFICATION',   'DOCUMENT',          NULL, NULL, 5, 1, NOW(6), NOW(6));

INSERT INTO workflow_levels (definition_id, level_order, name, approver_role, mandatory)
SELECT d.id, 1, 'Manager approval', 'MANAGER', 1 FROM workflow_definitions d;
