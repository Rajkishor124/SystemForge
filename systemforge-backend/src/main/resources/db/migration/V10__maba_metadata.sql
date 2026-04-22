-- =============================================================================
-- V10: MABA Metadata — Stores per-agent execution payloads on generation jobs
-- =============================================================================
-- Adds a JSONB column to `generation_jobs` to persist MABA pipeline metadata:
--   - Per-agent token usage (prompt + completion)
--   - Per-agent timing (durationMs)
--   - Per-agent status (COMPLETED, FAILED, DEGRADED)
--   - Pipeline trace ID and total duration
--
-- This enables:
--   1. Cost auditing (total tokens per job across all agents)
--   2. Performance debugging (which agent was slowest?)
--   3. Failure forensics (which agent failed and why?)
--   4. Admin dashboard metrics
-- =============================================================================

ALTER TABLE generation_jobs
    ADD COLUMN maba_metadata JSONB;

-- Index for querying by pipeline status or traceId inside the JSONB
CREATE INDEX idx_gj_maba_trace_id
    ON generation_jobs ((maba_metadata ->> 'traceId'))
    WHERE maba_metadata IS NOT NULL;

COMMENT ON COLUMN generation_jobs.maba_metadata IS 'MABA pipeline execution metadata: agents, tokens, timing, traceId';
