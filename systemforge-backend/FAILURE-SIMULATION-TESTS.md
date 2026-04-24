# SystemForge — Failure Simulation Test Guide

This document describes how to manually and automatically test failure scenarios
to verify the resilience of the async generation pipeline.

---

## Scenario 1: Server Crash During PROCESSING

**Goal:** Verify that jobs stuck in PROCESSING are recovered on restart.

### Steps
1. Submit a generation job and verify status transitions to `PROCESSING`
2. Kill the server process (`Ctrl+C` or `kill -9`)
3. Restart the server
4. Wait 5 minutes for the `cleanStuckJobs()` scheduler to run

### Expected Behavior
- The stuck job is detected and marked `FAILED` with message:
  `"Job exceeded maximum execution time limit (10 minutes)."`
- SSE listeners (if reconnected) receive a `FAILED` event
- The job appears as `FAILED` in `GET /api/v1/systems/jobs/{jobId}`

### Verification
```bash
# Check job status via API after restart
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/v1/systems/jobs/{jobId}

# Check logs for stuck job detection
grep "STUCK_JOBS_DETECTED" logs/systemforge.log
```

---

## Scenario 2: AI Pipeline Failure (Retry Logic)

**Goal:** Verify retry mechanism fires correctly.

### Steps
1. Temporarily make the LLM endpoint return errors (e.g., invalid API key)
2. Submit a generation job
3. Observe retry behavior in logs

### Expected Behavior
- Worker retries up to `maxRetries` (default: 3) times
- Each retry logs: `event=JOB_RETRY jobId=... attempt=X/3`
- After exhausting retries: `event=JOB_FAILED jobId=... status=FAILED`
- Micrometer counter `generation.jobs.retries` increments on each retry
- Micrometer counter `generation.jobs.failed` increments once after max retries

### Verification
```bash
# Check retry metrics
curl http://localhost:8080/actuator/metrics/generation.jobs.retries
curl http://localhost:8080/actuator/metrics/generation.jobs.failed

# Check structured logs
grep "JOB_RETRY" logs/systemforge.log
grep "JOB_FAILED" logs/systemforge.log
```

---

## Scenario 3: SSE Disconnect Mid-Stream

**Goal:** Verify reconnection replays event history.

### Steps
1. Submit a generation job
2. Open SSE stream in browser DevTools (Network tab, filter by EventStream)
3. Observe events being received (INIT, PROGRESS events)
4. Disconnect network briefly (toggle Wi-Fi or use Chrome DevTools throttling)
5. Re-enable network — EventSource will automatically reconnect

### Expected Behavior
- Frontend `useGenerationStream` detects the disconnect
- Exponential backoff kicks in (1s → 2s → 4s → 8s → 16s)
- On reconnection, the `SseEmitterRegistry.register()` replays up to 50 cached events
- Frontend state is reconstructed from replayed events
- If max retries (5) are exceeded, frontend falls back to polling

### Verification
- Check browser console for `[SSE] Connection error (attempt X/5). Reconnecting in Xms...`
- After reconnect, verify progress bar and step list are accurate
- Check backend logs: `event=SSE_REPLAY jobId=... eventsReplayed=N`

---

## Scenario 4: Executor Overload (Backpressure)

**Goal:** Verify the system rejects new jobs cleanly when at capacity.

### Steps
1. Reduce thread pool to minimum for testing:
   ```yaml
   # application-dev.yaml
   systemforge:
     async:
       core-pool-size: 1
       max-pool-size: 2
       queue-capacity: 2
   ```
2. Submit 5+ concurrent generation jobs rapidly
3. Observe the 3rd+ jobs being rejected

### Expected Behavior
- First 4 jobs accepted (2 threads + 2 queue slots)
- 5th job receives `HTTP 503 Service Unavailable`
  with body: `{"errorCode":"SYS_004","message":"System is busy, please try again"}`
- No crash, no thread exhaustion, no memory spike

### Verification
```bash
# Check health endpoint for thread pool status
curl http://localhost:8080/actuator/health | jq '.components.aiThreadPool'

# Check active job metrics
curl http://localhost:8080/actuator/metrics/generation.jobs.active
```

---

## Scenario 5: Duplicate Job Prevention (Concurrency Safety)

**Goal:** Verify the atomic `updateStatusConditionally` prevents duplicate processing.

### Steps
1. Submit a generation job
2. Observe the atomic status transition in logs
3. (Advanced) Use a debugger to pause the worker thread after `findById`
   but before `updateStatusConditionally`, then trigger a second worker
   for the same job

### Expected Behavior
- Only one worker successfully transitions `PENDING → PROCESSING`
- The second worker logs: `event=JOB_SKIP jobId=... message=Job is not PENDING`
- No duplicate processing occurs

### Verification
```bash
# Search for skip events
grep "JOB_SKIP" logs/systemforge.log

# Verify no duplicate COMPLETED events in SSE
grep "JOB_COMPLETED" logs/systemforge.log | awk '{print $2}' | sort | uniq -c | sort -rn
```

---

## Automated Verification Checklist

Run after each deployment:

```bash
# 1. Health check
curl -s http://localhost:8080/actuator/health | jq '.status'
# Expected: "UP"

# 2. Thread pool health
curl -s http://localhost:8080/actuator/health | jq '.components.aiThreadPool'
# Expected: status "UP", reasonable utilization

# 3. SSE registry health
curl -s http://localhost:8080/actuator/health | jq '.components.sseRegistry'
# Expected: activeConnections >= 0, no warnings

# 4. Metrics baseline
curl -s http://localhost:8080/actuator/metrics/generation.jobs.created
curl -s http://localhost:8080/actuator/metrics/generation.jobs.completed
curl -s http://localhost:8080/actuator/metrics/generation.jobs.failed
curl -s http://localhost:8080/actuator/metrics/generation.jobs.active
curl -s http://localhost:8080/actuator/metrics/generation.sse.connections

# 5. k6 load test
k6 run --vus 50 --duration 2m load-test.js
```
