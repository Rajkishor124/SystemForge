import Link from 'next/link';
import { Network, Download, Code, Share, CheckCircle, Server, Database, Shield, Zap, ArrowRight } from 'lucide-react';

export default function ResultPage() {
  return (
    <div className="p-8 max-w-7xl mx-auto w-full">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-end mb-8 gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary-container/10 border border-primary-container/20 text-primary-container text-[10px] font-bold uppercase tracking-widest mb-4">
            <CheckCircle className="w-3 h-3" />
            Generation Complete
          </div>
          <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">Ride-Sharing Backend Architecture</h1>
          <p className="text-[#dee1f7]/60 text-sm">Optimized for 100k+ concurrent users, high availability, and real-time tracking.</p>
        </div>
        <div className="flex gap-3">
          <button className="px-4 py-2 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors flex items-center gap-2">
            <Share className="w-4 h-4" /> Share
          </button>
          <button className="px-4 py-2 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors flex items-center gap-2">
            <Download className="w-4 h-4" /> Export PDF
          </button>
          <Link href="/architecture" className="cta-gradient text-on-primary px-5 py-2 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2">
            <Network className="w-4 h-4" /> View Graph
          </Link>
        </div>
      </div>

      <div className="grid lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-8">
          {/* Executive Summary */}
          <section className="glass-card rounded-xl p-8">
            <h2 className="text-xl font-bold font-headline mb-4 border-b border-outline-variant/20 pb-2">Executive Summary</h2>
            <p className="text-[#dee1f7]/80 leading-relaxed text-sm mb-6">
              The proposed architecture utilizes a microservices pattern deployed on Kubernetes to ensure independent scalability of critical components like location tracking and ride matching. An API Gateway handles initial request routing, authentication, and rate limiting. Real-time features are powered by WebSockets and Redis Pub/Sub, while persistent data is split between PostgreSQL (relational/transactional) and MongoDB (document/logs).
            </p>
            <div className="grid sm:grid-cols-3 gap-4">
              <div className="p-4 rounded-lg bg-surface-container-lowest border border-outline-variant/10">
                <div className="text-xs text-[#dee1f7]/60 mb-1">Estimated Cost</div>
                <div className="text-lg font-bold text-primary-container">$1.2k - $2.5k / mo</div>
              </div>
              <div className="p-4 rounded-lg bg-surface-container-lowest border border-outline-variant/10">
                <div className="text-xs text-[#dee1f7]/60 mb-1">Scalability Score</div>
                <div className="text-lg font-bold text-secondary-container">9.5 / 10</div>
              </div>
              <div className="p-4 rounded-lg bg-surface-container-lowest border border-outline-variant/10">
                <div className="text-xs text-[#dee1f7]/60 mb-1">Complexity</div>
                <div className="text-lg font-bold text-tertiary-container">High</div>
              </div>
            </div>
          </section>

          {/* Core Components */}
          <section className="space-y-4">
            <h2 className="text-xl font-bold font-headline mb-4">Core Components</h2>
            
            <div className="glass-card rounded-xl p-6 flex gap-6 items-start">
              <div className="p-3 rounded-lg bg-primary-container/10 text-primary-container shrink-0">
                <Server className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold mb-2">API Gateway & Auth</h3>
                <p className="text-sm text-[#dee1f7]/70 mb-3">Kong or AWS API Gateway handling routing, SSL termination, and JWT validation. Integrates with Auth0 or custom auth service.</p>
                <div className="flex gap-2">
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">Kong</span>
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">JWT</span>
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">Rate Limiting</span>
                </div>
              </div>
            </div>

            <div className="glass-card rounded-xl p-6 flex gap-6 items-start">
              <div className="p-3 rounded-lg bg-secondary-container/10 text-secondary-container shrink-0">
                <Zap className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold mb-2">Real-time Location Service</h3>
                <p className="text-sm text-[#dee1f7]/70 mb-3">Go-based WebSocket servers managing driver connections. Uses Redis geospatial indexes (GEOADD/GEORADIUS) for fast driver lookups.</p>
                <div className="flex gap-2">
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">Golang</span>
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">WebSockets</span>
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">Redis GEO</span>
                </div>
              </div>
            </div>

            <div className="glass-card rounded-xl p-6 flex gap-6 items-start">
              <div className="p-3 rounded-lg bg-tertiary-container/10 text-tertiary-container shrink-0">
                <Database className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-bold mb-2">Ride Matching & Transactions</h3>
                <p className="text-sm text-[#dee1f7]/70 mb-3">Node.js/Python workers consuming from Kafka topics to process ride requests, calculate fares, and update PostgreSQL.</p>
                <div className="flex gap-2">
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">Kafka</span>
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">PostgreSQL</span>
                  <span className="px-2 py-1 rounded bg-surface-container text-[10px] font-mono">Stripe API</span>
                </div>
              </div>
            </div>
          </section>
        </div>

        <div className="space-y-8">
          {/* Tech Stack */}
          <div className="glass-card rounded-xl p-6">
            <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4">Recommended Stack</h3>
            <div className="space-y-4">
              <div>
                <div className="text-xs font-bold mb-2 text-[#e1fdff]">Compute</div>
                <div className="flex flex-wrap gap-2">
                  <span className="px-2 py-1 rounded border border-outline-variant/20 text-xs">Kubernetes (EKS/GKE)</span>
                  <span className="px-2 py-1 rounded border border-outline-variant/20 text-xs">Golang</span>
                  <span className="px-2 py-1 rounded border border-outline-variant/20 text-xs">Node.js</span>
                </div>
              </div>
              <div>
                <div className="text-xs font-bold mb-2 text-[#e1fdff]">Data</div>
                <div className="flex flex-wrap gap-2">
                  <span className="px-2 py-1 rounded border border-outline-variant/20 text-xs">PostgreSQL</span>
                  <span className="px-2 py-1 rounded border border-outline-variant/20 text-xs">Redis</span>
                  <span className="px-2 py-1 rounded border border-outline-variant/20 text-xs">MongoDB</span>
                </div>
              </div>
              <div>
                <div className="text-xs font-bold mb-2 text-[#e1fdff]">Messaging</div>
                <div className="flex flex-wrap gap-2">
                  <span className="px-2 py-1 rounded border border-outline-variant/20 text-xs">Apache Kafka</span>
                </div>
              </div>
            </div>
          </div>

          {/* Next Steps */}
          <div className="glass-card rounded-xl p-6">
            <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4">Next Steps</h3>
            <div className="space-y-3">
              <button className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                <div className="flex items-center gap-3">
                  <Code className="w-4 h-4 text-primary-container" />
                  <span className="text-sm">Generate Terraform</span>
                </div>
                <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
              </button>
              <button className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                <div className="flex items-center gap-3">
                  <Database className="w-4 h-4 text-secondary-container" />
                  <span className="text-sm">View DB Schemas</span>
                </div>
                <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
              </button>
              <button className="w-full flex items-center justify-between p-3 rounded bg-surface-container-lowest hover:bg-surface-container transition-colors border border-outline-variant/10 group">
                <div className="flex items-center gap-3">
                  <Shield className="w-4 h-4 text-tertiary-container" />
                  <span className="text-sm">Security Audit</span>
                </div>
                <ArrowRight className="w-4 h-4 opacity-0 group-hover:opacity-100 transition-opacity" />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
