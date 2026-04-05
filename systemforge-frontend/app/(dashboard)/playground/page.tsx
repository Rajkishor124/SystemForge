'use client';

import { useState, useEffect, useCallback } from 'react';
import { Cpu, ChevronRight, Loader2, ToggleLeft, ToggleRight, Layers, Code2, Network, Sparkles, Copy, Check, Server, Shield, Database, Zap, Box, Radio, History, Download } from 'lucide-react';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { motion, AnimatePresence } from 'motion/react';
import { toast } from 'sonner';
import { api } from '@/lib/api';

// ─── Types ───────────────────────────────────────────────────────────────────

type ServiceType = 'AUTH' | 'PAYMENT' | 'NOTIFICATION' | 'DATABASE' | 'STORAGE' | 'MESSAGING';
type ServiceVariant = 'JWT' | 'OAUTH2' | 'OTP' | 'SESSION' | 'RAZORPAY' | 'STRIPE' | 'UPI';
type FeatureToggle = 'REFRESH_TOKEN' | 'RBAC' | 'TWO_FACTOR_AUTH' | 'RATE_LIMITING' | 'AUDIT_LOGGING' | 'IP_WHITELISTING';

interface CodeSections {
  controllerCode: string;
  serviceCode: string;
  configCode: string;
  securityCode: string;
}

interface PreviewData {
  generatedCode: CodeSections;
  architectureSteps: string[];
  components: string[];
  techStack: string[];
}

interface PlaygroundOutput {
  id?: string;
  createdAt?: string;
  serviceType: ServiceType;
  variant: ServiceVariant;
  appliedFeatures: FeatureToggle[];
  preview: PreviewData;
}

// ─── Icons & Labels ──────────────────────────────────────────────────────────

const SERVICE_META: Record<ServiceType, { icon: any; label: string; color: string }> = {
  AUTH:          { icon: Shield,   label: 'Authentication',    color: '#00f2ff' },
  PAYMENT:      { icon: Zap,      label: 'Payment',           color: '#f59e0b' },
  NOTIFICATION: { icon: Radio,    label: 'Notification',      color: '#8b5cf6' },
  DATABASE:     { icon: Database, label: 'Database',           color: '#10b981' },
  STORAGE:      { icon: Box,      label: 'Storage',            color: '#ec4899' },
  MESSAGING:    { icon: Server,   label: 'Messaging',          color: '#3b82f6' },
};

const FEATURE_LABELS: Record<FeatureToggle, string> = {
  REFRESH_TOKEN: 'Refresh Tokens',
  RBAC: 'Role-Based Access Control',
  TWO_FACTOR_AUTH: 'Two-Factor Auth',
  RATE_LIMITING: 'Rate Limiting',
  AUDIT_LOGGING: 'Audit Logging',
  IP_WHITELISTING: 'IP Whitelisting',
};

const CODE_TABS = [
  { key: 'controllerCode' as const, label: 'Controller', file: 'AuthController.java' },
  { key: 'serviceCode' as const,    label: 'Service',    file: 'AuthService.java' },
  { key: 'configCode' as const,     label: 'Config',     file: 'JwtConfig.java' },
  { key: 'securityCode' as const,   label: 'Security',   file: 'SecurityConfig.java' },
];

// ─── Copy Button ─────────────────────────────────────────────────────────────

function CopyButton({ text }: { text: string }) {
  const [copied, setCopied] = useState(false);
  const handleCopy = () => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };
  return (
    <button onClick={handleCopy} className="p-1.5 rounded-md bg-white/5 hover:bg-white/10 text-[#dee1f7]/40 hover:text-[#dee1f7]/80 transition-all" title="Copy code">
      {copied ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
    </button>
  );
}

// ─── Main Component ──────────────────────────────────────────────────────────

export default function PlaygroundPage() {
  // Config state
  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([]);
  const [variants, setVariants] = useState<ServiceVariant[]>([]);
  const [features, setFeatures] = useState<FeatureToggle[]>([]);
  const [selectedType, setSelectedType] = useState<ServiceType | null>(null);
  const [selectedVariant, setSelectedVariant] = useState<ServiceVariant | null>(null);
  const [enabledFeatures, setEnabledFeatures] = useState<Set<FeatureToggle>>(new Set());

  // Output state
  const [output, setOutput] = useState<PlaygroundOutput | null>(null);
  const [generating, setGenerating] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [activeCodeTab, setActiveCodeTab] = useState<keyof CodeSections>('controllerCode');

  // History state
  const [history, setHistory] = useState<PlaygroundOutput[]>([]);
  const [showHistory, setShowHistory] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);

  // Loading states
  const [loadingTypes, setLoadingTypes] = useState(true);
  const [loadingVariants, setLoadingVariants] = useState(false);
  const [loadingFeatures, setLoadingFeatures] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const res = await api<ServiceType[]>('/api/v1/playground/services');
        setServiceTypes(res.data);
      } catch (err: any) {
        toast.error(err.message || 'Failed to load service types');
      }
      finally { setLoadingTypes(false); }
    })();

    fetchHistory();
  }, []);

  const fetchHistory = async () => {
    setLoadingHistory(true);
    try {
      const res = await api<PlaygroundOutput[]>('/api/v1/playground/history');
      setHistory(res.data || []);
    } catch (err: any) {
      toast.error(err.message || 'Failed to load history');
    } finally {
      setLoadingHistory(false);
    }
  };

  const loadHistoricalConfig = async (item: PlaygroundOutput) => {
    setSelectedType(item.serviceType);
    setSelectedVariant(item.variant);
    
    // Optimistically update features for the UI
    const featuresSet = new Set<FeatureToggle>(item.appliedFeatures);
    setEnabledFeatures(featuresSet);
    
    // Also load variants and features for these selections
    try {
      const varRes = await api<ServiceVariant[]>(`/api/v1/playground/services/${item.serviceType}/variants`);
      setVariants(varRes.data);
      const featRes = await api<FeatureToggle[]>(`/api/v1/playground/services/${item.serviceType}/variants/${item.variant}/features`);
      setFeatures(featRes.data);
    } catch {}

    setOutput(item);
    setShowHistory(false);
    toast.success('Restored architecture from history');
  };

  // ─── Load variants when type changes ─────────────────────────────────────

  const handleSelectType = useCallback(async (type: ServiceType) => {
    setSelectedType(type);
    setSelectedVariant(null);
    setVariants([]);
    setFeatures([]);
    setEnabledFeatures(new Set());
    setOutput(null);
    setLoadingVariants(true);
    try {
      const res = await api<ServiceVariant[]>(`/api/v1/playground/services/${type}/variants`);
      setVariants(res.data);
    } catch (err: any) {
      toast.error(err.message || 'Failed to load variants');
    }
    finally { setLoadingVariants(false); }
  }, []);

  // ─── Load features when variant changes ──────────────────────────────────

  const handleSelectVariant = useCallback(async (variant: ServiceVariant) => {
    if (!selectedType) return;
    setSelectedVariant(variant);
    setEnabledFeatures(new Set());
    setOutput(null);
    setLoadingFeatures(true);
    try {
      const res = await api<FeatureToggle[]>(`/api/v1/playground/services/${selectedType}/variants/${variant}/features`);
      setFeatures(res.data);
    } catch (err: any) {
      toast.error(err.message || 'Failed to load feature toggles');
    }
    finally { setLoadingFeatures(false); }
  }, [selectedType]);

  // ─── Toggle feature ──────────────────────────────────────────────────────

  const toggleFeature = (f: FeatureToggle) => {
    setEnabledFeatures(prev => {
      const next = new Set(prev);
      if (next.has(f)) next.delete(f); else next.add(f);
      return next;
    });
  };

  // ─── Generate ────────────────────────────────────────────────────────────

  const handleGenerate = async () => {
    if (!selectedType || !selectedVariant || generating) return;
    setGenerating(true);
    try {
      const res = await api<PlaygroundOutput>('/api/v1/playground/generate', {
        method: 'POST',
        body: {
          serviceType: selectedType,
          variant: selectedVariant,
          features: Array.from(enabledFeatures),
        },
      });
      setOutput(res.data);
      toast.success('Architecture generated successfully');
      fetchHistory(); // Refresh history
    } catch (err: any) {
      toast.error(err.message || 'Generation failed');
    }
    finally { setGenerating(false); }
  };

  // ─── Export ──────────────────────────────────────────────────────────────

  const handleExport = async () => {
    if (!output || exporting) return;
    setExporting(true);
    try {
      const response = await fetch('/api/v1/playground/export', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          serviceType: output.serviceType,
          variant: output.variant,
          features: output.appliedFeatures,
        }),
      });

      if (!response.ok) throw new Error('Export failed');

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `systemforge-${output.serviceType.toLowerCase()}-${output.variant.toLowerCase()}.zip`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      toast.success('Project downloaded successfully');
    } catch (err: any) {
      toast.error(err.message || 'Export failed');
    } finally {
      setExporting(false);
    }
  };

  // ─── Render ──────────────────────────────────────────────────────────────

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] overflow-hidden">
      {/* Header */}
      <div className="p-6 pb-4 shrink-0">
        <div className="max-w-[1600px] mx-auto flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold tracking-tight font-headline text-[#e1fdff] mb-1 flex items-center gap-3">
              <div className="p-2 rounded-xl bg-primary-container/10 border border-primary-container/20">
                <Cpu className="w-5 h-5 text-primary-container" />
              </div>
              Microservices Playground
            </h1>
            <p className="text-[#dee1f7]/50 text-xs pl-[52px]">Configure, generate, and explore microservice architectures in real time.</p>
          </div>
          <button
            onClick={() => setShowHistory(!showHistory)}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-all border flex items-center gap-2 ${
              showHistory
                ? 'bg-primary-container/20 border-primary-container/40 text-primary-container'
                : 'bg-surface-container border-outline-variant/20 hover:border-outline-variant/40 text-[#dee1f7]/70 hover:text-[#dee1f7]'
            }`}
          >
            <History className="w-4 h-4" /> History ({history.length})
          </button>
        </div>
      </div>

      {/* 3-Panel Layout (or 4-panel with History) */}
      <div className="flex-1 flex gap-4 px-6 pb-6 min-h-0 max-w-[1600px] mx-auto w-full">

        {/* ────── HISTORY PANEL ────── */}
        <AnimatePresence>
          {showHistory && (
            <motion.div
              initial={{ opacity: 0, width: 0, margin: 0 }}
              animate={{ opacity: 1, width: 280, marginRight: '1rem' }}
              exit={{ opacity: 0, width: 0, margin: 0 }}
              className="shrink-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden min-h-0"
            >
              <div className="p-4 border-b border-outline-variant/10 bg-surface-container-lowest flex justify-between items-center">
                <h2 className="text-xs font-black uppercase tracking-widest text-[#dee1f7]/50 flex items-center gap-2">
                  <History className="w-3.5 h-3.5 text-primary-container" /> History
                </h2>
              </div>
              <div className="flex-1 overflow-y-auto custom-scrollbar p-3 space-y-2">
                {loadingHistory ? (
                  <div className="flex justify-center p-8"><Loader2 className="w-5 h-5 text-primary-container animate-spin" /></div>
                ) : history.length === 0 ? (
                  <div className="text-center p-6 text-xs text-[#dee1f7]/40">No generations yet.</div>
                ) : (
                  history.map((item) => (
                    <button
                      key={item.id}
                      onClick={() => loadHistoricalConfig(item)}
                      className="w-full text-left p-3 rounded-xl border border-outline-variant/10 hover:border-primary-container/30 hover:bg-surface-container transition-all group"
                    >
                      <div className="flex justify-between items-start mb-2">
                        <span className="text-[10px] font-black uppercase tracking-widest text-primary-container">
                          {item.serviceType}
                        </span>
                        <span className="text-[9px] text-[#dee1f7]/40">
                          {new Date(item.createdAt || '').toLocaleDateString()}
                        </span>
                      </div>
                      <div className="text-xs font-bold text-[#e1fdff] mb-1">{item.variant}</div>
                      <div className="flex flex-wrap gap-1">
                        {item.appliedFeatures.slice(0, 2).map((f) => (
                          <span key={f} className="text-[9px] text-[#dee1f7]/60 bg-white/5 px-1.5 py-0.5 rounded">
                            {f.replace(/_/g, ' ')}
                          </span>
                        ))}
                        {item.appliedFeatures.length > 2 && (
                          <span className="text-[9px] text-[#dee1f7]/40">+{item.appliedFeatures.length - 2}</span>
                        )}
                      </div>
                    </button>
                  ))
                )}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* ────── LEFT: Config Builder ────── */}
        <div className="w-80 shrink-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden min-h-0">
          <div className="p-4 border-b border-outline-variant/10 bg-surface-container-lowest">
            <h2 className="text-xs font-black uppercase tracking-widest text-[#dee1f7]/50 flex items-center gap-2">
              <Layers className="w-3.5 h-3.5 text-primary-container" /> Configuration
            </h2>
          </div>

          <div className="flex-1 overflow-y-auto custom-scrollbar p-4 space-y-6">
            {/* Service Type */}
            <div>
              <label className="text-[10px] font-bold uppercase tracking-widest text-[#dee1f7]/40 mb-3 block">1. Service Type</label>
              {loadingTypes ? (
                <div className="flex justify-center py-6"><Loader2 className="w-5 h-5 text-primary-container animate-spin" /></div>
              ) : (
                <div className="grid grid-cols-2 gap-2">
                  {serviceTypes.map(type => {
                    const meta = SERVICE_META[type];
                    const Icon = meta?.icon || Server;
                    const isActive = selectedType === type;
                    return (
                      <button
                        key={type}
                        onClick={() => handleSelectType(type)}
                        className={`flex flex-col items-center gap-1.5 p-3 rounded-xl border text-center transition-all ${
                          isActive
                            ? 'bg-primary-container/10 border-primary-container/30 shadow-[0_0_12px_rgba(0,242,255,0.1)]'
                            : 'border-outline-variant/10 hover:border-outline-variant/30 hover:bg-surface-container'
                        }`}
                      >
                        <Icon className="w-4 h-4" style={{ color: isActive ? (meta?.color || '#00f2ff') : '#dee1f780' }} />
                        <span className={`text-[10px] font-bold uppercase tracking-wider ${isActive ? 'text-[#e1fdff]' : 'text-[#dee1f7]/60'}`}>
                          {meta?.label || type}
                        </span>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Variant */}
            <AnimatePresence>
              {selectedType && (
                <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                  <label className="text-[10px] font-bold uppercase tracking-widest text-[#dee1f7]/40 mb-3 block">2. Variant</label>
                  {loadingVariants ? (
                    <div className="flex justify-center py-4"><Loader2 className="w-4 h-4 text-primary-container animate-spin" /></div>
                  ) : (
                    <div className="space-y-1.5">
                      {variants.map(v => (
                        <button
                          key={v}
                          onClick={() => handleSelectVariant(v)}
                          className={`w-full text-left px-3 py-2.5 rounded-lg border flex items-center gap-2 transition-all ${
                            selectedVariant === v
                              ? 'bg-primary-container/10 border-primary-container/30 text-[#e1fdff]'
                              : 'border-outline-variant/10 text-[#dee1f7]/60 hover:bg-surface-container hover:text-[#dee1f7]'
                          }`}
                        >
                          <ChevronRight className={`w-3.5 h-3.5 transition-transform ${selectedVariant === v ? 'rotate-90 text-primary-container' : ''}`} />
                          <span className="text-xs font-bold">{v}</span>
                        </button>
                      ))}
                    </div>
                  )}
                </motion.div>
              )}
            </AnimatePresence>

            {/* Features */}
            <AnimatePresence>
              {selectedVariant && (
                <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0 }}>
                  <label className="text-[10px] font-bold uppercase tracking-widest text-[#dee1f7]/40 mb-3 block">3. Feature Toggles</label>
                  {loadingFeatures ? (
                    <div className="flex justify-center py-4"><Loader2 className="w-4 h-4 text-primary-container animate-spin" /></div>
                  ) : (
                    <div className="space-y-1.5">
                      {features.map(f => {
                        const on = enabledFeatures.has(f);
                        return (
                          <button
                            key={f}
                            onClick={() => toggleFeature(f)}
                            className={`w-full text-left px-3 py-2.5 rounded-lg border flex items-center justify-between transition-all ${
                              on
                                ? 'bg-primary-container/10 border-primary-container/30'
                                : 'border-outline-variant/10 hover:bg-surface-container'
                            }`}
                          >
                            <span className={`text-xs font-bold ${on ? 'text-[#e1fdff]' : 'text-[#dee1f7]/60'}`}>
                              {FEATURE_LABELS[f] || f}
                            </span>
                            {on
                              ? <ToggleRight className="w-5 h-5 text-primary-container" />
                              : <ToggleLeft className="w-5 h-5 text-[#dee1f7]/30" />
                            }
                          </button>
                        );
                      })}
                    </div>
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </div>

          {/* Generate Button */}
          <div className="p-4 border-t border-outline-variant/10 bg-surface-container-lowest shrink-0">
            <button
              onClick={handleGenerate}
              disabled={!selectedType || !selectedVariant || generating}
              className="w-full py-3 rounded-xl font-bold text-sm flex items-center justify-center gap-2 transition-all disabled:opacity-40 disabled:cursor-not-allowed bg-gradient-to-r from-[#00f2ff] to-[#0566d9] text-[#0a0e1a] hover:brightness-110 active:scale-[0.98] shadow-[0_0_20px_rgba(0,242,255,0.2)]"
            >
              {generating ? <Loader2 className="w-4 h-4 animate-spin" /> : <Sparkles className="w-4 h-4" />}
              {generating ? 'Generating...' : 'Generate Architecture'}
            </button>
          </div>
        </div>

        {/* ────── CENTER: Architecture View ────── */}
        <div className="flex-1 min-w-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden">
          <div className="p-4 border-b border-outline-variant/10 bg-surface-container-lowest shrink-0 flex items-center justify-between">
            <h2 className="text-xs font-black uppercase tracking-widest text-[#dee1f7]/50 flex items-center gap-2">
              <Network className="w-3.5 h-3.5 text-primary-container" /> Architecture
            </h2>
            {output && (
              <button
                onClick={handleExport}
                disabled={exporting}
                className="px-3 py-1.5 rounded-lg border border-primary-container/30 bg-primary-container/10 text-primary-container text-xs font-bold flex items-center gap-2 hover:bg-primary-container/20 transition-all disabled:opacity-50"
              >
                {exporting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Download className="w-3.5 h-3.5" />}
                {exporting ? 'Zipping...' : 'Download Project (.zip)'}
              </button>
            )}
          </div>

          <div className="flex-1 overflow-y-auto custom-scrollbar p-5">
            {!output ? (
              <div className="flex flex-col items-center justify-center h-full text-center px-4">
                <div className="relative mb-5">
                  <div className="absolute inset-0 bg-[#00f2ff]/10 blur-2xl rounded-full" />
                  <Network className="w-12 h-12 text-[#00f2ff]/30 relative z-10" />
                </div>
                <h3 className="text-lg font-bold font-headline text-[#e1fdff] mb-2">No Architecture Generated</h3>
                <p className="text-xs text-[#dee1f7]/40 max-w-xs leading-relaxed">
                  Select a service type, variant, and features from the left panel, then click &quot;Generate&quot; to see the architecture.
                </p>
              </div>
            ) : (
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="space-y-6">
                {/* Service Badge */}
                <div className="flex items-center gap-3">
                  <div className="px-3 py-1.5 rounded-full bg-primary-container/10 border border-primary-container/20 text-[10px] font-black uppercase tracking-widest text-primary-container">
                    {output.serviceType} · {output.variant}
                  </div>
                  {output.appliedFeatures.map(f => (
                    <span key={f} className="px-2.5 py-1 rounded-full bg-surface-container-high text-[9px] font-bold uppercase tracking-wider text-[#dee1f7]/60 border border-outline-variant/10">
                      {f.replace(/_/g, ' ')}
                    </span>
                  ))}
                </div>

                {/* Architecture Description */}
                <div>
                  <h4 className="text-[10px] font-black uppercase tracking-widest text-[#dee1f7]/30 mb-3 flex items-center gap-2">
                    <span className="w-6 h-px bg-outline-variant/20" /> Architecture Flow <span className="flex-1 h-px bg-outline-variant/20" />
                  </h4>
                  <div className="space-y-2">
                    {output.preview.architectureSteps.map((step, i) => (
                      <div key={i} className="p-3 rounded-lg bg-surface-container-lowest border border-outline-variant/10 text-xs text-[#dee1f7]/80 leading-relaxed whitespace-pre-wrap font-mono">
                        {step}
                      </div>
                    ))}
                  </div>
                </div>

                {/* Components */}
                <div>
                  <h4 className="text-[10px] font-black uppercase tracking-widest text-[#dee1f7]/30 mb-3 flex items-center gap-2">
                    <span className="w-6 h-px bg-outline-variant/20" /> Components <span className="flex-1 h-px bg-outline-variant/20" />
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {output.preview.components.map((c, i) => (
                      <span key={i} className="px-3 py-1.5 rounded-lg bg-surface-container-high border border-outline-variant/10 text-[11px] font-bold text-[#dee1f7]/70">
                        {c}
                      </span>
                    ))}
                  </div>
                </div>

                {/* Tech Stack */}
                <div>
                  <h4 className="text-[10px] font-black uppercase tracking-widest text-[#dee1f7]/30 mb-3 flex items-center gap-2">
                    <span className="w-6 h-px bg-outline-variant/20" /> Recommended Stack <span className="flex-1 h-px bg-outline-variant/20" />
                  </h4>
                  <div className="grid grid-cols-2 gap-2">
                    {output.preview.techStack.map((t, i) => (
                      <div key={i} className="flex items-center gap-2 p-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/10">
                        <div className="w-1.5 h-1.5 rounded-full bg-green-400 shadow-[0_0_6px_rgba(74,222,128,0.5)]" />
                        <span className="text-[11px] font-bold text-[#dee1f7]/70">{t}</span>
                      </div>
                    ))}
                  </div>
                </div>
              </motion.div>
            )}
          </div>
        </div>

        {/* ────── RIGHT: Code Preview ────── */}
        <div className="w-[420px] shrink-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden min-h-0">
          <div className="p-4 border-b border-outline-variant/10 bg-surface-container-lowest shrink-0">
            <h2 className="text-xs font-black uppercase tracking-widest text-[#dee1f7]/50 flex items-center gap-2">
              <Code2 className="w-3.5 h-3.5 text-primary-container" /> Generated Code
            </h2>
          </div>

          {/* Code Tabs */}
          <div className="flex border-b border-outline-variant/10 bg-surface-container-lowest shrink-0">
            {CODE_TABS.map(tab => (
              <button
                key={tab.key}
                onClick={() => setActiveCodeTab(tab.key)}
                className={`flex-1 py-2.5 text-[10px] font-bold uppercase tracking-wider transition-all border-b-2 ${
                  activeCodeTab === tab.key
                    ? 'text-primary-container border-primary-container bg-primary-container/5'
                    : 'text-[#dee1f7]/40 border-transparent hover:text-[#dee1f7]/60 hover:bg-surface-container'
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          <div className="flex-1 overflow-y-auto custom-scrollbar min-h-0">
            {!output ? (
              <div className="flex flex-col items-center justify-center h-full text-center px-4">
                <Code2 className="w-10 h-10 text-[#dee1f7]/15 mb-3" />
                <p className="text-xs text-[#dee1f7]/30">Code will appear here after generation.</p>
              </div>
            ) : (
              <div className="relative">
                <div className="absolute top-2 right-2 z-10">
                  <CopyButton text={output.preview.generatedCode[activeCodeTab] || ''} />
                </div>
                <SyntaxHighlighter
                  language="java"
                  style={vscDarkPlus}
                  customStyle={{ margin: 0, background: 'transparent', padding: '1rem', fontSize: '0.75rem', lineHeight: '1.6' }}
                  showLineNumbers
                  lineNumberStyle={{ color: '#dee1f715', fontSize: '0.65rem' }}
                >
                  {output.preview.generatedCode[activeCodeTab] || '// No code generated for this section'}
                </SyntaxHighlighter>
              </div>
            )}
          </div>

          {/* File name footer */}
          {output && (
            <div className="p-2.5 border-t border-outline-variant/10 bg-surface-container-lowest shrink-0">
              <span className="text-[10px] font-mono text-[#dee1f7]/30">
                {CODE_TABS.find(t => t.key === activeCodeTab)?.file || 'Code.java'}
              </span>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
