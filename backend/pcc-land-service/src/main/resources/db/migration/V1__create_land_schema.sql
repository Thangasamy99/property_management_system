-- ============================================================
-- PCC Land Service schema (pcc_land_db)
-- Consolidated model: survey N/S/E/W boundaries live on
-- land_surveys; sale/transfer detail fields live on their
-- parents; land_ownership_history is an immutable event log.
-- ============================================================

CREATE TABLE land_parcels (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    parcel_reference   VARCHAR(20)  NOT NULL,
    name               VARCHAR(160) NOT NULL,
    description        TEXT         NULL,
    classification     VARCHAR(20)  NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    area_sqm           DECIMAL(15,3) NOT NULL,
    original_area      DECIMAL(15,3) NULL,
    original_unit      VARCHAR(20)   NULL,
    region             VARCHAR(100) NOT NULL,
    division           VARCHAR(100) NULL,
    subdivision        VARCHAR(100) NULL,
    locality           VARCHAR(120) NULL,
    address            VARCHAR(255) NULL,
    acquisition_date   DATE         NULL,
    created_by         BIGINT       NULL,
    created_by_name    VARCHAR(120) NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at         TIMESTAMP    NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_parcel_ref (parcel_reference),
    KEY idx_parcel_status (status),
    KEY idx_parcel_classification (classification),
    KEY idx_parcel_region (region)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_status_history (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    parcel_id       BIGINT      NOT NULL,
    previous_status VARCHAR(20) NULL,
    new_status      VARCHAR(20) NOT NULL,
    reason          VARCHAR(255) NULL,
    changed_by      BIGINT      NULL,
    changed_by_name VARCHAR(120) NULL,
    changed_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_lsh_parcel (parcel_id),
    CONSTRAINT fk_lsh_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_surveys (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    parcel_id             BIGINT       NOT NULL,
    survey_reference      VARCHAR(30)  NOT NULL,
    survey_date           DATE         NOT NULL,
    surveyor_name         VARCHAR(160) NULL,
    surveyor_organization VARCHAR(160) NULL,
    surveyed_area         DECIMAL(15,3) NULL,
    boundary_north        VARCHAR(255) NULL,
    boundary_south        VARCHAR(255) NULL,
    boundary_east         VARCHAR(255) NULL,
    boundary_west         VARCHAR(255) NULL,
    description           TEXT         NULL,
    document_reference    VARCHAR(120) NULL,
    created_by            BIGINT       NULL,
    created_by_name       VARCHAR(120) NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_survey_ref (survey_reference),
    KEY idx_survey_parcel (parcel_id),
    CONSTRAINT fk_survey_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_coordinates (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    parcel_id  BIGINT        NOT NULL,
    sequence   INT           NOT NULL,
    latitude   DECIMAL(10,7) NOT NULL,
    longitude  DECIMAL(11,7) NOT NULL,
    label      VARCHAR(80)   NULL,
    created_at TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_coord_seq (parcel_id, sequence),
    CONSTRAINT fk_coord_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_ownership (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    parcel_id           BIGINT       NOT NULL,
    ownership_reference VARCHAR(30)  NOT NULL,
    owner_name          VARCHAR(160) NOT NULL,
    owner_reference     VARCHAR(80)  NULL,
    title_reference     VARCHAR(120) NULL,
    share_percent       DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    acquisition_date    DATE         NULL,
    effective_date      DATE         NOT NULL,
    end_date            DATE         NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'CURRENT',
    document_reference  VARCHAR(120) NULL,
    created_by          BIGINT       NULL,
    created_by_name     VARCHAR(120) NULL,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ownership_ref (ownership_reference),
    KEY idx_own_parcel_status (parcel_id, status),
    CONSTRAINT fk_own_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_ownership_history (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    parcel_id        BIGINT      NOT NULL,
    ownership_id     BIGINT      NULL,
    event_type       VARCHAR(30) NOT NULL,
    previous_owner   VARCHAR(160) NULL,
    new_owner        VARCHAR(160) NULL,
    effective_date   DATE        NULL,
    performed_by     BIGINT      NULL,
    performed_by_name VARCHAR(120) NULL,
    note             VARCHAR(255) NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_loh_parcel (parcel_id),
    CONSTRAINT fk_loh_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id),
    CONSTRAINT fk_loh_ownership FOREIGN KEY (ownership_id) REFERENCES land_ownership (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_acquisitions (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    parcel_id            BIGINT       NOT NULL,
    acquisition_reference VARCHAR(30) NOT NULL,
    seller_name          VARCHAR(160) NOT NULL,
    seller_reference     VARCHAR(80)  NULL,
    seller_contact       VARCHAR(120) NULL,
    acquisition_date     DATE         NULL,
    purchase_price       DECIMAL(18,0) NULL,
    document_reference   VARCHAR(120) NULL,
    approval_reference   VARCHAR(80)  NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    decided_by           BIGINT       NULL,
    decided_by_name      VARCHAR(120) NULL,
    decided_at           TIMESTAMP    NULL,
    decision_note        VARCHAR(255) NULL,
    created_by           BIGINT       NULL,
    created_by_name      VARCHAR(120) NULL,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_acq_ref (acquisition_reference),
    KEY idx_acq_parcel_status (parcel_id, status),
    CONSTRAINT fk_acq_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_sales (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    parcel_id            BIGINT       NOT NULL,
    sale_reference       VARCHAR(30)  NOT NULL,
    buyer_name           VARCHAR(160) NOT NULL,
    buyer_reference      VARCHAR(80)  NULL,
    buyer_contact        VARCHAR(120) NULL,
    proposed_price       DECIMAL(18,0) NOT NULL,
    agreed_price         DECIMAL(18,0) NULL,
    agreement_reference  VARCHAR(120) NULL,
    agreement_date       DATE         NULL,
    document_reference   VARCHAR(120) NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    submitted_at         TIMESTAMP    NULL,
    decided_by           BIGINT       NULL,
    decided_by_name      VARCHAR(120) NULL,
    decided_at           TIMESTAMP    NULL,
    decision_note        VARCHAR(255) NULL,
    created_by            BIGINT       NULL,
    created_by_name       VARCHAR(120) NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sale_ref (sale_reference),
    KEY idx_sale_parcel_status (parcel_id, status),
    CONSTRAINT fk_sale_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_transfers (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    parcel_id             BIGINT       NOT NULL,
    transfer_reference    VARCHAR(30)  NOT NULL,
    previous_ownership_id BIGINT       NULL,
    previous_owner_name   VARCHAR(160) NULL,
    new_owner_name        VARCHAR(160) NOT NULL,
    new_owner_reference   VARCHAR(80)  NULL,
    sale_id               BIGINT       NULL,
    title_reference       VARCHAR(120) NULL,
    document_reference    VARCHAR(120) NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    legal_completion_date DATE         NULL,
    submitted_at          TIMESTAMP    NULL,
    decided_by            BIGINT       NULL,
    decided_by_name       VARCHAR(120) NULL,
    decided_at            TIMESTAMP    NULL,
    decision_note         VARCHAR(255) NULL,
    created_by            BIGINT       NULL,
    created_by_name       VARCHAR(120) NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_transfer_ref (transfer_reference),
    KEY idx_trf_parcel_status (parcel_id, status),
    CONSTRAINT fk_trf_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id),
    CONSTRAINT fk_trf_prev_own FOREIGN KEY (previous_ownership_id) REFERENCES land_ownership (id),
    CONSTRAINT fk_trf_sale FOREIGN KEY (sale_id) REFERENCES land_sales (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE land_transactions (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    parcel_id        BIGINT      NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    reference_id     BIGINT      NULL,
    reference_code   VARCHAR(40) NULL,
    amount           DECIMAL(18,0) NULL,
    status           VARCHAR(30) NULL,
    actor_id         BIGINT      NULL,
    actor_name       VARCHAR(120) NULL,
    description      VARCHAR(255) NULL,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_tx_parcel (parcel_id),
    KEY idx_tx_type (transaction_type),
    CONSTRAINT fk_tx_parcel FOREIGN KEY (parcel_id) REFERENCES land_parcels (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
