'use client';

import { useEffect, useState, useRef, useCallback } from 'react';

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
  | 'polling_fallback';

export interface GenerationStreamState {
  status: StreamStatus;
  steps: GenerationStep[];
  progress: number;
  error: string | null;
  jobId: string | null;
  isFallback: boolean;
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
}

// ─── Constants ──────────────────────────────────────────────────────────────────

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';
const POLL_INTERVAL_MS = 3000;
const SSE_RECONNECT_DELAY_MS = 2000;
const MAX_SSE_RETRIES = 3;

// ─── Hook ───────────────────────────────────────────────────────────────────────

/**
 * Custom hook for consuming the generation SSE stream.
 *
 * Usage:
 * ```tsx
 * const { status, steps, progress, error } = useGenerationStream(jobId);
 * ```
 *
 * Lifecycle:
 * 1. Opens EventSource to `/api/v1/systems/jobs/{jobId}/stream`
 * 2. Maps backend events to step state updates
 * 3. On SSE failure → falls back to polling `GET /jobs/{jobId}`
 * 4. On `completed` event → sets jobId for result fetching
 * 5. Cleans up EventSource on unmount
 */
export function useGenerationStream(jobId: string | null): GenerationStreamState {
  const [state, setState] = useState<GenerationStreamState>({
    status: 'idle',
    steps: [],
    progress: 0,
    error: null,
    jobId: null,
    isFallback: false,
  });

  const eventSourceRef = useRef<EventSource | null>(null);
  const pollIntervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const sseRetriesRef = useRef(0);
  const mountedRef = useRef(true);

  // ─── SSE Event Handlers ─────────────────────────────────────────────

  const handleEvent = useCallback((event: SSEEvent) => {
    if (!mountedRef.current) return;

    switch (event.type) {
      case 'connected':
        setState(prev => ({ ...prev, status: 'connected' }));
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
    }
  }, []);

  // ─── Polling Fallback ───────────────────────────────────────────────

  const startPollingFallback = useCallback(
    (id: string) => {
      if (pollIntervalRef.current) return; // Already polling

      setState(prev => ({ ...prev, status: 'polling_fallback' }));

      const poll = async () => {
        try {
          const res = await fetch(
            `${BASE_URL}/api/v1/systems/jobs/${id}`,
            { credentials: 'include' }
          );
          if (!res.ok) return;

          const json = await res.json();
          const job = json.data;

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
              progress: Math.min(prev.progress + 10, 90),
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

  // ─── SSE Connection ─────────────────────────────────────────────────

  useEffect(() => {
    mountedRef.current = true;

    if (!jobId) return;

    setState(prev => ({
      ...prev,
      status: 'connecting',
      steps: [],
      progress: 0,
      error: null,
      jobId,
    }));

    const url = `${BASE_URL}/api/v1/systems/jobs/${jobId}/stream`;

    try {
      const es = new EventSource(url, { withCredentials: true });
      eventSourceRef.current = es;

      es.addEventListener('generation-progress', (e: MessageEvent) => {
        try {
          const event: SSEEvent = JSON.parse(e.data);
          sseRetriesRef.current = 0; // Reset retries on successful message
          handleEvent(event);
        } catch {
          console.warn('[SSE] Failed to parse event data');
        }
      });

      // Handle initial "connected" event (different event name)
      es.addEventListener('connected', (e: MessageEvent) => {
        try {
          const event: SSEEvent = JSON.parse(e.data);
          handleEvent(event);
        } catch {
          // connected event might be plain text
          handleEvent({ type: 'connected' });
        }
      });

      es.onerror = () => {
        sseRetriesRef.current++;
        console.warn(
          `[SSE] Connection error (attempt ${sseRetriesRef.current}/${MAX_SSE_RETRIES})`
        );

        if (sseRetriesRef.current >= MAX_SSE_RETRIES) {
          console.warn('[SSE] Max retries reached — falling back to polling');
          es.close();
          eventSourceRef.current = null;
          startPollingFallback(jobId);
        }
      };
    } catch {
      // EventSource constructor failed (e.g., unsupported browser)
      console.warn('[SSE] EventSource not available — using polling fallback');
      startPollingFallback(jobId);
    }

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
    };
  }, [jobId, handleEvent, startPollingFallback]);

  return state;
}
