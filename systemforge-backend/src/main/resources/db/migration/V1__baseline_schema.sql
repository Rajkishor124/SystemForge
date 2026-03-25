-- ============================================================
-- SystemForge — V1 Baseline Schema (PostgreSQL)
-- ============================================================
-- Convention:
--   - All primary keys: UUID (native PostgreSQL type)
--   - All tables: soft delete via is_deleted BOOLEAN
--   - All tables: full audit columns (created_at, updated_at, created_by, updated_by)
--   - All tables: version BIGINT for optimistic locking
-- ============================================================

-- ─── USERS ────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS users (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    name              VARCHAR(100)    NOT NULL,
    email             VARCHAR(255)    NOT NULL,
    password          VARCHAR(72),
    role              VARCHAR(20)     NOT NULL DEFAULT 'DEVELOPER',
    account_status    VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    auth_provider     VARCHAR(20)     NOT NULL DEFAULT 'LOCAL',
    is_email_verified BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    last_login_at     TIMESTAMP(6),
    created_at        TIMESTAMP(6)    NOT NULL,
    updated_at        TIMESTAMP(6)    NOT NULL,
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE INDEX IF NOT EXISTS idx_users_email       ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_role        ON users (role);
CREATE INDEX IF NOT EXISTS idx_users_status_role ON users (account_status, role);
CREATE INDEX IF NOT EXISTS idx_users_is_deleted  ON users (is_deleted);

-- ─── OTP RECORDS ──────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS otp_records (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    email       VARCHAR(255)    NOT NULL,
    otp_hash    VARCHAR(72)     NOT NULL,
    auth_type   VARCHAR(30)     NOT NULL,
    expires_at  TIMESTAMP(6)    NOT NULL,
    is_used     BOOLEAN         NOT NULL DEFAULT FALSE,
    attempts    INT             NOT NULL DEFAULT 0,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP(6)    NOT NULL,
    updated_at  TIMESTAMP(6)    NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_otp_records PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_otp_email      ON otp_records (email);
CREATE INDEX IF NOT EXISTS idx_otp_expires    ON otp_records (expires_at);
CREATE INDEX IF NOT EXISTS idx_otp_email_used ON otp_records (email, is_used);

-- ─── REFRESH TOKENS ───────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id           UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id      UUID            NOT NULL,
    token_hash   VARCHAR(64)     NOT NULL,
    expires_at   TIMESTAMP(6)    NOT NULL,
    is_revoked   BOOLEAN         NOT NULL DEFAULT FALSE,
    issued_ip    VARCHAR(45),
    user_agent   VARCHAR(512),
    is_deleted   BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP(6)    NOT NULL,
    updated_at   TIMESTAMP(6)    NOT NULL,
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100),
    version      BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_rt_token_hash ON refresh_tokens (token_hash);
CREATE INDEX IF NOT EXISTS idx_rt_user_id    ON refresh_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_rt_expires    ON refresh_tokens (expires_at);

-- ─── SYSTEM DEFINITIONS ───────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS system_definitions (
    id            UUID            NOT NULL DEFAULT gen_random_uuid(),
    name          VARCHAR(100)    NOT NULL,
    system_type   VARCHAR(50)     NOT NULL,
    description   TEXT            NOT NULL,
    config_schema TEXT,
    is_active     BOOLEAN         NOT NULL DEFAULT TRUE,
    is_deleted    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP(6)    NOT NULL,
    updated_at    TIMESTAMP(6)    NOT NULL,
    created_by    VARCHAR(100),
    updated_by    VARCHAR(100),
    version       BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_system_definitions PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_system_def_type       ON system_definitions (system_type);
CREATE INDEX IF NOT EXISTS idx_system_def_is_deleted ON system_definitions (is_deleted);

-- ─── USER SYSTEM CONFIGS ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS user_system_configs (
    id                    UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id               UUID            NOT NULL,
    config_name           VARCHAR(150)    NOT NULL,
    app_type              VARCHAR(50)     NOT NULL,
    app_scale             VARCHAR(20)     NOT NULL,
    selected_systems_json TEXT           NOT NULL,
    generated_output_json TEXT,
    is_generated          BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted            BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP(6)    NOT NULL,
    updated_at            TIMESTAMP(6)    NOT NULL,
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),
    version               BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_user_system_configs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_usc_user_id    ON user_system_configs (user_id);
CREATE INDEX IF NOT EXISTS idx_usc_app_type   ON user_system_configs (app_type);
CREATE INDEX IF NOT EXISTS idx_usc_is_deleted ON user_system_configs (is_deleted);

-- ─── TEMPLATES ────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS templates (
    id          UUID            NOT NULL DEFAULT gen_random_uuid(),
    name        VARCHAR(150)    NOT NULL,
    description TEXT            NOT NULL,
    app_type    VARCHAR(50)     NOT NULL,
    system_type VARCHAR(50)     NOT NULL,
    app_scale   VARCHAR(20),
    config_json TEXT           NOT NULL,
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    sort_order  INT             NOT NULL DEFAULT 100,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP(6)    NOT NULL,
    updated_at  TIMESTAMP(6)    NOT NULL,
    created_by  VARCHAR(100),
    updated_by  VARCHAR(100),
    version     BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_templates PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_tmpl_app_type    ON templates (app_type);
CREATE INDEX IF NOT EXISTS idx_tmpl_system_type ON templates (system_type);
CREATE INDEX IF NOT EXISTS idx_tmpl_scale       ON templates (app_scale);

-- ─── NOTIFICATION EVENTS ──────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notification_events (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID,
    recipient           VARCHAR(512)    NOT NULL,
    channel             VARCHAR(30)     NOT NULL,
    subject             VARCHAR(255),
    body                TEXT            NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(255),
    failure_reason      TEXT,
    retry_count         INT             NOT NULL DEFAULT 0,
    delivered_at        TIMESTAMP(6),
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP(6)    NOT NULL,
    updated_at          TIMESTAMP(6)    NOT NULL,
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_notification_events PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_notif_recipient ON notification_events (recipient);
CREATE INDEX IF NOT EXISTS idx_notif_status    ON notification_events (status);
CREATE INDEX IF NOT EXISTS idx_notif_user_id   ON notification_events (user_id);
CREATE INDEX IF NOT EXISTS idx_notif_created   ON notification_events (created_at);

