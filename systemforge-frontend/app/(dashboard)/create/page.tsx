'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Brain, Settings, Database, Server, Zap, ArrowRight, Sparkles } from 'lucide-react';

export default function CreatePage() {
  const router = useRouter();
  const [prompt, setPrompt] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (prompt.trim()) {
      router.push('/processing');
    }
  };

  return (
    <div className="p-8 max-w-7xl mx-auto w-full h-full flex flex-col">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">Design Your System</h1>
        <p className="text-[#dee1f7]/60 text-sm">Describe your application requirements, and our AI will architect the optimal backend.</p>
      </div>

      <div className="grid lg:grid-cols-3 gap-8 flex-1 min-h-0">
        <div className="lg:col-span-2 flex flex-col h-full">
          <form onSubmit={handleSubmit} className="glass-card rounded-xl p-6 flex flex-col h-full">
            <div className="mb-6">
              <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">System Requirements Prompt</label>
              <textarea 
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="e.g., I need a backend for a ride-sharing app like Uber. It needs to handle real-time location tracking for 100k concurrent users, process payments, and have a matching algorithm for drivers and riders. High availability is crucial."
                className="w-full h-48 bg-surface-container-lowest border border-outline-variant/20 rounded-lg p-4 text-sm focus:ring-1 focus:ring-primary-container focus:border-primary-container outline-none transition-all placeholder:text-on-surface-variant/30 resize-none custom-scrollbar"
                required
              />
            </div>

            <div className="grid sm:grid-cols-2 gap-6 mb-8">
              <div>
                <label className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Expected Scale</label>
                <select className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]">
                  <option>Startup (0 - 10k users)</option>
                  <option>Growth (10k - 100k users)</option>
                  <option>Enterprise (100k+ users)</option>
                </select>
              </div>
              <div>
                <label className="font-label text-[10px] uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Primary Cloud Provider</label>
                <select className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]">
                  <option>AWS</option>
                  <option>Google Cloud</option>
                  <option>Azure</option>
                  <option>Cloud Agnostic</option>
                </select>
              </div>
            </div>

            <div className="mt-auto flex justify-end">
              <button 
                type="submit"
                className="cta-gradient text-on-primary px-8 py-3.5 rounded-lg font-bold text-sm uppercase tracking-widest shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2"
                disabled={!prompt.trim()}
              >
                <Sparkles className="w-4 h-4" />
                Generate Architecture
              </button>
            </div>
          </form>
        </div>

        <div className="space-y-6 flex flex-col h-full">
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
                  <p className="text-xs text-[#dee1f7]/70">Microservices architecture likely required based on complexity.</p>
                </div>
                <div className="p-3 rounded bg-surface-container border border-outline-variant/10">
                  <div className="flex items-center gap-2 mb-2 text-secondary-container">
                    <Database className="w-4 h-4" />
                    <span className="text-xs font-bold">Data Storage</span>
                  </div>
                  <p className="text-xs text-[#dee1f7]/70">Suggesting a mix of Relational (users/payments) and NoSQL/In-memory (real-time tracking).</p>
                </div>
                <div className="p-3 rounded bg-surface-container border border-outline-variant/10">
                  <div className="flex items-center gap-2 mb-2 text-tertiary-container">
                    <Server className="w-4 h-4" />
                    <span className="text-xs font-bold">Infrastructure</span>
                  </div>
                  <p className="text-xs text-[#dee1f7]/70">Kubernetes cluster recommended for scaling individual components.</p>
                </div>
              </div>
            )}
          </div>
          
          <div className="glass-card rounded-xl p-6">
            <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4">Pro Tips</h3>
            <ul className="text-xs text-[#dee1f7]/70 space-y-3">
              <li className="flex gap-2"><ArrowRight className="w-3 h-3 text-primary-container shrink-0 mt-0.5" /> Be specific about read vs. write ratios.</li>
              <li className="flex gap-2"><ArrowRight className="w-3 h-3 text-primary-container shrink-0 mt-0.5" /> Mention any strict latency requirements.</li>
              <li className="flex gap-2"><ArrowRight className="w-3 h-3 text-primary-container shrink-0 mt-0.5" /> Specify if you have a preferred tech stack (e.g., &quot;Use Go and gRPC&quot;).</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
