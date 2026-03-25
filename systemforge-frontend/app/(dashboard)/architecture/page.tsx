import { Download, ZoomIn, ZoomOut, RotateCcw } from 'lucide-react';

export default function ArchitecturePage() {
  return (
    <div className="flex flex-col h-full">
      <div className="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest shrink-0">
        <div>
          <h1 className="text-xl font-bold font-headline text-[#e1fdff]">Resource Graph</h1>
          <p className="text-xs text-[#dee1f7]/60">Interactive topology of your generated system</p>
        </div>
        <div className="flex gap-2">
          <button className="p-2 rounded bg-surface-container hover:bg-surface-container-high transition-colors text-[#dee1f7]/80" title="Zoom In">
            <ZoomIn className="w-4 h-4" />
          </button>
          <button className="p-2 rounded bg-surface-container hover:bg-surface-container-high transition-colors text-[#dee1f7]/80" title="Zoom Out">
            <ZoomOut className="w-4 h-4" />
          </button>
          <button className="p-2 rounded bg-surface-container hover:bg-surface-container-high transition-colors text-[#dee1f7]/80" title="Reset View">
            <RotateCcw className="w-4 h-4" />
          </button>
          <div className="w-px h-8 bg-outline-variant/20 mx-2"></div>
          <button className="px-4 py-2 rounded bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors flex items-center gap-2">
            <Download className="w-4 h-4" /> Export SVG
          </button>
        </div>
      </div>

      <div className="flex-1 relative bg-[#090e1c] overflow-hidden mesh-bg">
        {/* Static representation of a node graph using absolute positioning */}
        <div className="absolute inset-0 flex items-center justify-center">
          <div className="relative w-[800px] h-[600px]">
            
            {/* SVG Lines for connections */}
            <svg className="absolute inset-0 w-full h-full pointer-events-none" style={{ zIndex: 0 }}>
              <defs>
                <linearGradient id="lineGrad1" x1="0%" y1="0%" x2="100%" y2="0%">
                  <stop offset="0%" stopColor="#00f2ff" stopOpacity="0.5" />
                  <stop offset="100%" stopColor="#0566d9" stopOpacity="0.5" />
                </linearGradient>
              </defs>
              {/* Client to Gateway */}
              <path d="M 100 300 C 200 300, 200 300, 300 300" stroke="url(#lineGrad1)" strokeWidth="2" fill="none" strokeDasharray="5,5" className="animate-[dash_20s_linear_infinite]" />
              
              {/* Gateway to Auth */}
              <path d="M 350 270 C 350 150, 450 150, 500 150" stroke="#3a494b" strokeWidth="2" fill="none" />
              
              {/* Gateway to Location */}
              <path d="M 400 300 C 450 300, 450 300, 500 300" stroke="#3a494b" strokeWidth="2" fill="none" />
              
              {/* Gateway to Matching */}
              <path d="M 350 330 C 350 450, 450 450, 500 450" stroke="#3a494b" strokeWidth="2" fill="none" />

              {/* Location to Redis */}
              <path d="M 600 300 C 650 300, 650 220, 700 220" stroke="#3a494b" strokeWidth="2" fill="none" />

              {/* Matching to Kafka */}
              <path d="M 600 450 C 650 450, 650 380, 700 380" stroke="#3a494b" strokeWidth="2" fill="none" />
              
              {/* Matching to Postgres */}
              <path d="M 600 450 C 650 450, 650 520, 700 520" stroke="#3a494b" strokeWidth="2" fill="none" />
            </svg>

            {/* Nodes */}
            {/* Client */}
            <div className="absolute left-[20px] top-[260px] w-20 h-20 rounded-xl glass-card flex flex-col items-center justify-center z-10 border-primary-container/30 shadow-[0_0_15px_rgba(0,242,255,0.1)]">
              <div className="text-xs font-bold text-[#dee1f7]">Client</div>
              <div className="text-[9px] text-[#dee1f7]/50 mt-1">Mobile/Web</div>
            </div>

            {/* API Gateway */}
            <div className="absolute left-[300px] top-[250px] w-24 h-24 rounded-xl bg-surface-container-high border-2 border-primary-container flex flex-col items-center justify-center z-10 shadow-[0_0_20px_rgba(0,242,255,0.2)]">
              <div className="text-sm font-bold text-primary-container">API Gateway</div>
              <div className="text-[10px] text-[#dee1f7]/60 mt-1">Kong</div>
            </div>

            {/* Auth Service */}
            <div className="absolute left-[500px] top-[110px] w-24 h-20 rounded-xl glass-card flex flex-col items-center justify-center z-10 hover:border-secondary-container/50 transition-colors cursor-pointer">
              <div className="text-xs font-bold text-[#dee1f7]">Auth Service</div>
              <div className="text-[9px] text-[#dee1f7]/50 mt-1">Node.js</div>
            </div>

            {/* Location Service */}
            <div className="absolute left-[500px] top-[260px] w-24 h-20 rounded-xl glass-card flex flex-col items-center justify-center z-10 hover:border-secondary-container/50 transition-colors cursor-pointer">
              <div className="text-xs font-bold text-[#dee1f7]">Location WS</div>
              <div className="text-[9px] text-[#dee1f7]/50 mt-1">Golang</div>
            </div>

            {/* Matching Service */}
            <div className="absolute left-[500px] top-[410px] w-24 h-20 rounded-xl glass-card flex flex-col items-center justify-center z-10 hover:border-secondary-container/50 transition-colors cursor-pointer">
              <div className="text-xs font-bold text-[#dee1f7]">Ride Matching</div>
              <div className="text-[9px] text-[#dee1f7]/50 mt-1">Python</div>
            </div>

            {/* Redis */}
            <div className="absolute left-[700px] top-[180px] w-20 h-20 rounded-full bg-surface-container border border-error/30 flex flex-col items-center justify-center z-10 shadow-[0_0_15px_rgba(255,180,171,0.1)]">
              <div className="text-xs font-bold text-error">Redis</div>
              <div className="text-[9px] text-[#dee1f7]/50 mt-1">Cache/GEO</div>
            </div>

            {/* Kafka */}
            <div className="absolute left-[700px] top-[340px] w-20 h-20 rounded-full bg-surface-container border border-tertiary-container/30 flex flex-col items-center justify-center z-10 shadow-[0_0_15px_rgba(254,216,58,0.1)]">
              <div className="text-xs font-bold text-tertiary-container">Kafka</div>
              <div className="text-[9px] text-[#dee1f7]/50 mt-1">Events</div>
            </div>

            {/* Postgres */}
            <div className="absolute left-[700px] top-[480px] w-20 h-20 rounded-full bg-surface-container border border-secondary-container/30 flex flex-col items-center justify-center z-10 shadow-[0_0_15px_rgba(5,102,217,0.1)]">
              <div className="text-xs font-bold text-secondary-container">Postgres</div>
              <div className="text-[9px] text-[#dee1f7]/50 mt-1">Primary DB</div>
            </div>

          </div>
        </div>

        {/* Properties Panel (Mock) */}
        <div className="absolute right-6 top-6 bottom-6 w-64 glass-panel rounded-xl border border-outline-variant/20 p-4 flex flex-col">
          <h3 className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-4 border-b border-outline-variant/10 pb-2">Node Properties</h3>
          <div className="space-y-4">
            <div>
              <div className="text-[10px] text-[#dee1f7]/40 uppercase mb-1">Selected Node</div>
              <div className="text-sm font-bold text-primary-container">API Gateway</div>
            </div>
            <div>
              <div className="text-[10px] text-[#dee1f7]/40 uppercase mb-1">Type</div>
              <div className="text-sm">Load Balancer / Router</div>
            </div>
            <div>
              <div className="text-[10px] text-[#dee1f7]/40 uppercase mb-1">Technology</div>
              <div className="text-sm">Kong API Gateway</div>
            </div>
            <div>
              <div className="text-[10px] text-[#dee1f7]/40 uppercase mb-1">Scaling Policy</div>
              <div className="text-sm">Horizontal (CPU &gt; 70%)</div>
            </div>
          </div>
          <div className="mt-auto">
            <button className="w-full py-2 rounded bg-surface-container border border-outline-variant/20 text-xs font-bold hover:bg-surface-container-high transition-colors">
              Edit Configuration
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
