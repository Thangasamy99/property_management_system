-- V1: Document Management schema.
-- Business references to other services are external refs (no cross-DB FKs).

CREATE TABLE document_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    code        VARCHAR(40)  NOT NULL,
    name        VARCHAR(120) NOT NULL,
    domain      VARCHAR(20)  NOT NULL,        -- PROPERTY | LAND | FINANCE | ADMINISTRATION
    description VARCHAR(255) NULL,
    requires_expiry TINYINT(1) NOT NULL DEFAULT 0,
    active      TINYINT(1)   NOT NULL DEFAULT 1,
    created_at  DATETIME(6)  NOT NULL,
    CONSTRAINT pk_doc_categories PRIMARY KEY (id),
    CONSTRAINT uk_doccat_code UNIQUE (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE documents (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    document_ref      VARCHAR(20)  NOT NULL,
    category_id       BIGINT       NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    mime_type         VARCHAR(120) NOT NULL,
    file_size         BIGINT       NOT NULL,
    description       VARCHAR(500) NULL,
    document_date     DATE         NULL,
    expiry_date       DATE         NULL,
    current_version   INT          NOT NULL DEFAULT 1,
    status            VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | ARCHIVED
    uploaded_by_id    BIGINT       NOT NULL,
    uploaded_by_name  VARCHAR(120) NOT NULL,
    created_at        DATETIME(6)  NOT NULL,
    updated_at        DATETIME(6)  NOT NULL,
    CONSTRAINT pk_documents PRIMARY KEY (id),
    CONSTRAINT uk_doc_ref UNIQUE (document_ref),
    CONSTRAINT fk_doc_category FOREIGN KEY (category_id) REFERENCES document_categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_doc_status_expiry ON documents (status, expiry_date);
CREATE INDEX idx_doc_category ON documents (category_id);
CREATE INDEX idx_doc_uploaded_by ON documents (uploaded_by_id);

CREATE TABLE document_versions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    document_id     BIGINT       NOT NULL,
    version_number  INT          NOT NULL,
    storage_key     VARCHAR(80)  NOT NULL,   -- UUID; never the client filename
    file_size       BIGINT       NOT NULL,
    mime_type       VARCHAR(120) NOT NULL,
    uploaded_by_id  BIGINT       NOT NULL,
    uploaded_by_name VARCHAR(120) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_doc_versions PRIMARY KEY (id),
    CONSTRAINT fk_docver_doc FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT uk_docver UNIQUE (document_id, version_number),
    CONSTRAINT uk_docver_key UNIQUE (storage_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE document_associations (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    document_id     BIGINT       NOT NULL,
    entity_type     VARCHAR(30)  NOT NULL,
    entity_ref      VARCHAR(80)  NOT NULL,   -- external ID/reference from another service
    associated_by_id   BIGINT    NOT NULL,
    associated_by_name VARCHAR(120) NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_doc_associations PRIMARY KEY (id),
    CONSTRAINT fk_docassoc_doc FOREIGN KEY (document_id) REFERENCES documents (id),
    CONSTRAINT uk_docassoc UNIQUE (document_id, entity_type, entity_ref)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_docassoc_entity ON document_associations (entity_type, entity_ref);

CREATE TABLE document_audit_logs (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    document_id  BIGINT       NOT NULL,
    action       VARCHAR(30)  NOT NULL,      -- UPLOAD|NEW_VERSION|DOWNLOAD|PREVIEW|UPDATE|ASSOCIATE|DISASSOCIATE|ARCHIVE|RESTORE
    actor_id     BIGINT       NOT NULL,
    actor_name   VARCHAR(120) NOT NULL,
    detail       VARCHAR(500) NULL,
    created_at   DATETIME(6)  NOT NULL,
    CONSTRAINT pk_doc_audit PRIMARY KEY (id),
    CONSTRAINT fk_docaudit_doc FOREIGN KEY (document_id) REFERENCES documents (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_docaudit_doc ON document_audit_logs (document_id);
CREATE INDEX idx_docaudit_actor ON document_audit_logs (actor_id);

-- Seed categories across the four domains
INSERT INTO document_categories (code, name, domain, description, requires_expiry, created_at) VALUES
('PROP_OWNERSHIP',   'Property ownership document',    'PROPERTY', 'Title/ownership proof for a property', 0, NOW(6)),
('PROP_PLAN',        'Building plan',                  'PROPERTY', 'Architectural or engineering plan', 0, NOW(6)),
('PROP_PHOTO',       'Property photograph',            'PROPERTY', 'Photographic record of property', 0, NOW(6)),
('PROP_INSURANCE',   'Insurance document',             'PROPERTY', 'Insurance policy/certificate', 1, NOW(6)),
('PROP_REGISTRATION','Registration certificate',       'PROPERTY', 'Property registration certificate', 0, NOW(6)),
('RENTAL_AGREEMENT', 'Rental agreement',               'PROPERTY', 'Signed tenancy/rental agreement', 1, NOW(6)),
('LAND_OWNERSHIP',   'Land ownership certificate',     'LAND', 'Certificate of ownership', 0, NOW(6)),
('LAND_TITLE',       'Land title deed',                'LAND', 'Title deed document', 0, NOW(6)),
('LAND_SURVEY',      'Survey plan',                    'LAND', 'Survey plan / coordinates record', 0, NOW(6)),
('LAND_BOUNDARY',    'Boundary document',              'LAND', 'Boundary demarcation document', 0, NOW(6)),
('LAND_ACQ_AGREEMENT','Land acquisition agreement',    'LAND', 'Acquisition agreement with seller', 0, NOW(6)),
('LAND_SALE_AGREEMENT','Land sale agreement',          'LAND', 'Sale agreement with buyer', 0, NOW(6)),
('LAND_TRANSFER',    'Ownership transfer document',    'LAND', 'Transfer deed/document', 0, NOW(6)),
('FIN_RECEIPT',      'Payment receipt',                'FINANCE', 'Payment receipt copy', 0, NOW(6)),
('FIN_EXPENSE',      'Expense invoice',                'FINANCE', 'Vendor invoice / expense evidence', 0, NOW(6)),
('FIN_SUPPORT',      'Payment supporting document',    'FINANCE', 'Evidence supporting a payment', 0, NOW(6)),
('FIN_BANK',         'Bank statement',                 'FINANCE', 'Bank statement', 0, NOW(6)),
('FIN_REFUND',       'Refund supporting document',     'FINANCE', 'Evidence supporting a refund', 0, NOW(6)),
('ADM_EMPLOYEE',     'Employee document',              'ADMINISTRATION', 'Employee record document', 0, NOW(6)),
('ADM_GENERAL',      'General company document',       'ADMINISTRATION', 'Company document', 0, NOW(6)),
('ADM_OTHER',        'Other supporting document',      'ADMINISTRATION', 'Miscellaneous supporting document', 0, NOW(6));
