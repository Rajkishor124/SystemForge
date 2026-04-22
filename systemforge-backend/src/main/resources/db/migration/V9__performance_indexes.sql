-- ============================================================
-- SystemForge — V9: Performance Indexes
-- ============================================================
-- Composite and partial indexes for hot query paths.
-- Optimizes the most frequent queries in the application:
--   - Job polling by user + status
--   - Config listing by user (paginated, sorted)
--   - Active config lookup
--   - Refresh token validation
-- ============================================================

-- Job polling: GET /jobs/{id} queries by user_id + status
-- This is the most frequent query during generation (polled every 3s)
CREATE INDEX IF NOT EXISTS idx_gj_user_status
    ON generation_jobs (user_id, status);

-- Config listing: paginated by user, sorted by created_at DESC
-- Covers the main dashboard query
CREATE INDEX IF NOT EXISTS idx_usc_user_created
    ON user_system_configs (user_id, created_at DESC)
    WHERE is_deleted = FALSE;

-- Active generated configs: quick lookup for completed architectures
CREATE INDEX IF NOT EXISTS idx_usc_user_generated
    ON user_system_configs (user_id)
    WHERE is_deleted = FALSE AND is_generated = TRUE;

-- Refresh token lookup: validate active tokens for a user
-- Critical for the cookie-based auth refresh flow
CREATE INDEX IF NOT EXISTS idx_rt_user_active
    ON refresh_tokens (user_id, is_revoked)
    WHERE is_revoked = FALSE AND is_deleted = FALSE;

-- Job history: user's completed/failed jobs ordered by completion
CREATE INDEX IF NOT EXISTS idx_gj_user_completed
    ON generation_jobs (user_id, completed_at DESC)
    WHERE status IN ('COMPLETED', 'FAILED');
