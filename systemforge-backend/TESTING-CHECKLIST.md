# SystemForge — Real User Testing Checklist

Use this checklist before each production release to validate all critical user flows.

---

## 1. SSE Reconnect Scenarios

### 1.1 Network Interruption Recovery
- [ ] Start a generation job
- [ ] While PROCESSING, disconnect Wi-Fi for 5 seconds
- [ ] Re-enable Wi-Fi
- [ ] **Expected**: SSE reconnects automatically (check console for `[SSE] Connection error (attempt X/5). Reconnecting in Xms...`)
- [ ] **Expected**: Progress bar resumes from where it left off (replay events)
- [ ] **Expected**: No duplicate step cards in the UI

### 1.2 Server Restart During Job
- [ ] Start a generation job
- [ ] Restart the backend server while job is PROCESSING
- [ ] **Expected**: SSE shows connection error, then reconnects
- [ ] **Expected**: After stuck job sweeper runs (~5 min), job transitions to FAILED
- [ ] **Expected**: Frontend shows failure message, not infinite spinner

### 1.3 SSE Max Retries Exhaustion
- [ ] Start a generation job
- [ ] Block backend port (e.g., firewall rule) for 2+ minutes
- [ ] **Expected**: After 5 retry attempts, frontend falls back to polling
- [ ] **Expected**: Console shows `[SSE] Max retries (5) reached — falling back to polling`
- [ ] **Expected**: Progress continues via polling (slower updates but functional)

### 1.4 Heartbeat Verification
- [ ] Open browser DevTools → Network → filter "EventStream"
- [ ] Subscribe to a job's SSE stream
- [ ] Wait 30 seconds
- [ ] **Expected**: At least 2 HEARTBEAT events visible in the stream
- [ ] **Expected**: No connection timeout during idle periods

---

## 2. Token Expiry Handling

### 2.1 Expired JWT During Active SSE
- [ ] Set short JWT expiry (e.g., 60 seconds) in test env
- [ ] Start a generation job (long-running)
- [ ] Wait for JWT to expire during processing
- [ ] **Expected**: On next SSE reconnect, server sends TOKEN_EXPIRED event
- [ ] **Expected**: Frontend status changes to `token_expired`
- [ ] **Expected**: UI shows "Please re-authenticate" (not a generic error)
- [ ] **Expected**: No infinite reconnection loop

### 2.2 Expired JWT on Initial SSE Connect
- [ ] Let JWT expire, then navigate to a page with active generation
- [ ] **Expected**: SSE connection attempt returns TOKEN_EXPIRED event
- [ ] **Expected**: Frontend does NOT fall back to polling with an expired token
- [ ] **Expected**: User is prompted to refresh/re-login

### 2.3 Token Refresh Flow
- [ ] Start a generation job
- [ ] Let access token expire
- [ ] Refresh the token via the refresh token endpoint
- [ ] **Expected**: SSE reconnects with new token and resumes updates
- [ ] **Expected**: No loss of job progress state

---

## 3. Network Interruptions

### 3.1 Slow Network (3G Simulation)
- [ ] Open DevTools → Network → Throttle to "Slow 3G"
- [ ] Start a generation job
- [ ] **Expected**: Job submission succeeds (may take longer)
- [ ] **Expected**: SSE events arrive (delayed but complete)
- [ ] **Expected**: No timeout errors during normal 3G latency

### 3.2 Complete Offline
- [ ] Start a generation job
- [ ] Go fully offline (DevTools → Network → Offline)
- [ ] Wait 10 seconds
- [ ] Go back online
- [ ] **Expected**: SSE reconnects with exponential backoff
- [ ] **Expected**: Missed events are replayed on reconnect

### 3.3 Intermittent Connection
- [ ] Start a generation job
- [ ] Toggle network on/off every 5 seconds during processing
- [ ] **Expected**: System recovers each time
- [ ] **Expected**: Final result is correct (no corrupted state)
- [ ] **Expected**: No duplicate completion events

---

## 4. Multi-Tab Behavior

### 4.1 Same Job in Two Tabs
- [ ] Start a generation job in Tab A
- [ ] Open the same job page in Tab B
- [ ] **Expected**: Both tabs receive SSE events independently
- [ ] **Expected**: Both tabs show progress in sync
- [ ] **Expected**: Completion is reflected in both tabs

### 4.2 Tab Close During Processing
- [ ] Start a generation job
- [ ] Close the browser tab while job is PROCESSING
- [ ] Re-open the page in a new tab
- [ ] **Expected**: SSE reconnects and replays cached events
- [ ] **Expected**: If job completed while tab was closed, shows COMPLETED status
- [ ] **Expected**: No orphaned SSE connections (check `/actuator/health`)

### 4.3 Multiple Jobs Across Tabs
- [ ] Open 3 tabs, each with a different generation job
- [ ] **Expected**: Each tab tracks its own job independently
- [ ] **Expected**: No cross-contamination of job progress between tabs
- [ ] **Expected**: SSE connections count = 3 in metrics

---

## 5. Rate Limiting

### 5.1 Rapid Job Submission
- [ ] Submit 5 generation jobs in quick succession
- [ ] **Expected**: First 3 succeed (rate limit: 3 per 60 min in prod)
- [ ] **Expected**: 4th+ returns `HTTP 429 Too Many Requests`
- [ ] **Expected**: Response includes `Retry-After` header
- [ ] **Expected**: Frontend shows appropriate error message

### 5.2 Rate Limit Recovery
- [ ] Exceed rate limit (trigger 429)
- [ ] Wait for the `Retry-After` period to elapse
- [ ] Submit another job
- [ ] **Expected**: Request succeeds normally

---

## 6. Backpressure & System Overload

### 6.1 Executor Overload
- [ ] Submit more jobs than thread pool capacity
- [ ] **Expected**: `HTTP 503 Service Unavailable` with message "System is busy"
- [ ] **Expected**: No server crash, no memory spike
- [ ] **Expected**: Previously submitted jobs continue processing

### 6.2 Circuit Breaker Activation
- [ ] Configure invalid AI API key
- [ ] Submit a generation job
- [ ] **Expected**: Job retries 3 times, then fails
- [ ] **Expected**: After several failures, circuit breaker opens
- [ ] **Expected**: Subsequent jobs fail fast with "AI service temporarily unavailable"
- [ ] **Expected**: After 60s cooldown, system auto-recovers

---

## 7. Data Integrity

### 7.1 Job State Consistency
- [ ] After each test scenario, verify via API:
  ```bash
  curl -H "Authorization: Bearer $TOKEN" \
    http://localhost:8080/api/v1/systems/jobs/{jobId}
  ```
- [ ] **Expected**: Status is one of: PENDING, PROCESSING, COMPLETED, FAILED
- [ ] **Expected**: No jobs stuck in PROCESSING indefinitely (sweeper catches them)
- [ ] **Expected**: completedAt/failedAt timestamps are set for terminal jobs

### 7.2 Generated Output Integrity
- [ ] Complete a generation job successfully
- [ ] Fetch the full result via polling API
- [ ] **Expected**: `generatedOutputJson` contains valid JSON
- [ ] **Expected**: Architecture document is complete and parseable

---

## Post-Test Verification

After all tests, verify system health:

```bash
# Health check
curl -s http://localhost:8080/actuator/health | jq '.status'
# Expected: "UP"

# No leaked SSE connections
curl -s http://localhost:8080/actuator/health | jq '.components.sseRegistry.details.activeConnections'
# Expected: 0 (if no active jobs)

# Metrics sanity check
curl -s http://localhost:8080/actuator/metrics/generation.jobs.created | jq '.measurements[0].value'
curl -s http://localhost:8080/actuator/metrics/generation.jobs.completed | jq '.measurements[0].value'
curl -s http://localhost:8080/actuator/metrics/generation.jobs.failed | jq '.measurements[0].value'
# Expected: created = completed + failed (approximately)
```
