import Link from 'next/link';
import { Plus, LayoutGrid, List, Brain, Activity, ArrowRight, Zap, Shield, Database } from 'lucide-react';

export default function DashboardPage() {
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

      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between mb-4">
            <h2 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60">Recent Architectures</h2>
            <div className="flex gap-2">
              <button className="p-1.5 rounded bg-surface-container-high text-[#00f2ff]"><LayoutGrid className="w-4 h-4" /></button>
              <button className="p-1.5 rounded bg-surface-container text-[#dee1f7]/40 hover:text-[#dee1f7]"><List className="w-4 h-4" /></button>
            </div>
          </div>

          <div className="grid sm:grid-cols-2 gap-4">
            {/* Project Card 1 */}
            <div className="glass-card rounded-xl p-6 hover:border-primary-container/40 transition-colors group cursor-pointer relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary-container to-secondary-container opacity-0 group-hover:opacity-100 transition-opacity"></div>
              <div className="flex justify-between items-start mb-4">
                <div className="p-2 rounded-lg bg-primary-container/10 text-primary-container">
                  <Database className="w-5 h-5" />
                </div>
                <span className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/40">2 days ago</span>
              </div>
              <h3 className="font-headline font-bold text-lg mb-2 text-[#e1fdff] group-hover:text-[#00f2ff] transition-colors">E-commerce Microservices</h3>
              <p className="text-sm text-[#dee1f7]/60 mb-6 line-clamp-2">High-throughput retail backend with inventory management and payment gateway integration.</p>
              <div className="flex items-center justify-between mt-auto">
                <div className="flex -space-x-2">
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">PG</div>
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">RD</div>
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">KF</div>
                </div>
                <Link href="/project/1" className="text-xs font-bold text-primary-container flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  View <ArrowRight className="w-3 h-3" />
                </Link>
              </div>
            </div>

            {/* Project Card 2 */}
            <div className="glass-card rounded-xl p-6 hover:border-primary-container/40 transition-colors group cursor-pointer relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary-container to-secondary-container opacity-0 group-hover:opacity-100 transition-opacity"></div>
              <div className="flex justify-between items-start mb-4">
                <div className="p-2 rounded-lg bg-secondary-container/10 text-secondary-container">
                  <Zap className="w-5 h-5" />
                </div>
                <span className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/40">5 days ago</span>
              </div>
              <h3 className="font-headline font-bold text-lg mb-2 text-[#e1fdff] group-hover:text-[#00f2ff] transition-colors">Real-time Chat Server</h3>
              <p className="text-sm text-[#dee1f7]/60 mb-6 line-clamp-2">WebSocket based messaging platform with presence detection and message history.</p>
              <div className="flex items-center justify-between mt-auto">
                <div className="flex -space-x-2">
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">WS</div>
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">RD</div>
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">MG</div>
                </div>
                <Link href="/project/2" className="text-xs font-bold text-primary-container flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  View <ArrowRight className="w-3 h-3" />
                </Link>
              </div>
            </div>
            
            {/* Project Card 3 */}
            <div className="glass-card rounded-xl p-6 hover:border-primary-container/40 transition-colors group cursor-pointer relative overflow-hidden">
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary-container to-secondary-container opacity-0 group-hover:opacity-100 transition-opacity"></div>
              <div className="flex justify-between items-start mb-4">
                <div className="p-2 rounded-lg bg-tertiary-container/10 text-tertiary-container">
                  <Shield className="w-5 h-5" />
                </div>
                <span className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/40">1 week ago</span>
              </div>
              <h3 className="font-headline font-bold text-lg mb-2 text-[#e1fdff] group-hover:text-[#00f2ff] transition-colors">Fintech Auth Gateway</h3>
              <p className="text-sm text-[#dee1f7]/60 mb-6 line-clamp-2">Secure API gateway with rate limiting, OAuth2, and anomaly detection.</p>
              <div className="flex items-center justify-between mt-auto">
                <div className="flex -space-x-2">
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">GO</div>
                  <div className="w-6 h-6 rounded-full bg-surface-container border border-outline-variant/20 flex items-center justify-center text-[10px] font-bold">RD</div>
                </div>
                <Link href="/project/3" className="text-xs font-bold text-primary-container flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                  View <ArrowRight className="w-3 h-3" />
                </Link>
              </div>
            </div>

            {/* Empty State / Create New */}
            <Link href="/create" className="rounded-xl p-6 border-2 border-dashed border-outline-variant/30 hover:border-primary-container/50 hover:bg-primary-container/5 transition-all flex flex-col items-center justify-center text-center min-h-[220px] group">
              <div className="w-12 h-12 rounded-full bg-surface-container-high flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                <Plus className="w-6 h-6 text-[#dee1f7]/60 group-hover:text-primary-container" />
              </div>
              <h3 className="font-headline font-bold text-[#dee1f7] mb-1">Create New Project</h3>
              <p className="text-xs text-[#dee1f7]/50">Start a new AI-assisted design session</p>
            </Link>
          </div>
        </div>

        <div className="space-y-6">
          <h2 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60">System Intelligence</h2>
          
          <div className="glass-card rounded-xl p-6">
            <div className="flex items-center gap-3 mb-6">
              <Brain className="w-5 h-5 text-primary-container" />
              <h3 className="font-headline font-bold text-[#e1fdff]">AI Insights</h3>
            </div>
            <div className="space-y-4">
              <div className="p-3 rounded-lg bg-surface-container-lowest border border-outline-variant/10">
                <div className="flex justify-between items-start mb-1">
                  <span className="text-xs font-bold text-secondary-container">Optimization Suggestion</span>
                  <span className="text-[10px] text-[#dee1f7]/40">Just now</span>
                </div>
                <p className="text-xs text-[#dee1f7]/70">Consider adding a CDN layer to your E-commerce project to reduce latency for static assets.</p>
              </div>
              <div className="p-3 rounded-lg bg-surface-container-lowest border border-outline-variant/10">
                <div className="flex justify-between items-start mb-1">
                  <span className="text-xs font-bold text-error">Security Alert</span>
                  <span className="text-[10px] text-[#dee1f7]/40">2 hrs ago</span>
                </div>
                <p className="text-xs text-[#dee1f7]/70">Your Fintech Auth Gateway design might need stricter rate limiting on the /login endpoint.</p>
              </div>
            </div>
          </div>

          <div className="glass-card rounded-xl p-6">
            <div className="flex items-center gap-3 mb-6">
              <Activity className="w-5 h-5 text-primary-container" />
              <h3 className="font-headline font-bold text-[#e1fdff]">Usage Stats</h3>
            </div>
            <div className="space-y-4">
              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-[#dee1f7]/60">Architectures Generated</span>
                  <span className="font-bold text-[#00f2ff]">3 / 5</span>
                </div>
                <div className="h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden">
                  <div className="h-full bg-primary-container w-[60%]"></div>
                </div>
              </div>
              <div>
                <div className="flex justify-between text-xs mb-1">
                  <span className="text-[#dee1f7]/60">AI Tokens Used</span>
                  <span className="font-bold text-[#00f2ff]">12.4k / 50k</span>
                </div>
                <div className="h-1.5 w-full bg-surface-container-highest rounded-full overflow-hidden">
                  <div className="h-full bg-secondary-container w-[25%]"></div>
                </div>
              </div>
            </div>
            <button className="w-full mt-6 py-2 rounded border border-primary-container/30 text-primary-container text-xs font-bold uppercase tracking-widest hover:bg-primary-container/10 transition-colors">
              Upgrade Plan
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
