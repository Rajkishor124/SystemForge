'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { Plus, LayoutGrid, List, Brain, Activity, ArrowRight, Zap, Shield, Database, Loader2, Trash2, AlertCircle, Server, Network } from 'lucide-react';
import { api, ApiError } from '@/lib/api';

// ─── Types ───────────────────────────────────────────────────────────────────

interface SystemConfig {
  id: string;
  configName: string;
  appType: string;
  appScale: string;
  generated: boolean;
  createdAt: string;
}

interface PagedResponse {
  content: SystemConfig[];
  totalElements: number;
  totalPages: number;
  pageNumber: number;
}

const APP_TYPE_ICONS: Record<string, typeof Server> = {
  RIDE_HAILING: Zap,
  ECOMMERCE: Database,
  SAAS: Server,
  FINTECH: Shield,
  HEALTHCARE: Shield,
  SOCIAL_MEDIA: Activity,
  FOOD_DELIVERY: Zap,
  EDTECH: Brain,
  IOT_PLATFORM: Network,
  MARKETPLACE: Database,
  ENTERPRISE_INTERNAL: Server,
  CUSTOM: Brain,
};

function formatType(type: string): string {
  return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

function timeAgo(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - d.getTime();
  const minutes = Math.floor(diffMs / 60000);
  if (minutes < 1) return 'Just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 7) return `${days}d ago`;
  return d.toLocaleDateString();
}

export default function DashboardPage() {
  const [configs, setConfigs] = useState<SystemConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [deleting, setDeleting] = useState<string | null>(null);

  async function fetchConfigs() {
    try {
      const res = await api<PagedResponse>('/api/v1/systems/configs?page=0&size=20');
      setConfigs(res.data.content);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Failed to load projects.');
      }
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchConfigs(); }, []);

  async function handleDelete(configId: string) {
    if (!confirm('Delete this project? This cannot be undone.')) return;
    setDeleting(configId);
    try {
      await api(`/api/v1/systems/configs/${configId}`, { method: 'DELETE' });
      setConfigs(prev => prev.filter(c => c.id !== configId));
    } catch {
      // Silently fail — user sees it's still there
    } finally {
      setDeleting(null);
    }
  }

  return (
    <div className="p-8 max-w-7xl mx-auto w-full">
      <div className="flex justify-between items-end mb-12">
        <div>
          <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">My Projects</h1>
          <p className="text-[#dee1f7]/60 text-sm">Manage and monitor your generated system architectures.</p>
        </div>
        <Link href="/create" className="cta-gradient text-on-primary px-5 py-2.5 rounded-lg font-bold text-xs uppercase tracking-wider shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2">
          <Plus className="w-4 h-4" />
          New Project
        </Link>
      </div>

      {loading ? (
        <div className="flex items-center justify-center py-20">
          <Loader2 className="w-8 h-8 text-primary-container animate-spin" />
        </div>
      ) : error ? (
        <div className="glass-card rounded-xl p-12 text-center">
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <p className="text-sm text-red-400">{error}</p>
        </div>
      ) : configs.length === 0 ? (
        <div className="glass-card rounded-xl p-16 text-center">
          <Brain className="w-16 h-16 text-[#dee1f7]/20 mx-auto mb-6" />
          <h2 className="text-xl font-bold font-headline text-[#e1fdff] mb-3">No Projects Yet</h2>
          <p className="text-sm text-[#dee1f7]/60 mb-8 max-w-md mx-auto">
            Start by describing your application requirements and let our AI architect the optimal backend for you.
          </p>
          <Link href="/create" className="cta-gradient text-on-primary px-6 py-3 rounded-lg font-bold text-sm inline-flex items-center gap-2">
            <Plus className="w-4 h-4" /> Create Your First Project
          </Link>
        </div>
      ) : (
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60">
                {configs.length} Architecture{configs.length !== 1 ? 's' : ''}
              </h2>
            </div>

            <div className="grid sm:grid-cols-2 gap-4">
              {configs.map(config => {
                const Icon = APP_TYPE_ICONS[config.appType] || Brain;
                return (
                  <div key={config.id} className="glass-card rounded-xl p-6 hover:border-primary-container/40 transition-colors group cursor-pointer relative overflow-hidden">
                    <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary-container to-secondary-container opacity-0 group-hover:opacity-100 transition-opacity"></div>
                    <div className="flex justify-between items-start mb-4">
                      <div className="p-2 rounded-lg bg-primary-container/10 text-primary-container">
                        <Icon className="w-5 h-5" />
                      </div>
                      <span className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/40">{timeAgo(config.createdAt)}</span>
                    </div>
                    <h3 className="font-headline font-bold text-lg mb-2 text-[#e1fdff] group-hover:text-[#00f2ff] transition-colors">{config.configName}</h3>
                    <p className="text-sm text-[#dee1f7]/60 mb-4">
                      {formatType(config.appType)} · {config.appScale} scale
                    </p>
                    <div className="flex items-center justify-between">
                      <span className={`text-[10px] font-bold uppercase tracking-widest px-2 py-1 rounded-full ${
                        config.generated
                          ? 'bg-primary-container/10 text-primary-container border border-primary-container/20'
                          : 'bg-surface-container text-[#dee1f7]/40 border border-outline-variant/20'
                      }`}>
                        {config.generated ? '✓ Generated' : '◷ Pending'}
                      </span>
                      <div className="flex items-center gap-2">
                        <button
                          onClick={(e) => { e.stopPropagation(); handleDelete(config.id); }}
                          className="p-1.5 rounded text-[#dee1f7]/30 hover:text-red-400 hover:bg-red-500/10 transition-colors opacity-0 group-hover:opacity-100"
                          disabled={deleting === config.id}
                        >
                          {deleting === config.id ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Trash2 className="w-3.5 h-3.5" />}
                        </button>
                        <Link
                          href={config.generated ? `/result?configId=${config.id}` : `/processing?configId=${config.id}`}
                          className="text-xs font-bold text-primary-container flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          {config.generated ? 'View' : 'Generate'} <ArrowRight className="w-3 h-3" />
                        </Link>
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Sidebar stats */}
          <div className="space-y-6">
            <div className="glass-card rounded-xl p-6">
              <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4">Quick Stats</h3>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-sm text-[#dee1f7]/70">Total Projects</span>
                  <span className="text-lg font-bold text-primary-container">{configs.length}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-[#dee1f7]/70">Generated</span>
                  <span className="text-lg font-bold text-secondary-container">{configs.filter(c => c.generated).length}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-sm text-[#dee1f7]/70">Pending</span>
                  <span className="text-lg font-bold text-tertiary-container">{configs.filter(c => !c.generated).length}</span>
                </div>
              </div>
            </div>

            <div className="glass-card rounded-xl p-6">
              <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4">Quick Actions</h3>
              <div className="space-y-3">
                <Link href="/create" className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                  <div className="flex items-center gap-3">
                    <Plus className="w-4 h-4 text-primary-container" />
                    <span className="text-sm">New Architecture</span>
                  </div>
                  <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                </Link>
                <Link href="/chat" className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                  <div className="flex items-center gap-3">
                    <Brain className="w-4 h-4 text-secondary-container" />
                    <span className="text-sm">AI Chat</span>
                  </div>
                  <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
                </Link>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
