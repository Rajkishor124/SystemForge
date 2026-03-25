-- ============================================================
-- SystemForge — V3 In-App Notifications Schema
-- ============================================================

CREATE TABLE IF NOT EXISTS in_app_notifications (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID            NOT NULL,
    title             VARCHAR(150)    NOT NULL,
    message           TEXT            NOT NULL,
    type              VARCHAR(50)     NOT NULL,
    link              VARCHAR(255),
    is_read           BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_in_app_notifications PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_in_app_notif_user_id ON in_app_notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_in_app_notif_is_read ON in_app_notifications (is_read);
CREATE INDEX IF NOT EXISTS idx_in_app_notif_created_at ON in_app_notifications (created_at DESC);
