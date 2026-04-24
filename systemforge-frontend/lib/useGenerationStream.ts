'use client';

import { useEffect, useState, useRef, useCallback } from 'react';
import { api } from '@/lib/api';

// ─── Types ──────────────────────────────────────────────────────────────────────

export interface GenerationStep {
  name: string;
  order: number;
  totalSteps: number;
  status: 'pending' | 'running' | 'completed' | 'failed';
  output?: string;
  errorMessage?: string;
  durationMs?: number;
}

export type StreamStatus =
  | 'idle'
  | 'connecting'
  | 'connected'
  | 'streaming'
  | 'completed'
  | 'failed'
  | 'token_expired'
  | 'polling_fallback';

export interface GenerationStreamState {
  status: StreamStatus;
  steps: GenerationStep[];
  progress: number;
  error: string | null;
  jobId: string | null;
  isFallback: boolean;
  connectionAttempt: number;
}

interface SSEEvent {
  type: string;
  step?: string;
  order?: number;
  totalSteps?: number;
  output?: string;
  errorMessage?: string;
  durationMs?: number;
  progress?: number;
  jobId?: string;
  status?: string;
}

// ─── Constants ──────────────────────────────────────────────────────────────────

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const POLL_INTERVAL_MS = 3000;
const MAX_SSE_RETRIES = 5;
const INITIAL_BACKOFF_MS = 1000;
const MAX_BACKOFF_MS = 30000;
const BACKOFF_MULTIPLIER = 2;

// ─── Utility ────────────────────────────────────────────────────────────────────

/**
 * Calculates exponential backoff delay with jitter.
 * Prevents thundering herd on reconnection storms.
 */
function getBackoffDelay(attempt: number): number {
  const exponential = INITIAL_BACKOFF_MS * Math.pow(BACKOFF_MULTIPLIER, attempt);
  const capped = Math.min(exponential, MAX_BACKOFF_MS);
  // Add ±20% jitter
  const jitter = capped * (0.8 + Math.random() * 0.4);
  return Math.round(jitter);
}

// ─── Hook ───────────────────────────────────────────────────────────────────────

/**
 * Custom hook for consuming the generation SSE stream.
 *
 * Features:
 * - Automatic exponential backoff reconnection (up to MAX_SSE_RETRIES)
 * - Heartbeat-aware: resets retry counter on HEARTBEAT events
 * - Graceful fallback to polling on SSE exhaustion
 * - Handles all standardized events: INIT, PROGRESS, COMPLETED, FAILED, HEARTBEAT
 * - Cleans up resources on unmount
 *
 * Usage:
 * ```tsx
 * const { status, steps, progress, error } = useGenerationStream(jobId);
 * ```
 */
export function useGenerationStream(jobId: string | null): GenerationStreamState {
  const [state, setState] = useState<GenerationStreamState>({
    status: 'idle',
    steps: [],
    progress: 0,
    error: null,
    jobId: null,
    isFallback: false,
    connectionAttempt: 0,
  });

  const eventSourceRef = useRef<EventSource | null>(null);
  const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const reconnectTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const sseRetriesRef = useRef(0);
  const mountedRef = useRef(true);
  const lastHeartbeatRef = useRef<number>(Date.now());

  // ─── SSE Event Handlers ─────────────────────────────────────────────

  const handleEvent = useCallback((event: SSEEvent) => {
    if (!mountedRef.current) return;

    switch (event.type) {
      case 'connected':
        setState(prev => ({
          ...prev,
          status: 'connected',
          connectionAttempt: sseRetriesRef.current,
        }));
        break;

      case 'step_started':
        setState(prev => {
          const existing = prev.steps.find(
            s => s.name === event.step && s.order === event.order
          );
          if (existing) {
            return {
              ...prev,
              status: 'streaming',
              steps: prev.steps.map(s =>
                s.name === event.step && s.order === event.order
                  ? { ...s, status: 'running' as const }
                  : s
              ),
            };
          }
          return {
            ...prev,
            status: 'streaming',
            steps: [
              ...prev.steps,
              {
                name: event.step!,
                order: event.order!,
                totalSteps: event.totalSteps!,
                status: 'running' as const,
              },
            ],
          };
        });
        break;

      case 'step_completed':
        setState(prev => ({
          ...prev,
          steps: prev.steps.map(s =>
            s.name === event.step && s.order === event.order
              ? {
                  ...s,
                  status: 'completed' as const,
                  output: event.output,
                  durationMs: event.durationMs,
                }
              : s
          ),
        }));
        break;

      case 'step_failed':
        setState(prev => ({
          ...prev,
          steps: prev.steps.map(s =>
            s.name === event.step && s.order === event.order
              ? {
                  ...s,
                  status: 'failed' as const,
                  errorMessage: event.errorMessage,
                  durationMs: event.durationMs,
                }
              : s
          ),
        }));
        break;

      case 'progress':
        setState(prev => ({
          ...prev,
          progress: event.progress ?? prev.progress,
        }));
        break;

      case 'completed':
        setState(prev => ({
          ...prev,
          status: 'completed',
          progress: 100,
          jobId: event.jobId ?? prev.jobId,
        }));
        break;

      case 'failed':
        setState(prev => ({
          ...prev,
          status: 'failed',
          error: event.errorMessage ?? 'Generation failed',
          jobId: event.jobId ?? prev.jobId,
        }));
        break;

      case 'server_shutdown':
        // Server is shutting down — close SSE and fall back to polling
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }
        setState(prev => {
          if (prev.status !== 'completed' && prev.status !== 'failed') {
            return { ...prev, status: 'polling_fallback', isFallback: true };
          }
          return prev;
        });
        break;

      case 'token_expired':
        // JWT expired during SSE reconnect — stop retrying and notify UI
        console.warn('[SSE] Token expired — authentication required');
        if (eventSourceRef.current) {
          eventSourceRef.current.close();
          eventSourceRef.current = null;
        }
        setState(prev => ({
          ...prev,
          status: 'token_expired',
          error: event.errorMessage ?? 'Authentication token expired. Please refresh.',
        }));
        break;
    }
  }, []);

  // ─── Polling Fallback ───────────────────────────────────────────────

  const startPollingFallback = useCallback(
    (id: string) => {
      if (pollIntervalRef.current) return; // Already polling

      setState(prev => ({ ...prev, status: 'polling_fallback', isFallback: true }));

      const poll = async () => {
        try {
          const res = await api<any>(`/api/v1/systems/jobs/${id}`);
          const job = res.data;

          if (!mountedRef.current) return;

          if (job.status === 'COMPLETED') {
            setState(prev => ({
              ...prev,
              status: 'completed',
              progress: 100,
              jobId: id,
            }));
            if (pollIntervalRef.current) {
              clearInterval(pollIntervalRef.current);
              pollIntervalRef.current = null;
            }
          } else if (job.status === 'FAILED') {
            setState(prev => ({
              ...prev,
              status: 'failed',
              error: job.errorMessage ?? 'Generation failed',
              jobId: id,
            }));
            if (pollIntervalRef.current) {
              clearInterval(pollIntervalRef.current);
              pollIntervalRef.current = null;
            }
          } else if (job.status === 'PROCESSING') {
            setState(prev => ({
              ...prev,
              progress: Math.min(prev.progress + 5, 90),
            }));
          }
        } catch {
          // Polling errors are non-fatal — just wait for the next interval
        }
      };

      // Immediate first poll
      poll();
      pollIntervalRef.current = setInterval(poll, POLL_INTERVAL_MS);
    },
    []
  );

  // ─── SSE Connection with Exponential Backoff ────────────────────────

  const connectSSE = useCallback(
    (id: string) => {
      if (!mountedRef.current) return;

      const url = `${BASE_URL}/api/v1/systems/jobs/${id}/stream`;

      try {
        const es = new EventSource(url, { withCredentials: true });
        eventSourceRef.current = es;

        const handleStandardEvent = (e: MessageEvent) => {
          try {
            const event: SSEEvent = JSON.parse(e.data);
            sseRetriesRef.current = 0; // Reset retries on successful message
            handleEvent(event);
          } catch {
            console.warn('[SSE] Failed to parse event data:', e.data);
          }
        };

        es.addEventListener('PROGRESS', handleStandardEvent);
        es.addEventListener('COMPLETED', handleStandardEvent);
        es.addEventListener('FAILED', handleStandardEvent);

        // Handle initial "connected" event
        es.addEventListener('INIT', (e: MessageEvent) => {
          try {
            const event: SSEEvent = JSON.parse(e.data);
            sseRetriesRef.current = 0;
            handleEvent(event);
          } catch {
            handleEvent({ type: 'connected' });
          }
        });

        // Handle heartbeat — resets retry counter and tracks last heartbeat time
        es.addEventListener('HEARTBEAT', () => {
          sseRetriesRef.current = 0;
          lastHeartbeatRef.current = Date.now();
        });

        // Handle expired JWT during reconnect — stop retrying, notify UI
        es.addEventListener('TOKEN_EXPIRED', (e: MessageEvent) => {
          try {
            const event: SSEEvent = JSON.parse(e.data);
            handleEvent(event);
          } catch {
            handleEvent({ type: 'token_expired', errorMessage: 'Token expired' });
          }
        });

        es.onerror = () => {
          // Don't reconnect if we're in a terminal state
          setState(prev => {
            if (prev.status === 'completed' || prev.status === 'failed') {
              es.close();
              eventSourceRef.current = null;
              return prev;
            }
            return prev;
          });

          sseRetriesRef.current++;
          const attempt = sseRetriesRef.current;

          if (attempt >= MAX_SSE_RETRIES) {
            console.warn(`[SSE] Max retries (${MAX_SSE_RETRIES}) reached — falling back to polling`);
            es.close();
            eventSourceRef.current = null;
            startPollingFallback(id);
            return;
          }

          const delay = getBackoffDelay(attempt);
          console.warn(
            `[SSE] Connection error (attempt ${attempt}/${MAX_SSE_RETRIES}). ` +
            `Reconnecting in ${delay}ms...`
          );

          es.close();
          eventSourceRef.current = null;

          setState(prev => ({
            ...prev,
            status: 'connecting',
            connectionAttempt: attempt,
          }));

          // Schedule reconnection with exponential backoff
          reconnectTimeoutRef.current = setTimeout(() => {
            if (mountedRef.current) {
              connectSSE(id);
            }
          }, delay);
        };
      } catch {
        // EventSource constructor failed (e.g., unsupported browser)
        console.warn('[SSE] EventSource not available — using polling fallback');
        startPollingFallback(id);
      }
    },
    [handleEvent, startPollingFallback]
  );

  // ─── Main Effect ────────────────────────────────────────────────────

  useEffect(() => {
    mountedRef.current = true;

    if (!jobId) return;

    setState({
      status: 'connecting',
      steps: [],
      progress: 0,
      error: null,
      jobId,
      isFallback: false,
      connectionAttempt: 0,
    });

    sseRetriesRef.current = 0;
    connectSSE(jobId);

    // ─── Cleanup ────────────────────────────────────────────────────

    return () => {
      mountedRef.current = false;

      if (eventSourceRef.current) {
        eventSourceRef.current.close();
        eventSourceRef.current = null;
      }
      if (pollIntervalRef.current) {
        clearInterval(pollIntervalRef.current);
        pollIntervalRef.current = null;
      }
      if (reconnectTimeoutRef.current) {
        clearTimeout(reconnectTimeoutRef.current);
        reconnectTimeoutRef.current = null;
      }
    };
  }, [jobId, connectSSE]);

  return state;
}
