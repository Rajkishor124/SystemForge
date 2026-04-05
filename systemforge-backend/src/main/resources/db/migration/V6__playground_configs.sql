-- ============================================================
-- SystemForge — V6 Playground Configs
-- ============================================================

CREATE TABLE IF NOT EXISTS playground_configs (
    id                  UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID            NOT NULL,
    service_type        VARCHAR(30)     NOT NULL,
    variant             VARCHAR(30)     NOT NULL,
    features_json       TEXT,
    generated_output_json TEXT,
    is_deleted          BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP(6)    NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP(6)    NOT NULL DEFAULT now(),
    created_by          VARCHAR(100),
    updated_by          VARCHAR(100),
    version             BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_playground_configs PRIMARY KEY (id),
    CONSTRAINT fk_playground_configs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pg_configs_user_id ON playground_configs (user_id);
CREATE INDEX IF NOT EXISTS idx_pg_configs_created_at ON playground_configs (created_at DESC);
