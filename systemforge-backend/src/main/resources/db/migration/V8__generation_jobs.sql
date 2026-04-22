-- ============================================================
-- SystemForge — V8: Async Generation Jobs
-- ============================================================
-- Tracks background AI generation tasks.
-- Decouples long-running LLM calls from the HTTP request thread.
-- ============================================================

CREATE TABLE IF NOT EXISTS generation_jobs (
    id              UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL,
    config_id       UUID,
    session_id      UUID,
    job_type        VARCHAR(30)     NOT NULL DEFAULT 'SYSTEM_GENERATION',
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    result_json     TEXT,
    error_message   TEXT,
    started_at      TIMESTAMP(6),
    completed_at    TIMESTAMP(6),
    is_deleted      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP(6)    NOT NULL,
    updated_at      TIMESTAMP(6)    NOT NULL,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    version         BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_generation_jobs PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_gj_user_id   ON generation_jobs (user_id);
CREATE INDEX IF NOT EXISTS idx_gj_status    ON generation_jobs (status);
CREATE INDEX IF NOT EXISTS idx_gj_config_id ON generation_jobs (config_id);
CREATE INDEX IF NOT EXISTS idx_gj_created   ON generation_jobs (created_at);
