'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { Network, Download, Share, CheckCircle, Server, Database, Shield, Zap, ArrowRight, Loader2, AlertCircle, Lightbulb, AlertTriangle, ChevronRight } from 'lucide-react';
import { api, ApiError } from '@/lib/api';

// ─── Types matching backend DTOs ─────────────────────────────────────────────

interface RecommendationItem {
  title: string;
  description: string;
  confidence: number;
  score: number;
  alternatives: string[];
}

interface ModuleRecommendation {
  module: string;
  recommendations: RecommendationItem[];
}

interface RecommendationResult {
  appType: string;
  appScale: string;
  architectureSummary: string;
  modules: ModuleRecommendation[];
  aiImprovements: string[];
  aiTradeoffs: string[];
}

interface SystemConfig {
  id: string;
  configName: string;
  appType: string;
  appScale: string;
  selectedSystemsJson: string;
  generatedOutputJson: string;
  generated: boolean;
  createdAt: string;
}

// Module icons map
const MODULE_ICONS: Record<string, typeof Server> = {
  AUTH: Shield,
  ARCHITECTURE: Server,
  DATABASE: Database,
  PAYMENT: Zap,
  NOTIFICATION: Zap,
  CACHING: Database,
  MESSAGING: Server,
  SEARCH: Server,
  MONITORING: Server,
  STORAGE: Database,
};

function getModuleIcon(module: string) {
  return MODULE_ICONS[module] || Server;
}

function formatModuleName(module: string): string {
  return module.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

export default function ResultPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const configId = searchParams.get('configId');

  const [config, setConfig] = useState<SystemConfig | null>(null);
  const [result, setResult] = useState<RecommendationResult | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!configId) {
      router.push('/dashboard');
      return;
    }

    async function fetchConfig() {
      try {
        const res = await api<SystemConfig>(`/api/v1/systems/configs/${configId}`);
        setConfig(res.data);

        if (res.data.generatedOutputJson) {
          const parsed: RecommendationResult = JSON.parse(res.data.generatedOutputJson);
          setResult(parsed);
        }
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.message);
        } else {
          setError('Failed to load architecture result.');
        }
      } finally {
        setLoading(false);
      }
    }

    fetchConfig();
  }, [configId, router]);

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <Loader2 className="w-8 h-8 text-primary-container animate-spin" />
      </div>
    );
  }

  if (error || !config) {
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="max-w-md w-full text-center glass-card rounded-xl p-8">
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">Error</h2>
          <p className="text-sm text-[#dee1f7]/60 mb-6">{error || 'Config not found.'}</p>
          <button onClick={() => router.push('/dashboard')} className="cta-gradient text-on-primary px-5 py-2.5 rounded-lg font-bold text-sm">
            Go to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-7xl mx-auto w-full">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-8 gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-container/10 border border-primary-container/20 text-primary-container text-[10px] font-bold uppercase tracking-widest mb-4">
            <CheckCircle className="w-3 h-3" />
            Generation Complete
          </div>
          <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">{config.configName}</h1>
          <p className="text-[#dee1f7]/60 text-sm">
            {formatModuleName(config.appType)} · {config.appScale} scale · Generated {new Date(config.createdAt).toLocaleDateString()}
          </p>
        </div>
        <div className="flex gap-3">
          <button className="px-4 py-2 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors flex items-center gap-2">
            <Share className="w-4 h-4" /> Share
          </button>
          <button className="px-4 py-2 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors flex items-center gap-2">
            <Download className="w-4 h-4" /> Export
          </button>
          <Link href={`/architecture?configId=${configId}`} className="cta-gradient text-on-primary px-5 py-2 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2">
            <Network className="w-4 h-4" /> View Graph
          </Link>
        </div>
      </div>

      {result ? (
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            {/* Executive Summary */}
            <section className="glass-card rounded-xl p-8">
              <h2 className="text-xl font-bold font-headline mb-4 border-b border-outline-variant/20 pb-2">Executive Summary</h2>
              <p className="text-[#dee1f7]/80 leading-relaxed text-sm whitespace-pre-line">
                {result.architectureSummary || 'Architecture recommendations generated based on your system requirements.'}
              </p>
            </section>

            {/* Modules */}
            {result.modules && result.modules.length > 0 && (
              <section className="space-y-4">
                <h2 className="text-xl font-bold font-headline mb-4">System Modules</h2>
                {result.modules.map((mod, idx) => {
                  const Icon = getModuleIcon(mod.module);
                  return (
                    <div key={idx} className="glass-card rounded-xl p-6">
                      <div className="flex items-start gap-4 mb-4">
                        <div className="p-3 rounded-lg bg-primary-container/10 text-primary-container shrink-0">
                          <Icon className="w-6 h-6" />
                        </div>
                        <h3 className="text-lg font-bold text-[#e1fdff]">{formatModuleName(mod.module)}</h3>
                      </div>
                      <div className="space-y-3 pl-16">
                        {mod.recommendations.map((rec, ri) => (
                          <div key={ri} className="p-3 rounded bg-surface-container-lowest border border-outline-variant/10">
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-sm font-bold text-[#e1fdff]">{rec.title}</span>
                              <span className="text-[10px] font-mono text-primary-container bg-primary-container/10 px-2 py-0.5 rounded">
                                {Math.round(rec.confidence * 100)}% confidence
                              </span>
                            </div>
                            <p className="text-xs text-[#dee1f7]/70 mb-2">{rec.description}</p>
                            {rec.alternatives && rec.alternatives.length > 0 && (
                              <div className="flex flex-wrap gap-1.5 mt-2">
                                <span className="text-[10px] text-[#dee1f7]/40">Alternatives:</span>
                                {rec.alternatives.map((alt, ai) => (
                                  <span key={ai} className="px-2 py-0.5 rounded bg-surface-container text-[10px] font-mono text-[#dee1f7]/60">
                                    {alt}
                                  </span>
                                ))}
                              </div>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  );
                })}
              </section>
            )}
          </div>

          {/* Sidebar */}
          <div className="space-y-8">
            {/* AI Improvements */}
            {result.aiImprovements && result.aiImprovements.length > 0 && (
              <div className="glass-card rounded-xl p-6">
                <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4 flex items-center gap-2">
                  <Lightbulb className="w-4 h-4 text-primary-container" />
                  AI Improvements
                </h3>
                <div className="space-y-3">
                  {result.aiImprovements.map((item, i) => (
                    <div key={i} className="flex gap-2 text-xs text-[#dee1f7]/70">
                      <ChevronRight className="w-3 h-3 text-primary-container shrink-0 mt-0.5" />
                      {item}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Trade-offs */}
            {result.aiTradeoffs && result.aiTradeoffs.length > 0 && (
              <div className="glass-card rounded-xl p-6">
                <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4 flex items-center gap-2">
                  <AlertTriangle className="w-4 h-4 text-tertiary-container" />
                  Trade-offs
                </h3>
                <div className="space-y-3">
                  {result.aiTradeoffs.map((item, i) => (
                    <div key={i} className="flex gap-2 text-xs text-[#dee1f7]/70">
                      <ChevronRight className="w-3 h-3 text-tertiary-container shrink-0 mt-0.5" />
                      {item}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Next Steps */}
            <div className="glass-card rounded-xl p-6">
              <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4">Next Steps</h3>
              <div className="space-y-3">
                <Link href={`/architecture?configId=${configId}`} className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                  <div className="flex items-center gap-3">
                    <Network className="w-4 h-4 text-primary-container" />
                    <span className="text-sm">View Architecture Graph</span>
                  </div>
                  <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                </Link>
                <Link href="/create" className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                  <div className="flex items-center gap-3">
                    <Zap className="w-4 h-4 text-secondary-container" />
                    <span className="text-sm">Create New Design</span>
                  </div>
                  <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                </Link>
                <Link href="/dashboard" className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                  <div className="flex items-center gap-3">
                    <Database className="w-4 h-4 text-tertiary-container" />
                    <span className="text-sm">Back to Projects</span>
                  </div>
                  <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                </Link>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="glass-card rounded-xl p-12 text-center">
          <AlertCircle className="w-12 h-12 text-[#dee1f7]/30 mx-auto mb-4" />
          <h2 className="text-xl font-bold font-headline mb-2 text-[#e1fdff]">No Architecture Generated</h2>
          <p className="text-sm text-[#dee1f7]/60 mb-6">This config hasn&apos;t been processed by the AI engine yet.</p>
          <Link href={`/processing?configId=${configId}`} className="cta-gradient text-on-primary px-6 py-2.5 rounded-lg font-bold text-sm inline-flex items-center gap-2">
            <Zap className="w-4 h-4" /> Generate Now
          </Link>
        </div>
      )}
    </div>
  );
}
