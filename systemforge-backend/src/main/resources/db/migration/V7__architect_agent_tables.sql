-- =====================================================================
-- V7: AI Architect Agent — Session, Message, Step, and Tool tables
-- =====================================================================

-- Architect design sessions
CREATE TABLE architect_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL,
    title           VARCHAR(200) NOT NULL DEFAULT 'New Design Session',
    intent          VARCHAR(50),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_architect_sessions_user ON architect_sessions(user_id, is_deleted, created_at DESC);

-- Messages within a session
CREATE TABLE architect_messages (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id          UUID NOT NULL REFERENCES architect_sessions(id) ON DELETE CASCADE,
    role                VARCHAR(20) NOT NULL,
    content             TEXT NOT NULL,
    source              VARCHAR(30),
    intent              VARCHAR(50),
    processing_time_ms  BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_architect_messages_session ON architect_messages(session_id, created_at);

-- Reasoning steps for each assistant message
CREATE TABLE architect_steps (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES architect_messages(id) ON DELETE CASCADE,
    step_name       VARCHAR(100) NOT NULL,
    step_order      INT NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    output          TEXT,
    duration_ms     BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_architect_steps_message ON architect_steps(message_id, step_order);

-- Tool invocations for each assistant message
CREATE TABLE architect_tool_invocations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    message_id      UUID NOT NULL REFERENCES architect_messages(id) ON DELETE CASCADE,
    tool_name       VARCHAR(100) NOT NULL,
    input_json      TEXT,
    output_json     TEXT,
    status          VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    duration_ms     BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_architect_tool_invocations_message ON architect_tool_invocations(message_id);
