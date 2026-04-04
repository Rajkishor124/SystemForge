'use client';

import { useEffect, useState } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import { Network, Download, Share, CheckCircle, Server, Database, Shield, Zap, ArrowRight, Loader2, AlertCircle, Lightbulb, AlertTriangle, ChevronRight } from 'lucide-react';
import { api, ApiError } from '@/lib/api';
import { motion, AnimatePresence } from 'motion/react';

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
    async function fetchConfig() {
      try {
        let currentConfigId = configId;
        
        // Smart fallback: If no configId provided in URL, try to load their latest project
        if (!currentConfigId) {
          const listRes = await api<{ content: SystemConfig[] }>('/api/v1/systems/configs?page=0&size=1');
          if (listRes.data && listRes.data.content.length > 0) {
            currentConfigId = listRes.data.content[0].id;
            // Update URL cleanly without triggering a full page re-render loop
            window.history.replaceState(null, '', `/result?configId=${currentConfigId}`);
          } else {
            setError('NO_PROJECT');
            setLoading(false);
            return;
          }
        }

        const res = await api<SystemConfig>(`/api/v1/systems/configs/${currentConfigId}`);
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
      <div className="flex-1 flex flex-col items-center justify-center p-8 gap-4 bg-[#090e1c]">
        <div className="relative">
          <div className="absolute inset-0 bg-primary-container/20 blur-2xl rounded-full animate-pulse"></div>
          <Loader2 className="w-10 h-10 text-primary-container animate-spin relative z-10" />
        </div>
        <p className="text-xs font-label uppercase tracking-widest text-[#dee1f7]/40 animate-pulse">Compiling Architecture...</p>
      </div>
    );
  }

  if (error || !config) {
    if (error === 'NO_PROJECT') {
      return (
        <div className="flex-1 flex items-center justify-center p-8 bg-[#090e1c] h-full min-h-[calc(100vh-64px)]">
          <div className="max-w-md w-full text-center glass-card rounded-xl p-10 border border-outline-variant/10">
            <div className="relative w-16 h-16 mx-auto mb-6">
              <div className="absolute inset-0 bg-primary-container/10 blur-xl rounded-full"></div>
              <Database className="w-16 h-16 text-primary-container/40 relative z-10" />
            </div>
            <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">No Project Selected</h2>
            <p className="text-sm text-[#dee1f7]/60 mb-8 leading-relaxed">
              You haven't generated any systems yet. Start a new project to view your architecture schema.
            </p>
            <button onClick={() => router.push('/dashboard')} className="cta-gradient text-on-primary px-6 py-2.5 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-all">
              Go to Dashboard
            </button>
          </div>
        </div>
      );
    }
    
    return (
      <div className="flex-1 flex items-center justify-center p-8 h-full min-h-[calc(100vh-64px)]">
        <div className="max-w-md w-full text-center glass-card rounded-xl p-8 border border-red-500/20">
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">Error</h2>
          <p className="text-sm text-[#dee1f7]/60 mb-6">{error || 'Config not found.'}</p>
          <button onClick={() => router.push('/dashboard')} className="bg-surface-container-high border border-outline-variant/20 text-[#e1fdff] px-5 py-2.5 rounded-lg font-bold text-sm hover:bg-surface-container-highest transition-colors">
            Return to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-7xl mx-auto w-full">
      {/* Header */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="flex flex-col md:flex-row justify-between items-start md:items-end mb-12 gap-6"
      >
        <div>
          <motion.div 
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 }}
            className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#00f2ff]/10 border border-[#00f2ff]/20 text-[#00f2ff] text-[10px] font-bold uppercase tracking-widest mb-4"
          >
            <CheckCircle className="w-3 h-3" />
            Forge Complete
          </motion.div>
          <h1 className="text-4xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">
            {config.configName} <span className="text-gradient">Blueprint</span>
          </h1>
          <p className="text-[#dee1f7]/60 text-sm">
            {formatModuleName(config.appType)} · {config.appScale} scale · Generated {new Date(config.createdAt).toLocaleDateString()}
          </p>
        </div>
        <div className="flex gap-3">
          <button className="px-5 py-2.5 rounded-xl bg-white/5 border border-white/10 text-xs font-bold hover:bg-white/10 transition-colors flex items-center gap-2">
            <Download className="w-4 h-4" /> Export
          </button>
          <Link href={`/architecture?configId=${configId}`} className="cta-gradient text-on-primary px-6 py-2.5 rounded-xl font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2">
            <Network className="w-4 h-4" /> View Map
          </Link>
        </div>
      </motion.div>

      {result ? (
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            {/* Executive Summary */}
            <motion.section 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.1 }}
              className="glass-card rounded-2xl p-8 border-white/5 bg-gradient-to-br from-white/[0.03] to-transparent"
            >
              <h2 className="text-xl font-bold font-headline mb-6 flex items-center gap-3">
                <span className="p-2 rounded-lg bg-primary-container/10 text-primary-container">
                  <Network className="w-5 h-5" />
                </span>
                Blueprint Overview
              </h2>
              <p className="text-[#dee1f7]/80 leading-relaxed text-sm whitespace-pre-line border-l-2 border-primary-container/30 pl-6 py-2">
                {result.architectureSummary || 'Architecture recommendations generated based on your system requirements.'}
              </p>
            </motion.section>

            {/* Modules */}
            {result.modules && result.modules.length > 0 && (
              <section className="space-y-6">
                <h2 className="text-xl font-bold font-headline mb-4 flex items-center gap-2">
                  System Modules <span className="text-[10px] font-mono text-[#dee1f7]/40 ml-2">{result.modules.length} Nodes</span>
                </h2>
                <div className="grid gap-6">
                  {result.modules.map((mod, idx) => {
                    const Icon = getModuleIcon(mod.module);
                    return (
                      <motion.div 
                        key={idx}
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.2 + idx * 0.1 }}
                        className="glass-card rounded-2xl p-6 border-white/10 hover:border-[#00f2ff]/30 transition-all duration-500 group"
                      >
                        <div className="flex items-center gap-4 mb-6">
                          <div className="p-3 rounded-xl bg-[#00f2ff]/10 text-[#00f2ff] border border-[#00f2ff]/20 group-hover:scale-110 transition-transform">
                            <Icon className="w-6 h-6" />
                          </div>
                          <h3 className="text-lg font-bold text-[#e1fdff] font-headline">{formatModuleName(mod.module)}</h3>
                        </div>
                        <div className="space-y-4 pl-0 md:pl-16">
                          {mod.recommendations.map((rec, ri) => (
                            <div key={ri} className="p-4 rounded-xl bg-white/[0.02] border border-white/5 hover:bg-white/[0.04] transition-colors">
                              <div className="flex items-center justify-between mb-2">
                                <span className="text-sm font-bold text-[#e1fdff]">{rec.title}</span>
                                <div className="flex items-center gap-2">
                                  <div className="w-16 h-1 bg-white/5 rounded-full overflow-hidden hidden sm:block">
                                    <div className="h-full bg-primary-container" style={{ width: `${rec.confidence * 100}%` }}></div>
                                  </div>
                                  <span className="text-[10px] font-mono text-primary-container font-black">
                                    {Math.round(rec.confidence * 100)}%
                                  </span>
                                </div>
                              </div>
                              <p className="text-xs text-[#dee1f7]/60 leading-relaxed mb-4">{rec.description}</p>
                              {rec.alternatives && rec.alternatives.length > 0 && (
                                <div className="flex flex-wrap gap-2 mt-2 pt-3 border-t border-white/5">
                                  <span className="text-[9px] uppercase tracking-widest text-[#dee1f7]/30 font-black">Alternatives</span>
                                  {rec.alternatives.map((alt, ai) => (
                                    <span key={ai} className="px-2 py-0.5 rounded bg-white/5 text-[10px] font-mono text-[#dee1f7]/50 border border-white/5">
                                      {alt}
                                    </span>
                                  ))}
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      </motion.div>
                    );
                  })}
                </div>
              </section>
            )}
          </div>

          {/* Sidebar */}
          <motion.div 
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3 }}
            className="space-y-8"
          >
            {/* AI Improvements */}
            {result.aiImprovements && result.aiImprovements.length > 0 && (
              <div className="glass-card rounded-2xl p-6 border-white/5 bg-gradient-to-br from-[#00f2ff]/5 to-transparent">
                <h3 className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/40 mb-6 flex items-center gap-3">
                  <Lightbulb className="w-4 h-4 text-[#00f2ff]" />
                  AI Optimization
                </h3>
                <div className="space-y-4">
                  {result.aiImprovements.map((item, i) => (
                    <div key={i} className="flex gap-3 text-xs text-[#dee1f7]/70 leading-relaxed group">
                      <div className="w-1.5 h-1.5 rounded-full bg-[#00f2ff] mt-1.5 group-hover:scale-125 transition-transform" />
                      {item}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Trade-offs */}
            {result.aiTradeoffs && result.aiTradeoffs.length > 0 && (
              <div className="glass-card rounded-2xl p-6 border-white/5 bg-gradient-to-br from-tertiary-container/5 to-transparent">
                <h3 className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/40 mb-6 flex items-center gap-3">
                  <AlertTriangle className="w-4 h-4 text-tertiary-container" />
                  Engineering Trade-offs
                </h3>
                <div className="space-y-4">
                  {result.aiTradeoffs.map((item, i) => (
                    <div key={i} className="flex gap-3 text-xs text-[#dee1f7]/70 leading-relaxed group">
                      <div className="w-1.5 h-1.5 rounded bg-tertiary-container mt-1.5 group-hover:rotate-45 transition-transform" />
                      {item}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Next Steps */}
            <div className="glass-card rounded-2xl p-6 border-white/5">
              <h3 className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/40 mb-6">Operations</h3>
              <div className="space-y-3">
                <Link href={`/architecture?configId=${configId}`} className="w-full flex items-center justify-between p-4 rounded-xl bg-white/5 hover:bg-[#00f2ff]/10 transition-all border border-white/5 group">
                  <div className="flex items-center gap-3">
                    <Network className="w-4 h-4 text-[#00f2ff]" />
                    <span className="text-sm font-bold text-[#e1fdff]">Explore Topology</span>
                  </div>
                  <ArrowRight className="w-4 h-4 text-[#00f2ff] opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
                </Link>
                <Link href="/create" className="w-full flex items-center justify-between p-4 rounded-xl bg-white/5 hover:bg-white/10 transition-all border border-white/5 group">
                  <div className="flex items-center gap-3">
                    <Zap className="w-4 h-4 text-secondary-container" />
                    <span className="text-sm font-bold text-[#e1fdff]">New Configuration</span>
                  </div>
                  <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 -translate-x-2 group-hover:translate-x-0 transition-all" />
                </Link>
              </div>
            </div>
          </motion.div>
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
