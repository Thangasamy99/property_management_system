-- ============================================================
-- PCC Property Service - V1 schema
-- Money: DECIMAL(18,0) XAF · Area: DECIMAL(15,3) m² — no floats
-- ============================================================

CREATE TABLE properties (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    code              VARCHAR(20)  NOT NULL UNIQUE,
    name              VARCHAR(150) NOT NULL,
    property_type     VARCHAR(30)  NOT NULL,
    description       TEXT         NULL,
    acquisition_date  DATE         NULL,
    acquisition_cost  DECIMAL(18,0) NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_by        BIGINT       NULL,      -- identity user id (cross-service ref, no FK)
    created_by_name   VARCHAR(120) NULL,
    deleted_at        DATETIME     NULL,
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_properties_status (status),
    INDEX idx_properties_type   (property_type),
    INDEX idx_properties_name   (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE property_addresses (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT       NOT NULL UNIQUE,
    line1       VARCHAR(200) NULL,
    line2       VARCHAR(200) NULL,
    city        VARCHAR(100) NULL,
    region      VARCHAR(100) NULL,
    country     VARCHAR(100) NULL DEFAULT 'Cameroon',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_addr_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE property_status_history (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id   BIGINT       NOT NULL,
    from_status   VARCHAR(30)  NULL,
    to_status     VARCHAR(30)  NOT NULL,
    reason        VARCHAR(500) NULL,
    actor_id      BIGINT       NULL,
    actor_username VARCHAR(120) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_psh_property (property_id),
    CONSTRAINT fk_psh_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE buildings (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id       BIGINT       NOT NULL,
    code              VARCHAR(20)  NOT NULL UNIQUE,
    name              VARCHAR(150) NOT NULL,
    number_of_floors  INT          NULL,
    construction_year INT          NULL,
    description       TEXT         NULL,
    status            VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_buildings_property (property_id),
    CONSTRAINT fk_building_property FOREIGN KEY (property_id) REFERENCES properties (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE apartments (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id      BIGINT       NOT NULL,
    apartment_number VARCHAR(20)  NOT NULL,
    floor_number     INT          NULL,
    bedrooms         INT          NULL,
    bathrooms        INT          NULL,
    area_sqm         DECIMAL(15,3) NULL,
    monthly_rent     DECIMAL(18,0) NULL,
    status           VARCHAR(30)  NOT NULL DEFAULT 'AVAILABLE',
    description      TEXT         NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_apartment_number_per_building (building_id, apartment_number),
    INDEX idx_apartments_status (status),
    CONSTRAINT fk_apartment_building FOREIGN KEY (building_id) REFERENCES buildings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tenants (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                   VARCHAR(20)  NOT NULL UNIQUE,
    tenant_type            VARCHAR(20)  NOT NULL DEFAULT 'INDIVIDUAL',
    name                   VARCHAR(150) NOT NULL,
    email                  VARCHAR(150) NULL,
    phone                  VARCHAR(40)  NULL,
    address                VARCHAR(255) NULL,
    identification_type    VARCHAR(40)  NULL,
    identification_number  VARCHAR(80)  NULL,
    emergency_contact_name VARCHAR(120) NULL,
    emergency_contact_phone VARCHAR(40) NULL,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenants_status (status),
    INDEX idx_tenants_name   (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rental_agreements (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_number   VARCHAR(20)  NOT NULL UNIQUE,
    apartment_id       BIGINT       NOT NULL,
    tenant_id          BIGINT       NOT NULL,
    start_date         DATE         NOT NULL,
    end_date           DATE         NOT NULL,
    monthly_rent       DECIMAL(18,0) NOT NULL,
    deposit            DECIMAL(18,0) NULL,
    payment_frequency  VARCHAR(20)  NOT NULL DEFAULT 'MONTHLY',
    rent_due_day       INT          NOT NULL DEFAULT 5,
    status             VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    termination_reason VARCHAR(500) NULL,
    created_by         BIGINT       NULL,
    created_by_name    VARCHAR(120) NULL,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agreements_apartment (apartment_id),
    INDEX idx_agreements_tenant    (tenant_id),
    INDEX idx_agreements_status    (status),
    CONSTRAINT fk_agreement_apartment FOREIGN KEY (apartment_id) REFERENCES apartments (id),
    CONSTRAINT fk_agreement_tenant    FOREIGN KEY (tenant_id)    REFERENCES tenants (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rental_agreement_terms (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_id BIGINT       NOT NULL,
    term_order   INT          NOT NULL,
    content      TEXT         NOT NULL,
    CONSTRAINT fk_term_agreement FOREIGN KEY (agreement_id) REFERENCES rental_agreements (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rent_schedules (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_id BIGINT        NOT NULL,
    period_start DATE          NOT NULL,
    period_end   DATE          NOT NULL,
    due_date     DATE          NOT NULL,
    amount       DECIMAL(18,0) NOT NULL,
    status       VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_schedule_period (agreement_id, period_start),
    INDEX idx_schedules_due (due_date, status),
    CONSTRAINT fk_schedule_agreement FOREIGN KEY (agreement_id) REFERENCES rental_agreements (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rental_agreement_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    agreement_id   BIGINT       NOT NULL,
    event          VARCHAR(40)  NOT NULL,
    from_status    VARCHAR(30)  NULL,
    to_status      VARCHAR(30)  NULL,
    note           VARCHAR(500) NULL,
    actor_id       BIGINT       NULL,
    actor_username VARCHAR(120) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agr_history (agreement_id),
    CONSTRAINT fk_agr_history FOREIGN KEY (agreement_id) REFERENCES rental_agreements (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE maintenance_requests (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_number        VARCHAR(20)  NOT NULL UNIQUE,
    apartment_id          BIGINT       NULL,
    building_id           BIGINT       NULL,
    category              VARCHAR(40)  NOT NULL,
    description           TEXT         NOT NULL,
    priority              VARCHAR(20)  NOT NULL DEFAULT 'MEDIUM',
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    assigned_employee_id  BIGINT       NULL,
    assigned_employee_name VARCHAR(120) NULL,
    completed_at          DATETIME     NULL,
    created_by            BIGINT       NULL,
    created_by_name       VARCHAR(120) NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_maint_status   (status),
    INDEX idx_maint_apartment (apartment_id),
    CONSTRAINT fk_maint_apartment FOREIGN KEY (apartment_id) REFERENCES apartments (id),
    CONSTRAINT fk_maint_building  FOREIGN KEY (building_id)  REFERENCES buildings (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE maintenance_history (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id     BIGINT       NOT NULL,
    event          VARCHAR(40)  NOT NULL,
    note           VARCHAR(500) NULL,
    actor_id       BIGINT       NULL,
    actor_username VARCHAR(120) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_maint_history (request_id),
    CONSTRAINT fk_maint_history FOREIGN KEY (request_id) REFERENCES maintenance_requests (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
