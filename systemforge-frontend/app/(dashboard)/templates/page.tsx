'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Brain, Database, Server, Shield, Zap, Search, Loader2, AlertCircle, Tag, ArrowRight, DollarSign, Layers, Cpu } from 'lucide-react';
import { api, ApiError } from '@/lib/api';

// ─── Types ───────────────────────────────────────────────────────────────────

interface TemplateConfig {
  techStack: string[];
  modules: string[];
  architecture: string;
  deployment: string;
  estimatedCost: string;
}

interface Template {
  id: string;
  name: string;
  description: string;
  appType: string;
  systemType: string;
  appScale: string;
  configJson: string;
  active: boolean;
  sortOrder: number;
}

// ─── Constants ───────────────────────────────────────────────────────────────

const APP_TYPE_OPTIONS = [
  { value: '', label: 'All Types' },
  { value: 'ECOMMERCE', label: 'E-Commerce' },
  { value: 'RIDE_HAILING', label: 'Ride Hailing' },
  { value: 'SAAS', label: 'SaaS' },
  { value: 'FINTECH', label: 'FinTech' },
  { value: 'FOOD_DELIVERY', label: 'Food Delivery' },
  { value: 'SOCIAL_MEDIA', label: 'Social Media' },
  { value: 'EDTECH', label: 'EdTech' },
  { value: 'HEALTHCARE', label: 'Healthcare' },
  { value: 'IOT_PLATFORM', label: 'IoT Platform' },
  { value: 'MARKETPLACE', label: 'Marketplace' },
  { value: 'ENTERPRISE_INTERNAL', label: 'Enterprise' },
  { value: 'CUSTOM', label: 'Custom' },
];

const SCALE_OPTIONS = [
  { value: '', label: 'All Scales' },
  { value: 'SMALL', label: 'Startup' },
  { value: 'MEDIUM', label: 'Growth' },
  { value: 'LARGE', label: 'Enterprise' },
];

const APP_TYPE_ICONS: Record<string, typeof Server> = {
  RIDE_HAILING: Zap,
  ECOMMERCE: Database,
  SAAS: Server,
  FINTECH: Shield,
  HEALTHCARE: Shield,
  SOCIAL_MEDIA: Zap,
  FOOD_DELIVERY: Zap,
  EDTECH: Brain,
  IOT_PLATFORM: Cpu,
  MARKETPLACE: Database,
  ENTERPRISE_INTERNAL: Server,
  CUSTOM: Brain,
};

const SCALE_COLORS: Record<string, string> = {
  SMALL: 'text-green-400 bg-green-400/10 border-green-400/20',
  MEDIUM: 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20',
  LARGE: 'text-red-400 bg-red-400/10 border-red-400/20',
};

function formatType(type: string): string {
  return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
}

export default function TemplatesPage() {
  const router = useRouter();
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [filterType, setFilterType] = useState('');
  const [filterScale, setFilterScale] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedId, setSelectedId] = useState<string | null>(null);

  async function fetchTemplates() {
    setLoading(true);
    setError(null);
    try {
      let path = '/api/v1/templates';
      const params = new URLSearchParams();
      if (filterType) params.append('appType', filterType);
      if (filterScale) params.append('appScale', filterScale);
      if (params.toString()) path += '?' + params.toString();

      const res = await api<Template[]>(path);
      setTemplates(res.data);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load templates.');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { fetchTemplates(); }, [filterType, filterScale]);

  const filteredTemplates = templates.filter(t =>
    searchQuery === '' ||
    t.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
    t.description.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const selectedTemplate = selectedId ? templates.find(t => t.id === selectedId) : null;
  const selectedConfig: TemplateConfig | null = selectedTemplate?.configJson
    ? (() => { try { return JSON.parse(selectedTemplate.configJson); } catch { return null; } })()
    : null;

  const handleUseTemplate = (template: Template) => {
    const config: TemplateConfig | null = (() => { try { return JSON.parse(template.configJson); } catch { return null; } })();
    const params = new URLSearchParams({
      templateName: template.name,
      appType: template.appType,
      appScale: template.appScale || 'SMALL',
    });
    router.push(`/create?${params.toString()}`);
  };

  return (
    <div className="p-8 max-w-7xl mx-auto w-full">
      {/* Header */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">Template Library</h1>
        <p className="text-[#dee1f7]/60 text-sm">Start with a production-grade architecture blueprint. Choose a template and customize it for your project.</p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4 mb-8">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50" />
          <input
            type="text"
            placeholder="Search templates..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 pl-10 pr-3 text-sm focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
          />
        </div>
        <select
          value={filterType}
          onChange={(e) => setFilterType(e.target.value)}
          className="bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7] min-w-[160px]"
        >
          {APP_TYPE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        <select
          value={filterScale}
          onChange={(e) => setFilterScale(e.target.value)}
          className="bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7] min-w-[140px]"
        >
          {SCALE_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
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
      ) : filteredTemplates.length === 0 ? (
        <div className="glass-card rounded-xl p-16 text-center">
          <Layers className="w-16 h-16 text-[#dee1f7]/20 mx-auto mb-6" />
          <h2 className="text-xl font-bold font-headline text-[#e1fdff] mb-3">No Templates Found</h2>
          <p className="text-sm text-[#dee1f7]/60">Try adjusting your filters or search query.</p>
        </div>
      ) : (
        <div className="grid lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <div className="grid sm:grid-cols-2 gap-4">
              {filteredTemplates.map(template => {
                const Icon = APP_TYPE_ICONS[template.appType] || Brain;
                const scaleClass = SCALE_COLORS[template.appScale] || 'text-[#dee1f7]/40 bg-surface-container border-outline-variant/20';
                const config: TemplateConfig | null = (() => { try { return JSON.parse(template.configJson); } catch { return null; } })();

                return (
                  <div
                    key={template.id}
                    onClick={() => setSelectedId(template.id === selectedId ? null : template.id)}
                    className={`glass-card rounded-xl p-6 cursor-pointer transition-all group relative overflow-hidden ${
                      selectedId === template.id
                        ? 'border-primary-container/50 ring-1 ring-primary-container/30'
                        : 'hover:border-primary-container/30'
                    }`}
                  >
                    <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary-container to-secondary-container opacity-0 group-hover:opacity-100 transition-opacity"></div>

                    <div className="flex justify-between items-start mb-3">
                      <div className="p-2 rounded-lg bg-primary-container/10 text-primary-container">
                        <Icon className="w-5 h-5" />
                      </div>
                      <span className={`text-[10px] font-bold uppercase tracking-widest px-2 py-0.5 rounded-full border ${scaleClass}`}>
                        {template.appScale || 'Any'}
                      </span>
                    </div>

                    <h3 className="font-headline font-bold text-base mb-2 text-[#e1fdff] group-hover:text-[#00f2ff] transition-colors">{template.name}</h3>
                    <p className="text-xs text-[#dee1f7]/60 mb-4 line-clamp-2">{template.description}</p>

                    {config && (
                      <div className="flex flex-wrap gap-1.5 mb-4">
                        {config.techStack.slice(0, 4).map((tech, i) => (
                          <span key={i} className="px-2 py-0.5 rounded bg-surface-container text-[10px] font-mono text-[#dee1f7]/60">
                            {tech}
                          </span>
                        ))}
                        {config.techStack.length > 4 && (
                          <span className="px-2 py-0.5 rounded bg-surface-container text-[10px] font-mono text-primary-container">
                            +{config.techStack.length - 4}
                          </span>
                        )}
                      </div>
                    )}

                    <div className="flex items-center justify-between">
                      <span className="text-[10px] text-[#dee1f7]/40 uppercase tracking-widest">
                        {formatType(template.appType)}
                      </span>
                      <button
                        onClick={(e) => { e.stopPropagation(); handleUseTemplate(template); }}
                        className="text-xs font-bold text-primary-container flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity"
                      >
                        Use Template <ArrowRight className="w-3 h-3" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Detail Sidebar */}
          <div className="space-y-6">
            {selectedTemplate && selectedConfig ? (
              <>
                <div className="glass-card rounded-xl p-6">
                  <h3 className="font-headline font-bold text-lg text-[#e1fdff] mb-3">{selectedTemplate.name}</h3>
                  <p className="text-sm text-[#dee1f7]/70 mb-6">{selectedTemplate.description}</p>
                  <button
                    onClick={() => handleUseTemplate(selectedTemplate)}
                    className="w-full cta-gradient text-on-primary py-3 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center justify-center gap-2"
                  >
                    <Zap className="w-4 h-4" /> Use This Template
                  </button>
                </div>

                <div className="glass-card rounded-xl p-6">
                  <h4 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4 flex items-center gap-2">
                    <Layers className="w-4 h-4 text-primary-container" />
                    Architecture
                  </h4>
                  <div className="space-y-3 text-sm">
                    <div className="flex justify-between">
                      <span className="text-[#dee1f7]/60">Pattern</span>
                      <span className="font-bold text-[#e1fdff]">{selectedConfig.architecture}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#dee1f7]/60">Deployment</span>
                      <span className="font-bold text-[#e1fdff]">{selectedConfig.deployment}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-[#dee1f7]/60">Scale</span>
                      <span className="font-bold text-[#e1fdff]">{formatType(selectedTemplate.appScale || 'ANY')}</span>
                    </div>
                  </div>
                </div>

                <div className="glass-card rounded-xl p-6">
                  <h4 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4 flex items-center gap-2">
                    <Tag className="w-4 h-4 text-secondary-container" />
                    Tech Stack
                  </h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedConfig.techStack.map((tech, i) => (
                      <span key={i} className="px-3 py-1 rounded-full bg-surface-container border border-outline-variant/20 text-xs font-mono text-[#dee1f7]/80">
                        {tech}
                      </span>
                    ))}
                  </div>
                </div>

                <div className="glass-card rounded-xl p-6">
                  <h4 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4 flex items-center gap-2">
                    <Server className="w-4 h-4 text-tertiary-container" />
                    Modules ({selectedConfig.modules.length})
                  </h4>
                  <div className="space-y-2">
                    {selectedConfig.modules.map((mod, i) => (
                      <div key={i} className="flex items-center gap-2 text-xs text-[#dee1f7]/70">
                        <div className="w-1.5 h-1.5 rounded-full bg-primary-container"></div>
                        {mod}
                      </div>
                    ))}
                  </div>
                </div>

                {selectedConfig.estimatedCost && (
                  <div className="glass-card rounded-xl p-6">
                    <h4 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-3 flex items-center gap-2">
                      <DollarSign className="w-4 h-4 text-green-400" />
                      Estimated Cost
                    </h4>
                    <p className="text-lg font-bold text-green-400">{selectedConfig.estimatedCost}</p>
                    <p className="text-[10px] text-[#dee1f7]/40 mt-1">Cloud infrastructure only</p>
                  </div>
                )}
              </>
            ) : (
              <div className="glass-card rounded-xl p-8 text-center">
                <Layers className="w-12 h-12 text-[#dee1f7]/20 mx-auto mb-4" />
                <h4 className="font-bold text-[#e1fdff] mb-2">Select a Template</h4>
                <p className="text-xs text-[#dee1f7]/60">Click any template card to see its full details, tech stack, and modules.</p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
