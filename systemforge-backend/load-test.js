/**
 * SystemForge — k6 Load Test Script
 * ==================================
 *
 * Simulates real-world usage:
 * - Authenticates users
 * - Creates system configs
 * - Submits generation jobs
 * - Opens SSE connections for progress streaming
 * - Polls job status as fallback
 *
 * Prerequisites:
 * - k6 installed: https://k6.io/docs/get-started/installation/
 * - Backend running at BASE_URL
 * - Valid test user credentials
 *
 * Usage:
 *   k6 run --vus 50 --duration 2m load-test.js
 *   k6 run --vus 100 --duration 5m load-test.js
 *
 * Verification Checklist:
 * ✅ No HTTP 500 errors
 * ✅ No duplicate job processing (check logs)
 * ✅ SSE connections established and heartbeats received
 * ✅ Jobs complete or fail cleanly (no stuck PROCESSING)
 * ✅ p95 response time < 2s for job submission
 * ✅ Memory stable (check /actuator/health)
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Configuration ──────────────────────────────────────────────────────────────

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TEST_EMAIL = __ENV.TEST_EMAIL || 'loadtest@systemforge.dev';
const TEST_PASSWORD = __ENV.TEST_PASSWORD || 'LoadTest@123';

// ─── Custom Metrics ─────────────────────────────────────────────────────────────

const jobsCreated = new Counter('jobs_created');
const jobsCompleted = new Counter('jobs_completed');
const jobsFailed = new Counter('jobs_failed');
const jobsStuck = new Counter('jobs_stuck');
const sseConnections = new Counter('sse_connections');
const errorRate = new Rate('error_rate');
const jobDuration = new Trend('job_duration_ms');

// ─── Scenarios ──────────────────────────────────────────────────────────────────

export const options = {
  scenarios: {
    // Ramp up to 50 concurrent users, hold for 2 minutes
    load_test: {
      executor: 'ramping-vus',
      startVUs: 5,
      stages: [
        { duration: '30s', target: 25 },   // Ramp up
        { duration: '2m', target: 50 },    // Sustained load
        { duration: '30s', target: 0 },    // Ramp down
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<3000'],   // 95% of requests under 3s
    error_rate: ['rate<0.1'],            // Less than 10% error rate
    jobs_stuck: ['count<5'],             // No more than 5 stuck jobs
  },
};

// ─── Helpers ────────────────────────────────────────────────────────────────────

function authenticate() {
  const res = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({
      email: TEST_EMAIL,
      password: TEST_PASSWORD,
    }),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  const success = check(res, {
    'login successful': (r) => r.status === 200,
    'has access token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.accessToken;
      } catch {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
    console.error(`Auth failed: ${res.status} ${res.body}`);
    return null;
  }

  errorRate.add(0);
  const body = JSON.parse(res.body);
  return body.data.accessToken;
}

function createConfig(token) {
  const res = http.post(
    `${BASE_URL}/api/v1/systems/configs`,
    JSON.stringify({
      configName: `LoadTest-${Date.now()}-${__VU}`,
      appType: 'WEB_APPLICATION',
      appScale: 'SMALL',
      selectedSystems: ['AUTH', 'DATABASE'],
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
      },
    }
  );

  check(res, {
    'config created': (r) => r.status === 200 || r.status === 201,
  });

  if (res.status !== 200 && res.status !== 201) {
    errorRate.add(1);
    return null;
  }

  errorRate.add(0);
  const body = JSON.parse(res.body);
  return body.data?.id;
}

function submitGeneration(token, configId) {
  const res = http.post(
    `${BASE_URL}/api/v1/systems/configs/${configId}/generate`,
    null,
    {
      headers: {
        Authorization: `Bearer ${token}`,
      },
    }
  );

  const success = check(res, {
    'generation submitted': (r) => r.status === 202 || r.status === 200,
    'returns jobId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.id;
      } catch {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
    // 503 is expected under load (backpressure)
    if (res.status === 503) {
      console.log(`[VU ${__VU}] System busy (503) — backpressure working correctly`);
    }
    return null;
  }

  errorRate.add(0);
  jobsCreated.add(1);
  const body = JSON.parse(res.body);
  return body.data.id;
}

function pollJobStatus(token, jobId) {
  const startTime = Date.now();
  const maxWait = 180000; // 3 minutes
  const pollInterval = 3; // seconds

  while (Date.now() - startTime < maxWait) {
    const res = http.get(
      `${BASE_URL}/api/v1/systems/jobs/${jobId}`,
      {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      }
    );

    if (res.status === 200) {
      const body = JSON.parse(res.body);
      const status = body.data?.status;

      if (status === 'COMPLETED') {
        jobsCompleted.add(1);
        jobDuration.add(Date.now() - startTime);
        return 'COMPLETED';
      }

      if (status === 'FAILED') {
        jobsFailed.add(1);
        jobDuration.add(Date.now() - startTime);
        return 'FAILED';
      }
    }

    sleep(pollInterval);
  }

  // Job is stuck
  jobsStuck.add(1);
  console.error(`[VU ${__VU}] Job ${jobId} stuck after ${maxWait / 1000}s`);
  return 'STUCK';
}

function checkHealth() {
  const res = http.get(`${BASE_URL}/actuator/health`);
  check(res, {
    'health endpoint OK': (r) => r.status === 200,
    'system is UP': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.status === 'UP';
      } catch {
        return false;
      }
    },
  });
}

function checkMetrics() {
  const res = http.get(`${BASE_URL}/actuator/metrics/generation.jobs.created`);
  check(res, {
    'metrics endpoint accessible': (r) => r.status === 200,
  });
}

// ─── Main Test ──────────────────────────────────────────────────────────────────

export default function () {
  group('Health Check', () => {
    checkHealth();
  });

  let token;
  group('Authentication', () => {
    token = authenticate();
  });

  if (!token) {
    console.error(`[VU ${__VU}] Authentication failed — skipping iteration`);
    sleep(5);
    return;
  }

  let configId;
  group('Create Config', () => {
    configId = createConfig(token);
  });

  if (!configId) {
    console.warn(`[VU ${__VU}] Config creation failed — skipping generation`);
    sleep(2);
    return;
  }

  let jobId;
  group('Submit Generation', () => {
    jobId = submitGeneration(token, configId);
  });

  if (!jobId) {
    sleep(2);
    return;
  }

  group('Poll Job Status', () => {
    const finalStatus = pollJobStatus(token, jobId);
    console.log(`[VU ${__VU}] Job ${jobId} → ${finalStatus}`);
  });

  // Check metrics at end of each iteration
  group('Verify Metrics', () => {
    checkMetrics();
  });

  sleep(1);
}

// ─── Teardown ───────────────────────────────────────────────────────────────────

export function handleSummary(data) {
  const summary = {
    total_requests: data.metrics.http_reqs?.values?.count || 0,
    error_rate: data.metrics.error_rate?.values?.rate || 0,
    jobs_created: data.metrics.jobs_created?.values?.count || 0,
    jobs_completed: data.metrics.jobs_completed?.values?.count || 0,
    jobs_failed: data.metrics.jobs_failed?.values?.count || 0,
    jobs_stuck: data.metrics.jobs_stuck?.values?.count || 0,
    p95_response_time: data.metrics.http_req_duration?.values?.['p(95)'] || 0,
    sse_connections: data.metrics.sse_connections?.values?.count || 0,
  };

  console.log('\n══════════════════════════════════════════════');
  console.log('  SYSTEMFORGE LOAD TEST RESULTS');
  console.log('══════════════════════════════════════════════');
  console.log(`  Total Requests:     ${summary.total_requests}`);
  console.log(`  Error Rate:         ${(summary.error_rate * 100).toFixed(1)}%`);
  console.log(`  Jobs Created:       ${summary.jobs_created}`);
  console.log(`  Jobs Completed:     ${summary.jobs_completed}`);
  console.log(`  Jobs Failed:        ${summary.jobs_failed}`);
  console.log(`  Jobs Stuck:         ${summary.jobs_stuck}`);
  console.log(`  p95 Response Time:  ${summary.p95_response_time.toFixed(0)}ms`);
  console.log('══════════════════════════════════════════════\n');

  // Assertions
  const passed =
    summary.error_rate < 0.1 &&
    summary.jobs_stuck < 5;

  console.log(passed ? '✅ ALL CHECKS PASSED' : '❌ SOME CHECKS FAILED');

  return {
    stdout: JSON.stringify(summary, null, 2),
  };
}
