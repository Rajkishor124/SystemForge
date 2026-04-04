-- ============================================================
-- SystemForge — V5 Chat Conversations & Messages
-- ============================================================

CREATE TABLE IF NOT EXISTS conversations (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    user_id           UUID            NOT NULL,
    title             VARCHAR(255)    NOT NULL DEFAULT 'New Conversation',
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_conversations PRIMARY KEY (id),
    CONSTRAINT fk_conversations_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_conv_user_id ON conversations (user_id);
CREATE INDEX IF NOT EXISTS idx_conv_created_at ON conversations (created_at DESC);

CREATE TABLE IF NOT EXISTS chat_messages (
    id                UUID            NOT NULL DEFAULT gen_random_uuid(),
    conversation_id   UUID            NOT NULL,
    role              VARCHAR(20)     NOT NULL,  -- 'user' or 'assistant'
    content           TEXT            NOT NULL,
    source            VARCHAR(20),               -- 'AI' or 'RULE_ENGINE'
    is_deleted        BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP(6)    NOT NULL DEFAULT now(),
    created_by        VARCHAR(100),
    updated_by        VARCHAR(100),
    version           BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT pk_chat_messages PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_conv FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_chatmsg_conv_id ON chat_messages (conversation_id);
CREATE INDEX IF NOT EXISTS idx_chatmsg_created_at ON chat_messages (created_at ASC);
