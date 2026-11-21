-- pcc_reporting_db — reporting-owned metadata only. No copies of
-- operational service tables; source data is fetched over authorized APIs.

CREATE TABLE report_definitions (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    report_key      VARCHAR(80)  NOT NULL,
    name            VARCHAR(200) NOT NULL,
    domain          VARCHAR(20)  NOT NULL,      -- PROPERTY|LAND|FINANCE|WORKFLOW|DOCUMENT|EMPLOYEE|AUDIT
    description     VARCHAR(500) NULL,
    source_service  VARCHAR(20)  NOT NULL,
    source_path     VARCHAR(300) NOT NULL,
    permission      VARCHAR(60)  NOT NULL,
    columns_json    TEXT         NOT NULL,      -- ordered [{key,label,type}]
    exportable      TINYINT(1)   NOT NULL DEFAULT 1,
    active          TINYINT(1)   NOT NULL DEFAULT 1,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_repdef PRIMARY KEY (id),
    CONSTRAINT uk_repdef_key UNIQUE (report_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE report_cache_entries (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    cache_key       VARCHAR(200) NOT NULL,
    payload         MEDIUMTEXT   NOT NULL,
    source_health   VARCHAR(20)  NOT NULL,      -- OK|PARTIAL|DOWN
    fetched_at      DATETIME(6)  NOT NULL,
    expires_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_repcache PRIMARY KEY (id),
    CONSTRAINT uk_repcache_key UNIQUE (cache_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_repcache_exp ON report_cache_entries (expires_at);

CREATE TABLE report_execution_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    report_key      VARCHAR(80)  NOT NULL,
    report_name     VARCHAR(200) NOT NULL,
    executed_by_id  BIGINT       NOT NULL,
    executed_by     VARCHAR(120) NOT NULL,
    params_json     TEXT         NULL,
    row_count       INT          NULL,
    source_health   VARCHAR(20)  NULL,
    duration_ms     BIGINT       NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_repexec PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_repexec_user ON report_execution_logs (executed_by_id, created_at);
CREATE INDEX idx_repexec_key ON report_execution_logs (report_key, created_at);

CREATE TABLE report_export_logs (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    report_key      VARCHAR(80)  NOT NULL,
    format          VARCHAR(10)  NOT NULL,      -- CSV|XLSX|PDF
    filename        VARCHAR(200) NOT NULL,
    exported_by_id  BIGINT       NOT NULL,
    exported_by     VARCHAR(120) NOT NULL,
    row_count       INT          NULL,
    created_at      DATETIME(6)  NOT NULL,
    CONSTRAINT pk_repexp PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE INDEX idx_repexp_user ON report_export_logs (exported_by_id, created_at);
