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
  BookOpen,
  Layers,
  Code2,
  Scale,
  ClipboardList,
  FileCheck2,
} from 'lucide-react';
import { api, ApiError } from '@/lib/api';
import {
  useGenerationStream,
  type GenerationStep,
  type StreamStatus,
} from '@/lib/useGenerationStream';

// ─── MABA Phase Definitions ─────────────────────────────────────────────────────

const MABA_PHASES = [
  { name: 'Config Validation',          icon: Shield,        color: '#00f2ff', badge: '✓' },
  { name: 'Requirement Decomposition',  icon: BookOpen,      color: '#ff6b35', badge: 'Ω' },
  { name: 'Knowledge Retrieval & Requirements', icon: Brain, color: '#a855f7', badge: '◈' },
  { name: 'System Architecture',        icon: Layers,        color: '#00c9ff', badge: '◬' },
  { name: 'Data & API Design',          icon: Database,      color: '#22c55e', badge: '◉' },
  { name: 'Scalability & Security',     icon: Scale,         color: '#f59e0b', badge: '⟁' },
  { name: 'Implementation Planning',    icon: ClipboardList, color: '#ec4899', badge: '▣' },
  { name: 'Persisting Results',         icon: FileCheck2,    color: '#06b6d4', badge: 'Σ' },
];

function getPhaseConfig(stepName: string) {
  return MABA_PHASES.find(p => stepName.includes(p.name.split(' ')[0])) || MABA_PHASES[0];
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
      }, 1500);
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

  // ─── Determine Active Phase ─────────────────────────────────────────

  const activePhaseIndex = stream.steps.length > 0
    ? Math.max(...stream.steps.filter(s => s.status === 'running').map(s => s.order), 0)
    : 0;

  // ─── Main Render ────────────────────────────────────────────────────

  return (
    <div className="flex-1 flex items-center justify-center p-8">
      <div className="max-w-2xl w-full">

        {/* Header Section */}
        <div className="text-center mb-10">
          {/* Animated Agent Orb */}
          <div className="relative w-28 h-28 mx-auto mb-8">
            <div className="absolute inset-0 rounded-full border-2 border-surface-container-highest"></div>
            <div className="absolute inset-0 rounded-full border-2 border-[#ff6b35]/60 border-t-transparent animate-spin" style={{ animationDuration: '3s' }}></div>
            <div className="absolute inset-[-4px] rounded-full border-2 border-[#00c9ff]/30 border-b-transparent animate-spin" style={{ animationDuration: '5s', animationDirection: 'reverse' }}></div>
            <div className="absolute inset-0 flex items-center justify-center">
              <div className="text-3xl font-black text-[#ff6b35] animate-pulse">Ω</div>
            </div>
          </div>

          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#ff6b35]/10 border border-[#ff6b35]/20 text-[#ff6b35] text-[10px] font-bold uppercase tracking-widest mb-4">
            <Brain className="w-3 h-3" />
            Multi-Agent Architecture System
          </div>

          <h2 className="text-2xl font-bold font-headline mb-2 text-[#e1fdff]">
            Agents Forging Your System
          </h2>

          {/* Connection Status Badge */}
          <ConnectionBadge status={stream.status} />
        </div>

        {/* Progress Bar */}
        <div className="w-full h-1.5 bg-surface-container-highest rounded-full overflow-hidden mb-8">
          <div
            className="h-full rounded-full transition-all duration-1000 ease-out"
            style={{
              width: `${stream.progress}%`,
              background: 'linear-gradient(90deg, #ff6b35, #00c9ff, #a855f7)',
            }}
          />
        </div>

        {/* MABA Pipeline Steps */}
        <div className="space-y-2">
          {stream.steps.length > 0 ? (
            stream.steps
              .sort((a, b) => a.order - b.order)
              .map((step) => <MabaStepRow key={`${step.name}-${step.order}`} step={step} />)
          ) : (
            <MabaPlaceholderSteps status={stream.status} />
          )}
        </div>

        {/* Completion Message */}
        {stream.status === 'completed' && (
          <div className="mt-8 text-center">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-green-500/10 border border-green-500/20 text-green-400 text-sm font-bold">
              <CheckCircle className="w-4 h-4" />
              All agents completed · Redirecting to your design document...
            </div>
          </div>
        )}

        {/* Fallback indicator */}
        {stream.isFallback && (
          <div className="mt-4 px-4 py-2 rounded-lg bg-yellow-500/10 border border-yellow-500/20 text-xs text-yellow-400 text-center">
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
    idle: { icon: Loader2, text: 'Initializing orchestrator...', color: 'text-[#dee1f7]/40', spin: true },
    connecting: { icon: Wifi, text: 'Connecting to agent stream...', color: 'text-[#dee1f7]/60', spin: false },
    connected: { icon: Wifi, text: 'Stream connected', color: 'text-primary-container', spin: false },
    streaming: { icon: Wifi, text: 'Agents working', color: 'text-[#ff6b35]', spin: false },
    polling_fallback: { icon: RefreshCw, text: 'Polling for updates', color: 'text-yellow-400', spin: true },
    completed: { icon: CheckCircle, text: 'Pipeline complete', color: 'text-green-400', spin: false },
    failed: { icon: AlertCircle, text: 'Pipeline failed', color: 'text-red-400', spin: false },
  }[status];

  const Icon = config.icon;

  return (
    <div className={`inline-flex items-center gap-2 text-xs ${config.color}`}>
      <Icon className={`w-3 h-3 ${config.spin ? 'animate-spin' : ''}`} />
      <span className="font-label uppercase tracking-widest">{config.text}</span>
    </div>
  );
}

function MabaStepRow({ step }: { step: GenerationStep }) {
  const isRunning = step.status === 'running';
  const isCompleted = step.status === 'completed';
  const isFailed = step.status === 'failed';

  // Match to MABA phase config
  const phaseConfig = MABA_PHASES[step.order - 1] || MABA_PHASES[0];
  const Icon = phaseConfig.icon;
  const color = phaseConfig.color;

  return (
    <div
      className={`flex items-center gap-4 px-4 py-3 rounded-xl transition-all duration-500 ${
        isRunning
          ? 'bg-white/[0.04] border border-white/10 shadow-lg'
          : isCompleted
          ? 'opacity-60'
          : isFailed
          ? 'bg-red-500/5 border border-red-500/20'
          : 'opacity-20'
      }`}
      style={isRunning ? { borderColor: `${color}44`, boxShadow: `0 0 20px ${color}10` } : {}}
    >
      {/* Phase badge */}
      <div
        className={`w-9 h-9 rounded-lg flex items-center justify-center text-sm font-black shrink-0 transition-all duration-500 ${
          isRunning
            ? 'scale-110'
            : ''
        }`}
        style={{
          background: isRunning ? `${color}22` : isCompleted ? `${color}11` : 'rgba(255,255,255,0.03)',
          border: `1px solid ${isRunning ? `${color}55` : isCompleted ? `${color}33` : 'rgba(255,255,255,0.05)'}`,
          color: isCompleted || isRunning ? color : 'rgba(255,255,255,0.2)',
        }}
      >
        {isCompleted ? (
          <CheckCircle className="w-4 h-4" />
        ) : isRunning ? (
          <Loader2 className="w-4 h-4 animate-spin" />
        ) : isFailed ? (
          <AlertCircle className="w-4 h-4 text-red-400" />
        ) : (
          <span className="text-xs">{phaseConfig.badge}</span>
        )}
      </div>

      {/* Step info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <span
            className={`text-sm font-semibold block ${
              isRunning ? 'text-[#e1fdff]' : isFailed ? 'text-red-400' : 'text-on-surface'
            }`}
          >
            {step.name}
          </span>
          {isRunning && (
            <span className="text-[9px] font-mono uppercase tracking-wider animate-pulse" style={{ color }}>
              active
            </span>
          )}
        </div>
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
      <span className="text-[10px] font-mono text-[#dee1f7]/20 shrink-0">
        {step.order}/{step.totalSteps}
      </span>
    </div>
  );
}

function MabaPlaceholderSteps({ status }: { status: StreamStatus }) {
  return (
    <>
      {MABA_PHASES.map((phase, i) => {
        const Icon = phase.icon;
        const isFirst = i === 0 && (status === 'connected' || status === 'streaming');
        return (
          <div
            key={i}
            className={`flex items-center gap-4 px-4 py-3 rounded-xl transition-all duration-500 ${
              isFirst
                ? 'bg-white/[0.04] border border-white/10'
                : status === 'connecting' || status === 'idle'
                ? 'opacity-10'
                : 'opacity-15'
            }`}
            style={isFirst ? { borderColor: `${phase.color}44` } : {}}
          >
            <div
              className="w-9 h-9 rounded-lg flex items-center justify-center text-sm shrink-0"
              style={{
                background: isFirst ? `${phase.color}22` : 'rgba(255,255,255,0.03)',
                border: `1px solid ${isFirst ? `${phase.color}55` : 'rgba(255,255,255,0.05)'}`,
                color: isFirst ? phase.color : 'rgba(255,255,255,0.15)',
              }}
            >
              {isFirst ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <span className="text-xs">{phase.badge}</span>
              )}
            </div>
            <span className={`text-sm font-medium ${isFirst ? 'text-[#e1fdff]' : 'text-on-surface'}`}>
              {phase.name}
            </span>
          </div>
        );
      })}
    </>
  );
}
