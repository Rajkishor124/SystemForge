# SystemForge — Production Deployment Guide

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Environment Variables](#environment-variables)
3. [Single-Instance Deployment](#single-instance-deployment)
4. [Multi-Instance Scaling](#multi-instance-scaling)
5. [SSE Behind Load Balancer](#sse-behind-load-balancer)
6. [Monitoring & Observability](#monitoring--observability)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Docker 24+ and Docker Compose v2
- PostgreSQL 16+ (or use the Docker Compose postgres service)
- Redis 7+ (required for multi-instance; optional for single-instance)
- Java 21+ (if running without Docker)
- 2GB RAM minimum per backend instance

---

## Environment Variables

Copy `.env.example` to `.env` and configure:

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `DB_HOST` | ✅ | `localhost` | PostgreSQL host |
| `DB_PORT` | ❌ | `5432` | PostgreSQL port |
| `DB_NAME` | ❌ | `systemforge_db` | Database name |
| `DB_USERNAME` | ✅ | `postgres` | Database user |
| `DB_PASSWORD` | ✅ | — | Database password |
| `JWT_SECRET` | ✅ | — | JWT signing secret (min 32 chars) |
| `JWT_EXPIRATION` | ❌ | `86400000` | Access token TTL (ms) — 24h |
| `JWT_REFRESH_EXPIRATION` | ❌ | `604800000` | Refresh token TTL (ms) — 7d |
| `OPENAI_API_KEY` | ✅ | — | OpenAI API key |
| `OPENAI_MODEL` | ❌ | `gpt-4o` | LLM model to use |
| `OPENAI_BASE_URL` | ❌ | `https://api.openai.com/v1` | API base URL |
| `SPRING_REDIS_HOST` | ❌ | `localhost` | Redis host |
| `SPRING_REDIS_PORT` | ❌ | `6379` | Redis port |
| `SPRING_PROFILES_ACTIVE` | ❌ | `dev` | Set to `prod` for production |
| `SYSTEMFORGE_EVENT_BUS_TYPE` | ❌ | `local` | Set to `redis` for multi-instance |

### Security Notes

- **JWT_SECRET**: Generate with `openssl rand -base64 48`
- **DB_PASSWORD**: Use a strong random password
- **OPENAI_API_KEY**: Never commit to version control
- All secrets should be managed via a secrets manager (AWS Secrets Manager, Vault, etc.)

---

## Single-Instance Deployment

### Option A: Docker Compose (Recommended)

```bash
# 1. Configure environment
cp .env.example .env
nano .env  # Fill in DB_PASSWORD, JWT_SECRET, OPENAI_API_KEY

# 2. Start all services
docker compose up -d

# 3. Verify health
curl http://localhost:8080/actuator/health

# 4. Check logs
docker compose logs -f backend
```

### Option B: JAR Deployment

```bash
# 1. Build the JAR
./mvnw package -DskipTests

# 2. Run with production profile
SPRING_PROFILES_ACTIVE=prod \
DB_HOST=your-db-host \
DB_PASSWORD=your-password \
JWT_SECRET=your-secret \
OPENAI_API_KEY=your-key \
java -XX:+UseG1GC -XX:MaxRAMPercentage=75.0 \
  -jar target/*.jar
```

---

## Multi-Instance Scaling

### Architecture

```
                    ┌─────────────┐
                    │ Load        │
                    │ Balancer    │
                    │ (Nginx/ALB) │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────┴─────┐ ┌───┴───┐ ┌─────┴─────┐
        │ Backend-1 │ │ BE-2  │ │ Backend-3 │
        └─────┬─────┘ └───┬───┘ └─────┬─────┘
              │            │            │
              └────────────┼────────────┘
                           │
              ┌────────────┼────────────┐
              │                         │
        ┌─────┴─────┐           ┌──────┴──────┐
        │ PostgreSQL │           │    Redis    │
        │  (shared)  │           │  (Pub/Sub)  │
        └────────────┘           └─────────────┘
```

### Steps

1. **Enable Redis EventBus**:
   ```bash
   # In .env
   SYSTEMFORGE_EVENT_BUS_TYPE=redis
   ```

2. **Scale backend instances**:
   ```bash
   docker compose up -d --scale backend=3
   ```

3. **Configure load balancer** (see SSE section below)

### How It Works

- All instances share PostgreSQL (source of truth for job state)
- Redis Pub/Sub broadcasts SSE events across all instances
- When Backend-1 processes a job, it publishes events to Redis
- All instances receive the event and forward to their local SSE connections
- No duplicate processing: atomic `UPDATE ... WHERE status = 'PENDING'` query

### Sticky Sessions

**NOT required.** The system works without sticky sessions because:
- Job state is in PostgreSQL (shared)
- SSE events flow through Redis Pub/Sub (broadcast to all)
- Any instance can serve any SSE connection

However, sticky sessions **improve performance** by reducing Redis traffic
(events go directly to the instance with the SSE connection).

---

## SSE Behind Load Balancer

Server-Sent Events require special load balancer configuration:

### Nginx Configuration

```nginx
upstream systemforge_backend {
    # Use ip_hash for sticky sessions (optional but recommended)
    ip_hash;

    server backend-1:8080;
    server backend-2:8080;
    server backend-3:8080;
}

server {
    listen 443 ssl;
    server_name api.systemforge.dev;

    # SSE-specific settings
    location ~ /api/v1/systems/jobs/.*/stream {
        proxy_pass http://systemforge_backend;

        # CRITICAL: Disable buffering for SSE
        proxy_buffering off;
        proxy_cache off;

        # SSE-specific headers
        proxy_set_header Connection '';
        proxy_http_version 1.1;
        chunked_transfer_encoding off;

        # Timeout: match SSE timeout (5 min for prod)
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;

        # Standard proxy headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Regular API requests
    location /api/ {
        proxy_pass http://systemforge_backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### AWS ALB Configuration

- **Target Group**: HTTP, port 8080
- **Health Check**: `/actuator/health`
- **Stickiness**: Enable (optional), duration 5 min
- **Idle Timeout**: Set to 300 seconds (5 min) for SSE
- **Deregistration Delay**: 120 seconds (allow in-flight SSE to complete)

### Key Points

| Setting | Value | Why |
|---------|-------|-----|
| `proxy_buffering` | `off` | SSE events must be pushed immediately |
| `proxy_read_timeout` | `300s` | Must match or exceed SSE timeout |
| `chunked_transfer_encoding` | `off` | Prevents buffering of chunked responses |
| `Connection` header | `''` (empty) | Prevents keep-alive interference |

---

## Monitoring & Observability

### Health Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /actuator/health` | Overall system health |
| `GET /actuator/metrics` | Micrometer metrics index |
| `GET /actuator/metrics/generation.jobs.active` | Active jobs gauge |
| `GET /actuator/metrics/generation.sse.connections` | SSE connections gauge |
| `GET /actuator/metrics/generation.jobs.failed` | Failed jobs counter |

### Key Metrics to Monitor

| Metric | Alert Threshold | Description |
|--------|----------------|-------------|
| `generation.jobs.active` | > 40 | Jobs being processed |
| `generation.sse.connections` | > 300 | Open SSE connections |
| `generation.jobs.failed` (rate) | > 20% of created | Failure rate |
| `jvm.memory.used` | > 85% max | Heap pressure |
| `hikari.connections.active` | > 80% max | DB pool pressure |

### Log Aggregation

Production logs are JSON-structured (via `logback-spring.xml`):

```json
{
  "timestamp": "2026-04-24T14:30:00.000+05:30",
  "level": "INFO",
  "logger": "c.s.b.system.worker.GenerationWorker",
  "message": "event=JOB_COMPLETED jobId=abc-123 userId=user-456 status=COMPLETED durationMs=45230",
  "mdc": {
    "correlationId": "req-789",
    "jobId": "abc-123",
    "userId": "user-456"
  }
}
```

Filter by:
- `event=ALERT_CRITICAL` — immediate action required
- `event=JOB_FAILED` — investigate AI pipeline failures
- `event=SSE_TOKEN_EXPIRED` — track auth issues during SSE

### Prometheus Scrape Config

```yaml
scrape_configs:
  - job_name: 'systemforge'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend-1:8080', 'backend-2:8080', 'backend-3:8080']
```

---

## Troubleshooting

### Backend won't start

```bash
# Check logs
docker compose logs backend

# Common causes:
# 1. DB not ready → ensure postgres is healthy first
# 2. Missing env vars → check .env file
# 3. Port conflict → check `netstat -tulpn | grep 8080`
```

### SSE not receiving events

```bash
# 1. Check SSE health
curl http://localhost:8080/actuator/health | jq '.components.sseRegistry'

# 2. Check EventBus type
# If multi-instance, ensure SYSTEMFORGE_EVENT_BUS_TYPE=redis

# 3. Test SSE directly
curl -N http://localhost:8080/api/v1/systems/jobs/{jobId}/stream \
  -H "Authorization: Bearer $TOKEN"

# 4. Check Redis connectivity (if using Redis EventBus)
docker exec systemforge-redis redis-cli ping
```

### Jobs stuck in PROCESSING

```bash
# Check active jobs
curl http://localhost:8080/actuator/metrics/generation.jobs.active

# The stuck job sweeper runs every 5 minutes.
# Jobs stuck > 10 min (dev) or > 15 min (prod) are auto-failed.
# Check logs:
docker compose logs backend | grep "STUCK_JOBS_DETECTED"
```

### High memory usage

```bash
# Check JVM metrics
curl http://localhost:8080/actuator/metrics/jvm.memory.used

# Check SSE cache size (max 50 events per job)
curl http://localhost:8080/actuator/health | jq '.components.sseRegistry.details.cachedReplayEvents'

# If SSE connections are leaking:
curl http://localhost:8080/actuator/metrics/generation.sse.connections
```
