'use client';

import { useState, useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Brain, Database, Server, Zap, ArrowRight, Sparkles, Loader2, AlertCircle } from 'lucide-react';
import { api, ApiError } from '@/lib/api';

// ─── Constants matching backend enums ────────────────────────────────────────

const APP_TYPES = [
  { value: 'RIDE_HAILING',       label: 'Ride Hailing',       desc: 'Uber, Ola, Lyft style' },
  { value: 'ECOMMERCE',          label: 'E-Commerce',         desc: 'Online retail & storefront' },
  { value: 'SAAS',               label: 'SaaS',               desc: 'Software as a Service' },
  { value: 'FINTECH',            label: 'FinTech',            desc: 'Payments, banking, crypto' },
  { value: 'HEALTHCARE',         label: 'Healthcare',         desc: 'Telemedicine, EHR systems' },
  { value: 'SOCIAL_MEDIA',       label: 'Social Media',       desc: 'Community & content platform' },
  { value: 'FOOD_DELIVERY',      label: 'Food Delivery',      desc: 'Swiggy, Zomato, DoorDash' },
  { value: 'EDTECH',             label: 'EdTech',             desc: 'Learning & education' },
  { value: 'IOT_PLATFORM',       label: 'IoT Platform',       desc: 'Device management & telemetry' },
  { value: 'MARKETPLACE',        label: 'Marketplace',        desc: 'Multi-vendor platform' },
  { value: 'ENTERPRISE_INTERNAL',label: 'Enterprise Internal', desc: 'Internal tools & dashboards' },
  { value: 'CUSTOM',             label: 'Custom',             desc: 'Define your own' },
];

const APP_SCALES = [
  { value: 'SMALL',  label: 'Startup',    desc: '< 10k users' },
  { value: 'MEDIUM', label: 'Growth',     desc: '10k – 500k users' },
  { value: 'LARGE',  label: 'Enterprise', desc: '500k+ users' },
];

export default function CreatePage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [configName, setConfigName] = useState('');
  const [prompt, setPrompt] = useState('');
  const [appType, setAppType] = useState('ECOMMERCE');
  const [appScale, setAppScale] = useState('SMALL');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Pre-fill from template URL params
  useEffect(() => {
    const tName = searchParams.get('templateName');
    const tType = searchParams.get('appType');
    const tScale = searchParams.get('appScale');
    if (tName) setConfigName(tName);
    if (tType && APP_TYPES.some(t => t.value === tType)) setAppType(tType);
    if (tScale && APP_SCALES.some(s => s.value === tScale)) setAppScale(tScale);
  }, [searchParams]);

  const canSubmit = configName.trim().length >= 3 && prompt.trim().length > 0 && !submitting;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!canSubmit) return;

    setSubmitting(true);
    setError(null);

    try {
      const res = await api<{ id: string }>('/api/v1/systems/configs', {
        method: 'POST',
        body: {
          configName: configName.trim(),
          appType,
          appScale,
          selectedSystemsJson: JSON.stringify([{ prompt: prompt.trim() }]),
        },
      });

      const configId = res.data.id;
      router.push(`/processing?configId=${configId}`);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError('Something went wrong. Please try again.');
      }
      setSubmitting(false);
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto w-full h-full flex flex-col">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">Design Your System</h1>
        <p className="text-[#dee1f7]/60 text-sm">Describe your application requirements, and our AI will architect the optimal backend.</p>
      </div>

      {error && (
        <div className="mb-6 bg-red-500/10 border border-red-500/30 text-red-400 rounded-lg px-4 py-3 flex items-center gap-2 text-sm">
          <AlertCircle className="w-4 h-4 shrink-0" />
          {error}
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-8 flex-1 min-h-0">
        <div className="lg:col-span-2 flex flex-col h-full">
          <form onSubmit={handleSubmit} className="glass-card rounded-xl p-6 flex flex-col h-full">
            {/* Config Name */}
            <div className="mb-4">
              <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Project Name</label>
              <input
                type="text"
                value={configName}
                onChange={(e) => setConfigName(e.target.value)}
                placeholder="e.g., My Ride App - V1"
                required
                minLength={3}
                maxLength={150}
                className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/30"
              />
            </div>

            {/* Prompt */}
            <div className="mb-4">
              <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">System Requirements</label>
              <textarea
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="e.g., I need a backend for a ride-sharing app like Uber. It needs to handle real-time location tracking for 100k concurrent users, process payments, and have a matching algorithm for drivers and riders."
                className="w-full h-36 bg-surface-container-lowest border border-outline-variant/20 rounded-lg p-4 text-sm focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/30 resize-none custom-scrollbar"
                required
              />
            </div>

            {/* App Type + Scale */}
            <div className="grid sm:grid-cols-2 gap-6 mb-6">
              <div>
                <label className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Application Type</label>
                <select
                  value={appType}
                  onChange={(e) => setAppType(e.target.value)}
                  className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]"
                >
                  {APP_TYPES.map(t => (
                    <option key={t.value} value={t.value}>{t.label} — {t.desc}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Expected Scale</label>
                <select
                  value={appScale}
                  onChange={(e) => setAppScale(e.target.value)}
                  className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]"
                >
                  {APP_SCALES.map(s => (
                    <option key={s.value} value={s.value}>{s.label} ({s.desc})</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="mt-auto flex justify-end">
              <button
                type="submit"
                className="cta-gradient text-on-primary px-8 py-3.5 rounded-lg font-bold text-sm uppercase tracking-widest shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={!canSubmit}
              >
                {submitting ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    Creating...
                  </>
                ) : (
                  <>
                    <Sparkles className="w-4 h-4" />
                    Generate Architecture
                  </>
                )}
              </button>
            </div>
          </form>
        </div>

        <div className="space-y-6 flex flex-col h-full">
          {/* Live AI Analysis Sidebar */}
          <div className="glass-card rounded-xl p-6 flex-1">
            <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-6">Live AI Analysis</h3>

            {!prompt.trim() ? (
              <div className="h-full flex flex-col items-center justify-center text-center opacity-40">
                <Brain className="w-12 h-12 mb-4" />
                <p className="text-sm">Waiting for input...</p>
                <p className="text-xs mt-2">Start typing to see real-time suggestions.</p>
              </div>
            ) : (
              <div className="space-y-4 animate-in fade-in duration-500">
                <div className="p-3 rounded bg-surface-container border border-outline-variant/10">
                  <div className="flex items-center gap-2 mb-2 text-primary-container">
                    <Zap className="w-4 h-4" />
                    <span className="text-xs font-bold">Detected Pattern</span>
                  </div>
                  <p className="text-xs text-[#dee1f7]/70">
                    {APP_TYPES.find(t => t.value === appType)?.label || 'Custom'} architecture — {APP_SCALES.find(s => s.value === appScale)?.label} scale deployment.
                  </p>
                </div>
                <div className="p-3 rounded bg-surface-container border border-outline-variant/10">
                  <div className="flex items-center gap-2 mb-2 text-secondary-container">
                    <Database className="w-4 h-4" />
                    <span className="text-xs font-bold">Data Storage</span>
                  </div>
                  <p className="text-xs text-[#dee1f7]/70">
                    {appScale === 'LARGE' ? 'Multi-database strategy with read replicas and caching recommended.' :
                     appScale === 'MEDIUM' ? 'PostgreSQL with Redis cache layer recommended.' :
                     'Single PostgreSQL instance is sufficient for MVP.'}
                  </p>
                </div>
                <div className="p-3 rounded bg-surface-container border border-outline-variant/10">
                  <div className="flex items-center gap-2 mb-2 text-tertiary-container">
                    <Server className="w-4 h-4" />
                    <span className="text-xs font-bold">Infrastructure</span>
                  </div>
                  <p className="text-xs text-[#dee1f7]/70">
                    {appScale === 'LARGE' ? 'Kubernetes cluster recommended for independent scaling.' :
                     appScale === 'MEDIUM' ? 'Docker Compose or managed container service.' :
                     'Single server deployment with Docker is cost-effective.'}
                  </p>
                </div>
              </div>
            )}
          </div>

          <div className="glass-card rounded-xl p-6">
            <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4">Pro Tips</h3>
            <ul className="text-xs text-[#dee1f7]/70 space-y-3">
              <li className="flex gap-2"><ArrowRight className="w-3 h-3 text-primary-container shrink-0 mt-0.5" /> Be specific about read vs. write ratios.</li>
              <li className="flex gap-2"><ArrowRight className="w-3 h-3 text-primary-container shrink-0 mt-0.5" /> Mention any strict latency requirements.</li>
              <li className="flex gap-2"><ArrowRight className="w-3 h-3 text-primary-container shrink-0 mt-0.5" /> Specify if you have a preferred tech stack.</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
