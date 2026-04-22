'use client';

import { useEffect, useState, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  Brain,
  Database,
  Server,
  Shield,
  Zap,
  CheckCircle,
  AlertCircle,
  Loader2,
  Wifi,
  WifiOff,
  RefreshCw,
} from 'lucide-react';
import { api, ApiError } from '@/lib/api';
import {
  useGenerationStream,
  type GenerationStep,
  type StreamStatus,
} from '@/lib/useGenerationStream';

// ─── Step Icons ─────────────────────────────────────────────────────────────────

const STEP_ICONS: Record<string, typeof Brain> = {
  'Config Validation': Shield,
  'AI Generation': Brain,
  'Persisting Results': Database,
};

function getStepIcon(stepName: string) {
  return STEP_ICONS[stepName] || Server;
}

// ─── Main Component ─────────────────────────────────────────────────────────────

export default function ProcessingPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const configId = searchParams.get('configId');

  const [jobId, setJobId] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const apiCalledRef = useRef(false);

  // SSE-driven generation state
  const stream = useGenerationStream(jobId);

  // ─── Submit Generation Job ──────────────────────────────────────────

  useEffect(() => {
    if (!configId || apiCalledRef.current) return;
    apiCalledRef.current = true;

    async function submitJob() {
      try {
        const res = await api<{ id: string }>(`/api/v1/systems/configs/${configId}/generate`, {
          method: 'POST',
        });

        // 202 Accepted → extract jobId
        if (res.data?.id) {
          setJobId(res.data.id);
        }
      } catch (err) {
        if (err instanceof ApiError) {
          if (err.message?.includes('already generated')) {
            // Config already processed → skip to result
            router.push(`/result?configId=${configId}`);
          } else {
            setSubmitError(err.message);
          }
        } else {
          setSubmitError('Failed to start architecture generation. Please try again.');
        }
      }
    }

    submitJob();
  }, [configId, router]);

  // ─── Redirect on Completion ─────────────────────────────────────────

  useEffect(() => {
    if (stream.status === 'completed') {
      const timeout = setTimeout(() => {
        router.push(`/result?configId=${configId}`);
      }, 1200);
      return () => clearTimeout(timeout);
    }
  }, [stream.status, configId, router]);

  // ─── No configId Guard ──────────────────────────────────────────────

  useEffect(() => {
    if (!configId) {
      router.push('/create');
    }
  }, [configId, router]);

  // ─── Error State ────────────────────────────────────────────────────

  const errorMessage = submitError || stream.error;

  if (errorMessage) {
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="max-w-md w-full text-center glass-card rounded-xl p-8">
          <div className="w-16 h-16 mx-auto mb-6 rounded-full bg-red-500/10 flex items-center justify-center">
            <AlertCircle className="w-8 h-8 text-red-400" />
          </div>
          <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">Generation Failed</h2>
          <p className="text-sm text-[#dee1f7]/60 mb-6">{errorMessage}</p>
          <div className="flex gap-3 justify-center">
            <button
              onClick={() => router.push('/create')}
              className="px-5 py-2.5 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors"
            >
              Back to Create
            </button>
            <button
              onClick={() => {
                setSubmitError(null);
                setJobId(null);
                apiCalledRef.current = false;
              }}
              className="cta-gradient text-on-primary px-5 py-2.5 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

  // ─── Main Render ────────────────────────────────────────────────────

  return (
    <div className="flex-1 flex items-center justify-center p-8">
      <div className="max-w-lg w-full text-center">

        {/* Animated Brain Orb */}
        <div className="relative w-32 h-32 mx-auto mb-12">
          <div className="absolute inset-0 rounded-full border-4 border-surface-container-highest"></div>
          <div className="absolute inset-0 rounded-full border-4 border-primary-container border-t-transparent animate-spin"></div>
          <div className="absolute inset-0 flex items-center justify-center">
            <Brain className="w-12 h-12 text-primary-container animate-pulse" />
          </div>

          {/* Decorative nodes */}
          <div className="absolute -top-4 -left-4 w-8 h-8 rounded-full bg-surface-container border border-primary-container/30 flex items-center justify-center animate-bounce" style={{ animationDelay: '0ms' }}>
            <Database className="w-3 h-3 text-primary-container" />
          </div>
          <div className="absolute top-1/2 -right-6 w-8 h-8 rounded-full bg-surface-container border border-secondary-container/30 flex items-center justify-center animate-bounce" style={{ animationDelay: '200ms' }}>
            <Server className="w-3 h-3 text-secondary-container" />
          </div>
          <div className="absolute -bottom-4 left-4 w-8 h-8 rounded-full bg-surface-container border border-tertiary-container/30 flex items-center justify-center animate-bounce" style={{ animationDelay: '400ms' }}>
            <Zap className="w-3 h-3 text-tertiary-container" />
          </div>
        </div>

        <h2 className="text-2xl font-bold font-headline mb-3 text-[#e1fdff]">Synthesizing Architecture</h2>

        {/* Connection Status Badge */}
        <ConnectionBadge status={stream.status} />

        {/* Progress Bar */}
        <div className="w-full h-2 bg-surface-container-highest rounded-full overflow-hidden mb-8 mt-6">
          <div
            className="h-full bg-gradient-to-r from-primary-container to-secondary-container rounded-full transition-all duration-700 ease-out"
            style={{ width: `${stream.progress}%` }}
          />
        </div>

        {/* Real Pipeline Steps */}
        <div className="space-y-3 text-left">
          {stream.steps.length > 0 ? (
            stream.steps
              .sort((a, b) => a.order - b.order)
              .map((step) => <StepRow key={`${step.name}-${step.order}`} step={step} />)
          ) : (
            // Placeholder while waiting for first event
            <WaitingForEvents status={stream.status} />
          )}
        </div>

        {/* Completion Message */}
        {stream.status === 'completed' && (
          <p className="mt-6 text-sm text-primary-container animate-pulse">
            Redirecting to results...
          </p>
        )}

        {/* Fallback indicator */}
        {stream.isFallback && (
          <div className="mt-4 px-4 py-2 rounded-lg bg-yellow-500/10 border border-yellow-500/20 text-xs text-yellow-400">
            ⚠️ AI service using rule-based fallback. Results may be limited.
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Sub-components ─────────────────────────────────────────────────────────────

function ConnectionBadge({ status }: { status: StreamStatus }) {
  const config = {
    idle: { icon: Loader2, text: 'Initializing...', color: 'text-[#dee1f7]/40', spin: true },
    connecting: { icon: Wifi, text: 'Connecting to stream...', color: 'text-[#dee1f7]/60', spin: false },
    connected: { icon: Wifi, text: 'Stream connected', color: 'text-primary-container', spin: false },
    streaming: { icon: Wifi, text: 'Receiving updates', color: 'text-primary-container', spin: false },
    polling_fallback: { icon: RefreshCw, text: 'Polling for updates', color: 'text-yellow-400', spin: true },
    completed: { icon: CheckCircle, text: 'Generation complete', color: 'text-green-400', spin: false },
    failed: { icon: AlertCircle, text: 'Generation failed', color: 'text-red-400', spin: false },
  }[status];

  const Icon = config.icon;

  return (
    <div className={`inline-flex items-center gap-2 text-xs ${config.color}`}>
      <Icon className={`w-3 h-3 ${config.spin ? 'animate-spin' : ''}`} />
      <span className="font-label uppercase tracking-widest">{config.text}</span>
    </div>
  );
}

function StepRow({ step }: { step: GenerationStep }) {
  const Icon = getStepIcon(step.name);
  const isRunning = step.status === 'running';
  const isCompleted = step.status === 'completed';
  const isFailed = step.status === 'failed';

  return (
    <div
      className={`flex items-center gap-4 p-3 rounded-lg transition-all duration-500 ${
        isRunning
          ? 'bg-primary-container/10 border border-primary-container/30'
          : isCompleted
          ? 'opacity-70'
          : isFailed
          ? 'bg-red-500/10 border border-red-500/30'
          : 'opacity-20'
      }`}
    >
      <div
        className={`p-2 rounded-full ${
          isRunning
            ? 'bg-primary-container text-on-primary'
            : isCompleted
            ? 'bg-surface-container-high text-primary-container'
            : isFailed
            ? 'bg-red-500/20 text-red-400'
            : 'bg-surface-container text-on-surface-variant'
        }`}
      >
        {isRunning ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : isCompleted ? (
          <CheckCircle className="w-4 h-4" />
        ) : isFailed ? (
          <AlertCircle className="w-4 h-4" />
        ) : (
          <Icon className="w-4 h-4" />
        )}
      </div>

      <div className="flex-1 min-w-0">
        <span
          className={`text-sm font-medium block ${
            isRunning
              ? 'text-primary-container'
              : isFailed
              ? 'text-red-400'
              : 'text-on-surface'
          }`}
        >
          {step.name}
        </span>
        {isCompleted && step.durationMs != null && (
          <span className="text-[10px] font-mono text-[#dee1f7]/30">
            {step.durationMs < 1000
              ? `${step.durationMs}ms`
              : `${(step.durationMs / 1000).toFixed(1)}s`}
          </span>
        )}
        {isFailed && step.errorMessage && (
          <span className="text-[10px] text-red-400/60 block truncate">
            {step.errorMessage}
          </span>
        )}
      </div>

      {/* Step counter */}
      <span className="text-[10px] font-mono text-[#dee1f7]/20">
        {step.order}/{step.totalSteps}
      </span>
    </div>
  );
}

function WaitingForEvents({ status }: { status: StreamStatus }) {
  const placeholders = [
    { name: 'Config Validation', icon: Shield },
    { name: 'AI Generation', icon: Brain },
    { name: 'Persisting Results', icon: Database },
  ];

  return (
    <>
      {placeholders.map((p, i) => (
        <div
          key={i}
          className={`flex items-center gap-4 p-3 rounded-lg transition-all duration-500 ${
            status === 'connecting' || status === 'idle'
              ? 'opacity-20'
              : i === 0
              ? 'bg-primary-container/10 border border-primary-container/30 opacity-100'
              : 'opacity-20'
          }`}
        >
          <div className="p-2 rounded-full bg-surface-container text-on-surface-variant">
            <p.icon className="w-4 h-4" />
          </div>
          <span className="text-sm font-medium text-on-surface">{p.name}</span>
        </div>
      ))}
    </>
  );
}
