import Link from 'next/link';
import Image from 'next/image';
import * as motion from 'motion/react-client';
import { Shield, Zap, GitPullRequest, LayoutGrid, Brain, Wrench, Network, ChevronRight, Server, Lock, Database, Globe, Terminal, Cloud } from 'lucide-react';
import { LandingNavbar } from '@/components/layout/landing-navbar';

export default function LandingPage() {
  return (
    <div className="bg-[#090e1c] text-[#dee1f7] font-sans min-h-screen flex flex-col selection:bg-[#00f2ff]/30 selection:text-[#e1fdff] overflow-hidden">
      <LandingNavbar />

      {/* Background Effects */}
      <div className="fixed inset-0 z-0 pointer-events-none">
        <div className="absolute top-[-20%] left-[-10%] w-[50%] h-[50%] rounded-full bg-[#0566d9]/20 blur-[120px]" />
        <div className="absolute bottom-[-20%] right-[-10%] w-[50%] h-[50%] rounded-full bg-[#00f2ff]/10 blur-[120px]" />
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiPjxmaWx0ZXIgaWQ9Im4iPmhmZWlydXJidWxlbmNlIHR5cGU9ImZyYWN0YWxOb2lzZSIgYmFzZUZyZXF1ZW5jeT0iMC42IiBudW1PY3RhdmVzPSIzIiBzdGl0Y2hUaWxlcz0ic3RpdGNoIi8+PC9maWx0ZXI+PHJlY3Qgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsdGVyPSJ1cmwoI24pIi8+PC9zdmc+')] opacity-20 mix-blend-overlay"></div>
      </div>

      <main className="pt-32 pb-20 flex-grow relative z-10">
        {/* Hero Section */}
        <section className="max-w-7xl mx-auto px-6 md:px-8 pt-12 md:pt-24 mb-32">
          <div className="flex flex-col items-center text-center max-w-4xl mx-auto space-y-8">
            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5 }}
              className="inline-flex items-center px-4 py-1.5 rounded-full bg-[#00f2ff]/10 border border-[#00f2ff]/20 text-[#00f2ff] text-xs font-bold tracking-wide uppercase"
            >
              <span className="w-2 h-2 rounded-full bg-[#00f2ff] mr-2 animate-pulse shadow-[0_0_8px_#00f2ff]"></span>
              SystemForge Engine v2.4 is Live
            </motion.div>
            
            <motion.h1 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.1 }}
              className="text-5xl md:text-7xl font-extrabold tracking-tighter leading-[1.1] text-white"
            >
              Architect Scalable <br className="hidden md:block" />
              <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#00f2ff] to-[#0566d9]">
                Backend Systems with AI
              </span>
            </motion.h1>
            
            <motion.p 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.2 }}
              className="text-lg md:text-xl text-[#dee1f7]/70 max-w-2xl leading-relaxed"
            >
              Stop guessing your infrastructure. Generate enterprise-grade system designs, interactive architecture diagrams, and deploy-ready code in seconds.
            </motion.p>
            
            <motion.div 
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ duration: 0.5, delay: 0.3 }}
              className="flex flex-col sm:flex-row gap-4 pt-4 w-full sm:w-auto"
            >
              <Link 
                href="/create" 
                className="group relative flex items-center justify-center gap-2 bg-gradient-to-br from-[#00f2ff] to-[#0566d9] text-white px-8 py-4 rounded-xl font-bold text-sm uppercase tracking-wider shadow-[0_0_20px_rgba(0,242,255,0.3)] hover:shadow-[0_0_30px_rgba(0,242,255,0.5)] hover:brightness-110 transition-all overflow-hidden"
              >
                <div className="absolute inset-0 bg-white/20 translate-y-full group-hover:translate-y-0 transition-transform duration-300 ease-out"></div>
                <span className="relative z-10">Start Designing Free</span>
                <ChevronRight className="w-4 h-4 relative z-10 group-hover:translate-x-1 transition-transform" />
              </Link>
              <Link 
                href="/dashboard" 
                className="flex items-center justify-center gap-2 bg-white/5 border border-white/10 text-white px-8 py-4 rounded-xl font-bold text-sm uppercase tracking-wider hover:bg-white/10 transition-all"
              >
                View Demo
              </Link>
            </motion.div>
          </div>

          {/* Abstract Hero Visual */}
          <motion.div 
            initial={{ opacity: 0, scale: 0.95, y: 40 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.4 }}
            className="mt-20 relative max-w-5xl mx-auto"
          >
            <div className="absolute -inset-1 bg-gradient-to-r from-[#00f2ff]/30 to-[#0566d9]/30 rounded-2xl blur-2xl opacity-50"></div>
            <div className="relative bg-[#0e1322]/80 backdrop-blur-xl rounded-2xl border border-white/10 shadow-2xl overflow-hidden flex flex-col">
              {/* Fake Window Header */}
              <div className="h-12 border-b border-white/10 flex items-center px-4 gap-2 bg-white/5">
                <div className="flex gap-1.5">
                  <div className="w-3 h-3 rounded-full bg-red-500/80"></div>
                  <div className="w-3 h-3 rounded-full bg-yellow-500/80"></div>
                  <div className="w-3 h-3 rounded-full bg-green-500/80"></div>
                </div>
                <div className="mx-auto text-xs font-mono text-white/40 tracking-widest uppercase">SystemForge Visualizer</div>
              </div>
              {/* Fake Window Content */}
              <div className="p-8 grid md:grid-cols-3 gap-6 items-center">
                <div className="col-span-1 space-y-4">
                  <div className="h-4 w-24 bg-white/10 rounded animate-pulse"></div>
                  <div className="h-8 w-full bg-white/5 border border-white/10 rounded-lg p-3 flex items-center gap-3">
                    <Server className="w-4 h-4 text-[#00f2ff]" />
                    <div className="h-2 w-20 bg-white/20 rounded"></div>
                  </div>
                  <div className="h-8 w-full bg-white/5 border border-white/10 rounded-lg p-3 flex items-center gap-3">
                    <Database className="w-4 h-4 text-[#0566d9]" />
                    <div className="h-2 w-24 bg-white/20 rounded"></div>
                  </div>
                  <div className="h-8 w-full bg-white/5 border border-white/10 rounded-lg p-3 flex items-center gap-3">
                    <Lock className="w-4 h-4 text-purple-400" />
                    <div className="h-2 w-16 bg-white/20 rounded"></div>
                  </div>
                </div>
                <div className="col-span-2 relative h-64 border border-white/10 rounded-xl bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-white/5 to-transparent flex items-center justify-center overflow-hidden">
                  {/* Abstract Nodes */}
                  <div className="absolute inset-0 opacity-20" style={{ backgroundImage: 'radial-gradient(circle at 2px 2px, white 1px, transparent 0)', backgroundSize: '24px 24px' }}></div>
                  
                  <motion.div 
                    animate={{ y: [0, -10, 0] }} 
                    transition={{ duration: 4, repeat: Infinity, ease: "easeInOut" }}
                    className="absolute top-1/4 left-1/4 w-16 h-16 bg-[#00f2ff]/20 border border-[#00f2ff]/50 rounded-2xl backdrop-blur-md flex items-center justify-center shadow-[0_0_30px_rgba(0,242,255,0.2)]"
                  >
                    <Globe className="w-8 h-8 text-[#00f2ff]" />
                  </motion.div>
                  
                  <motion.div 
                    animate={{ y: [0, 10, 0] }} 
                    transition={{ duration: 5, repeat: Infinity, ease: "easeInOut", delay: 1 }}
                    className="absolute bottom-1/4 right-1/4 w-20 h-20 bg-[#0566d9]/20 border border-[#0566d9]/50 rounded-2xl backdrop-blur-md flex items-center justify-center shadow-[0_0_30px_rgba(5,102,217,0.2)]"
                  >
                    <Database className="w-10 h-10 text-[#0566d9]" />
                  </motion.div>

                  <motion.div 
                    animate={{ scale: [1, 1.05, 1] }} 
                    transition={{ duration: 3, repeat: Infinity, ease: "easeInOut" }}
                    className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-24 h-24 bg-purple-500/20 border border-purple-500/50 rounded-full backdrop-blur-md flex items-center justify-center shadow-[0_0_40px_rgba(168,85,247,0.2)]"
                  >
                    <Brain className="w-12 h-12 text-purple-400" />
                  </motion.div>

                  {/* Connecting Lines (SVG) */}
                  <svg className="absolute inset-0 w-full h-full pointer-events-none" viewBox="0 0 100 100" preserveAspectRatio="none" style={{ zIndex: -1 }}>
                    <motion.path 
                      initial={{ pathLength: 0 }}
                      animate={{ pathLength: 1 }}
                      transition={{ duration: 2, repeat: Infinity, ease: "linear" }}
                      d="M 25 25 L 50 50" 
                      stroke="#00f2ff" strokeWidth="2" strokeDasharray="4 4" fill="none" opacity="0.5" 
                    />
                    <motion.path 
                      initial={{ pathLength: 0 }}
                      animate={{ pathLength: 1 }}
                      transition={{ duration: 2.5, repeat: Infinity, ease: "linear", delay: 0.5 }}
                      d="M 50 50 L 75 75" 
                      stroke="#0566d9" strokeWidth="2" strokeDasharray="4 4" fill="none" opacity="0.5" 
                    />
                  </svg>
                </div>
              </div>
            </div>
          </motion.div>
        </section>

        {/* Social Proof */}
        <section className="border-y border-white/5 bg-white/[0.02] py-10 mb-32">
          <div className="max-w-7xl mx-auto px-8 text-center">
            <p className="text-xs font-bold uppercase tracking-widest text-[#dee1f7]/40 mb-8">Trusted by innovative engineering teams</p>
            <div className="flex flex-wrap justify-center gap-12 md:gap-24 opacity-50 grayscale hover:grayscale-0 transition-all duration-500">
              {/* Placeholder Logos */}
              <div className="flex items-center gap-2 font-bold text-xl"><Cloud className="w-6 h-6" /> CloudScale</div>
              <div className="flex items-center gap-2 font-bold text-xl"><Shield className="w-6 h-6" /> SecureNet</div>
              <div className="flex items-center gap-2 font-bold text-xl"><Zap className="w-6 h-6" /> FastAPI</div>
              <div className="flex items-center gap-2 font-bold text-xl"><Database className="w-6 h-6" /> DataFlow</div>
            </div>
          </div>
        </section>

        {/* How It Works */}
        <section className="max-w-7xl mx-auto px-8 mb-32">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-5xl font-bold tracking-tight text-white mb-4">From Idea to Architecture in <span className="text-[#00f2ff]">Seconds</span></h2>
            <p className="text-[#dee1f7]/60 max-w-2xl mx-auto">SystemForge acts as your AI Staff Engineer, analyzing requirements and generating production-ready system designs.</p>
          </div>

          <div className="grid md:grid-cols-3 gap-8 relative">
            {/* Connecting Line */}
            <div className="hidden md:block absolute top-12 left-[15%] right-[15%] h-0.5 bg-gradient-to-r from-[#00f2ff]/0 via-[#00f2ff]/30 to-[#00f2ff]/0"></div>

            {[
              { step: '01', title: 'Define Requirements', desc: 'Input your app idea, expected traffic, and constraints in plain English.' },
              { step: '02', title: 'AI Analysis', desc: 'Our engine cross-references millions of successful enterprise patterns.' },
              { step: '03', title: 'Export & Deploy', desc: 'Get interactive diagrams, tech stack justifications, and IaC code.' }
            ].map((item, i) => (
              <div key={i} className="relative z-10 flex flex-col items-center text-center p-6 rounded-2xl bg-white/[0.02] border border-white/5 hover:bg-white/[0.04] transition-colors">
                <div className="w-16 h-16 rounded-full bg-[#090e1c] border-2 border-[#00f2ff]/30 flex items-center justify-center text-[#00f2ff] font-bold text-xl mb-6 shadow-[0_0_15px_rgba(0,242,255,0.2)]">
                  {item.step}
                </div>
                <h3 className="text-xl font-bold text-white mb-3">{item.title}</h3>
                <p className="text-[#dee1f7]/60 text-sm leading-relaxed">{item.desc}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Features Bento Grid */}
        <section className="max-w-7xl mx-auto px-8 mb-32">
          <div className="text-center mb-16">
            <h2 className="text-3xl md:text-5xl font-bold tracking-tight text-white mb-4">Enterprise-Grade <span className="text-[#0566d9]">Capabilities</span></h2>
            <p className="text-[#dee1f7]/60 max-w-2xl mx-auto">Everything you need to design, validate, and deploy scalable backend systems.</p>
          </div>

          <div className="grid md:grid-cols-3 gap-6">
            <div className="md:col-span-2 p-8 rounded-2xl bg-gradient-to-br from-white/[0.05] to-transparent border border-white/10 relative overflow-hidden group hover:border-[#00f2ff]/30 transition-colors">
              <div className="absolute top-0 right-0 w-64 h-64 bg-[#00f2ff]/10 rounded-full blur-3xl -translate-y-1/2 translate-x-1/2 group-hover:bg-[#00f2ff]/20 transition-colors"></div>
              <div className="relative z-10 space-y-4">
                <div className="w-12 h-12 rounded-xl bg-[#00f2ff]/10 flex items-center justify-center mb-6">
                  <Brain className="text-[#00f2ff] w-6 h-6" />
                </div>
                <h3 className="text-2xl font-bold text-white">Intelligent Recommendations</h3>
                <p className="text-[#dee1f7]/60 max-w-md leading-relaxed">Our AI doesn&apos;t just suggest tools; it understands dependencies. It knows when your choice of PostgreSQL needs a PGBouncer middleman or when your read-heavy app needs a Redis layer.</p>
              </div>
            </div>
            
            <div className="p-8 rounded-2xl bg-white/[0.02] border border-white/10 hover:bg-white/[0.04] transition-colors">
              <div className="w-12 h-12 rounded-xl bg-[#0566d9]/10 flex items-center justify-center mb-6">
                <LayoutGrid className="text-[#0566d9] w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-3">Modular Design</h3>
              <p className="text-sm text-[#dee1f7]/60 leading-relaxed">Switch components in and out. See how changing from REST to gRPC affects your architecture instantly.</p>
            </div>
            
            <div className="p-8 rounded-2xl bg-white/[0.02] border border-white/10 hover:bg-white/[0.04] transition-colors">
              <div className="w-12 h-12 rounded-xl bg-purple-500/10 flex items-center justify-center mb-6">
                <Shield className="text-purple-400 w-6 h-6" />
              </div>
              <h3 className="text-xl font-bold text-white mb-3">Security First</h3>
              <p className="text-sm text-[#dee1f7]/60 leading-relaxed">Automated vulnerability scanning and best-practice security architecture recommendations built-in.</p>
            </div>
            
            <div className="md:col-span-2 p-8 rounded-2xl bg-gradient-to-bl from-white/[0.05] to-transparent border border-white/10 relative overflow-hidden group hover:border-[#0566d9]/30 transition-colors">
              <div className="absolute bottom-0 left-0 w-64 h-64 bg-[#0566d9]/10 rounded-full blur-3xl translate-y-1/2 -translate-x-1/2 group-hover:bg-[#0566d9]/20 transition-colors"></div>
              <div className="relative z-10 space-y-4">
                <div className="w-12 h-12 rounded-xl bg-[#0566d9]/10 flex items-center justify-center mb-6">
                  <Network className="text-[#0566d9] w-6 h-6" />
                </div>
                <h3 className="text-2xl font-bold text-white">Scalable Architecture Planning</h3>
                <p className="text-[#dee1f7]/60 max-w-md leading-relaxed">Plan for 10 users or 10 million. Visualize your data flow through horizontal scaling, load balancing, and sharding strategies designed for enterprise growth.</p>
              </div>
            </div>
          </div>
        </section>

        {/* Final CTA */}
        <section className="max-w-5xl mx-auto px-6 md:px-8 pb-20">
          <div className="relative rounded-3xl overflow-hidden border border-white/10 bg-[#0e1322]">
            {/* CTA Background Gradients */}
            <div className="absolute top-0 left-0 w-full h-full bg-gradient-to-br from-[#00f2ff]/10 via-transparent to-[#0566d9]/20"></div>
            <div className="absolute -top-24 -right-24 w-96 h-96 bg-[#00f2ff]/20 rounded-full blur-[100px]"></div>
            <div className="absolute -bottom-24 -left-24 w-96 h-96 bg-[#0566d9]/20 rounded-full blur-[100px]"></div>
            
            <div className="relative z-10 px-8 py-20 md:py-24 text-center flex flex-col items-center">
              <h2 className="text-4xl md:text-5xl font-extrabold tracking-tight text-white mb-6">Ready to Build Better Systems?</h2>
              <p className="text-[#dee1f7]/70 max-w-xl mx-auto text-lg mb-10">Join 15,000+ developers building robust, production-ready infrastructures without the guesswork.</p>
              
              <Link 
                href="/dashboard" 
                className="group relative flex items-center justify-center gap-2 bg-white text-[#090e1c] px-10 py-5 rounded-xl font-bold text-sm uppercase tracking-wider shadow-[0_0_30px_rgba(255,255,255,0.2)] hover:shadow-[0_0_40px_rgba(255,255,255,0.4)] transition-all overflow-hidden"
              >
                <div className="absolute inset-0 bg-[#00f2ff]/10 translate-y-full group-hover:translate-y-0 transition-transform duration-300 ease-out"></div>
                <span className="relative z-10">Start Designing Free</span>
                <ChevronRight className="w-4 h-4 relative z-10 group-hover:translate-x-1 transition-transform" />
              </Link>
            </div>
          </div>
        </section>
      </main>

      <footer className="bg-[#090e1c] w-full py-12 border-t border-white/5 font-sans text-sm">
        <div className="flex flex-col md:flex-row justify-between items-center px-8 md:px-12 gap-8 max-w-7xl mx-auto">
          <div className="flex items-center gap-2 text-lg font-bold text-white tracking-tight">
            <Terminal className="w-5 h-5 text-[#00f2ff]" />
            SystemForge
          </div>
          <div className="flex flex-wrap justify-center gap-8">
            <Link href="#" className="text-[#dee1f7]/50 hover:text-white transition-colors">Documentation</Link>
            <Link href="#" className="text-[#dee1f7]/50 hover:text-white transition-colors">Changelog</Link>
            <Link href="#" className="text-[#dee1f7]/50 hover:text-white transition-colors">Status</Link>
            <Link href="#" className="text-[#dee1f7]/50 hover:text-white transition-colors">Privacy</Link>
            <Link href="#" className="text-[#dee1f7]/50 hover:text-white transition-colors">Terms</Link>
          </div>
          <div className="text-[#dee1f7]/40">
            © 2026 SystemForge AI. Engineered for Scale.
          </div>
        </div>
      </footer>
    </div>
  );
}
