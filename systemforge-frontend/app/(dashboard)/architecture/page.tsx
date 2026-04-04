'use client';

import { useEffect, useState, useCallback, useMemo } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import ReactFlow, { 
  Background, 
  Controls, 
  Node, 
  Edge, 
  Handle, 
  Position, 
  ConnectionLineType,
  applyEdgeChanges,
  applyNodeChanges,
  NodeChange,
  EdgeChange,
  NodeProps,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { 
  Download, 
  ZoomIn, 
  ZoomOut, 
  RotateCcw, 
  Server, 
  Shield, 
  Database, 
  Zap, 
  Loader2, 
  AlertCircle,
  ChevronRight,
  Cpu,
  Layout
} from 'lucide-react';
import { api, ApiError } from '@/lib/api';
import { motion, AnimatePresence } from 'motion/react';

// ─── Types ───────────────────────────────────────────────────────────────────

interface RecommendationItem {
  title: string;
  description: string;
  confidence: number;
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
}

interface SystemConfig {
  id: string;
  configName: string;
  appType: string;
  appScale: string;
  generatedOutputJson: string;
  createdAt: string;
}

// ─── Custom Node Component ──────────────────────────────────────────────────

const MODULE_ICONS: Record<string, any> = {
  AUTH: Shield,
  ARCHITECTURE: Server,
  DATABASE: Database,
  PAYMENT: Zap,
  NOTIFICATION: Zap,
  CACHING: Database,
  MESSAGING: Server,
  SEARCH: Server,
  MONITORING: Layout,
  STORAGE: Database,
  GATEWAY: Cpu,
  CLIENT: Layout,
};

const CustomNode = ({ data, selected }: NodeProps) => {
  const Icon = MODULE_ICONS[data.module] || Server;
  
  return (
    <div className={`px-4 py-3 rounded-xl border transition-all duration-300 min-w-[180px] ${
      selected 
        ? 'glass-card border-primary-container shadow-[0_0_20px_rgba(0,242,255,0.3)] scale-105' 
        : 'glass-card border-outline-variant/20 opacity-80'
    }`}>
      <Handle type="target" position={Position.Top} className="!bg-primary-container !border-none !w-2 !h-2" />
      
      <div className="flex items-center gap-3">
        <div className={`p-2 rounded-lg bg-primary-container/10 text-primary-container`}>
          <Icon className="w-5 h-5" />
        </div>
        <div className="flex flex-col">
          <span className="text-xs font-black uppercase tracking-widest text-[#dee1f7]/40">
            {data.type || 'Service'}
          </span>
          <span className="text-sm font-bold text-[#e1fdff] font-headline truncate max-w-[120px]">
            {data.label}
          </span>
        </div>
      </div>
      
      {data.tech && (
        <div className="mt-2 pt-2 border-t border-outline-variant/10 text-[10px] text-[#dee1f7]/50 font-mono">
          {data.tech}
        </div>
      )}
      
      <Handle type="source" position={Position.Bottom} className="!bg-secondary-container !border-none !w-2 !h-2" />
    </div>
  );
};

const nodeTypes = {
  custom: CustomNode,
};

// ─── Main Component ──────────────────────────────────────────────────────────

export default function ArchitecturePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const configId = searchParams.get('configId');

  const [nodes, setNodes] = useState<Node[]>([]);
  const [edges, setEdges] = useState<Edge[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedNode, setSelectedNode] = useState<Node | null>(null);

  const onNodesChange = useCallback(
    (changes: NodeChange[]) => setNodes((nds) => applyNodeChanges(changes, nds)),
    []
  );
  const onEdgesChange = useCallback(
    (changes: EdgeChange[]) => setEdges((eds) => applyEdgeChanges(changes, eds)),
    []
  );

  const onNodeClick = useCallback((event: React.MouseEvent, node: Node) => {
    setSelectedNode(node);
  }, []);

  useEffect(() => {
    async function fetchData() {
      try {
        let currentConfigId = configId;
        
        // Smart fallback: If no configId provided in URL, try to load their latest project
        if (!currentConfigId) {
          const listRes = await api<{ content: SystemConfig[] }>('/api/v1/systems/configs?page=0&size=1');
          if (listRes.data && listRes.data.content.length > 0) {
            currentConfigId = listRes.data.content[0].id;
            // Update URL cleanly without refreshing page
            router.replace(`/architecture?configId=${currentConfigId}`);
            return; // let the URL change trigger the next useEffect cycle
          } else {
            setError('NO_PROJECT');
            setLoading(false);
            return;
          }
        }

        const res = await api<SystemConfig>(`/api/v1/systems/configs/${currentConfigId}`);
        const config = res.data;

        if (!config.generatedOutputJson) {
          setError('No architecture data generated yet.');
          return;
        }

        const result: RecommendationResult = JSON.parse(config.generatedOutputJson);
        
        // Build Graph
        const initialNodes: Node[] = [];
        const initialEdges: Edge[] = [];

        // 1. Add Gateway Node (Center)
        initialNodes.push({
          id: 'gateway',
          type: 'custom',
          data: { label: 'API Gateway', module: 'GATEWAY', type: 'Infrastructure', tech: 'Kong / Nginx' },
          position: { x: 400, y: 150 },
        });

        // 2. Add Client Node (Top)
        initialNodes.push({
          id: 'client',
          type: 'custom',
          data: { label: 'Client App', module: 'CLIENT', type: 'Frontend', tech: 'Next.js / Flutter' },
          position: { x: 400, y: 0 },
        });
        initialEdges.push({
          id: 'e-client-gateway',
          source: 'client',
          target: 'gateway',
          animated: true,
          style: { stroke: '#00f2ff' },
        });

        // 3. Add Module Nodes
        if (result.modules) {
          result.modules.forEach((mod, idx) => {
            const angle = (idx / result.modules.length) * Math.PI * 2;
            const radius = 250;
            const x = 400 + Math.cos(angle) * radius;
            const y = 350 + Math.sin(angle) * radius;

            const mainRec = mod.recommendations[0];
            
            initialNodes.push({
              id: mod.module,
              type: 'custom',
              data: { 
                label: mod.module.replace(/_/g, ' '), 
                module: mod.module, 
                type: 'Service', 
                tech: mainRec?.title || 'System Module',
                recommendations: mod.recommendations 
              },
              position: { x, y },
            });

            initialEdges.push({
              id: `e-gateway-${mod.module}`,
              source: 'gateway',
              target: mod.module,
              type: ConnectionLineType.SmoothStep,
              style: { stroke: '#3a494b' },
            });
          });
        }

        setNodes(initialNodes);
        setEdges(initialEdges);
      } catch (err) {
        if (err instanceof ApiError) setError(err.message);
        else setError('Failed to load architecture graph.');
      } finally {
        setLoading(false);
      }
    }

    fetchData();
  }, [configId, router]);

  if (loading) {
    return (
      <div className="flex-1 flex flex-col items-center justify-center p-8 gap-4 bg-[#090e1c]">
        <div className="relative">
          <div className="absolute inset-0 bg-primary-container/20 blur-2xl rounded-full animate-pulse"></div>
          <Loader2 className="w-10 h-10 text-primary-container animate-spin relative z-10" />
        </div>
        <p className="text-xs font-label uppercase tracking-widest text-[#dee1f7]/40 animate-pulse">Mapping Resources...</p>
      </div>
    );
  }

  if (error) {
    if (error === 'NO_PROJECT') {
      return (
        <div className="flex-1 flex items-center justify-center p-8 bg-[#090e1c]">
          <div className="max-w-md w-full text-center glass-card rounded-xl p-10 border border-outline-variant/10">
            <div className="relative w-16 h-16 mx-auto mb-6">
              <div className="absolute inset-0 bg-primary-container/10 blur-xl rounded-full"></div>
              <Server className="w-16 h-16 text-primary-container/40 relative z-10" />
            </div>
            <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">No Project Selected</h2>
            <p className="text-sm text-[#dee1f7]/60 mb-8 leading-relaxed">
              You haven't generated any system architectures yet. Start a new project to view your topology graph.
            </p>
            <button onClick={() => router.push('/dashboard')} className="cta-gradient text-on-primary px-6 py-2.5 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-all">
              Go to Dashboard
            </button>
          </div>
        </div>
      );
    }

    return (
      <div className="flex-1 flex items-center justify-center p-8 bg-[#090e1c]">
        <div className="max-w-md w-full text-center glass-card rounded-xl p-8 border border-red-500/20">
          <AlertCircle className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">Error</h2>
          <p className="text-sm text-[#dee1f7]/60 mb-6">{error}</p>
          <button onClick={() => router.push('/dashboard')} className="bg-surface-container-high border border-outline-variant/20 text-[#e1fdff] px-5 py-2.5 rounded-lg font-bold text-sm hover:bg-surface-container-highest transition-colors">
            Return to Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full bg-[#090e1c]">
      <div className="p-6 border-b border-outline-variant/10 flex justify-between items-center bg-surface-container-lowest shrink-0 z-20">
        <div>
          <h1 className="text-xl font-bold font-headline text-[#e1fdff]">Resource Graph</h1>
          <p className="text-xs text-[#dee1f7]/60">Interactive topology generated for your system</p>
        </div>
        <div className="flex gap-2">
          <button className="px-4 py-2 rounded bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors flex items-center gap-2">
            <Download className="w-4 h-4" /> Export Map
          </button>
          <button onClick={() => router.push(`/result?configId=${configId}`)} className="cta-gradient text-on-primary px-5 py-2 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2">
            View Details <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="flex-1 relative flex">
        <div className="flex-1 relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeClick={onNodeClick}
            nodeTypes={nodeTypes}
            fitView
            className="mesh-bg"
          >
            <Background color="#1e293b" gap={20} />
            <Controls className="!bg-[#0e1322] !border-outline-variant/20 !fill-[#dee1f7]" />
          </ReactFlow>
        </div>

        {/* Properties Panel */}
        <AnimatePresence>
          {selectedNode && (
            <motion.div 
              initial={{ x: 320, opacity: 0 }}
              animate={{ x: 0, opacity: 1 }}
              exit={{ x: 320, opacity: 0 }}
              transition={{ type: 'spring', damping: 25, stiffness: 200 }}
              className="w-80 glass-panel border-l border-white/10 p-6 overflow-y-auto custom-scrollbar z-10 shadow-[-8px_0_32px_rgba(0,0,0,0.5)]"
            >
              <div className="flex items-center justify-between mb-8">
                <h3 className="font-label text-[10px] uppercase tracking-[0.2em] text-[#dee1f7]/40">Intelligence Node</h3>
                <button onClick={() => setSelectedNode(null)} className="p-2 rounded-lg hover:bg-white/5 text-[#dee1f7]/30 hover:text-[#dee1f7] transition-colors">
                  <RotateCcw className="w-4 h-4" />
                </button>
              </div>

              <div className="mb-8 p-4 rounded-2xl bg-gradient-to-br from-white/5 to-transparent border border-white/5">
                <div className="text-[9px] text-primary-container font-black uppercase tracking-widest mb-2 opacity-70">{selectedNode.data.type}</div>
                <h4 className="text-2xl font-bold text-[#e1fdff] font-headline tracking-tight">{selectedNode.id === 'gateway' ? 'API Gateway' : selectedNode.data.label}</h4>
                <div className="text-[11px] font-mono text-secondary-container mt-2 flex items-center gap-2">
                  <Cpu className="w-3 h-3" /> {selectedNode.data.tech}
                </div>
              </div>

              {selectedNode.data.recommendations && (
                <div className="space-y-6">
                  <h5 className="text-[10px] uppercase tracking-[0.2em] font-black text-[#dee1f7]/20 flex items-center gap-3">
                    <span className="shrink-0">AI Insights</span>
                    <span className="h-px bg-white/5 flex-1"></span>
                  </h5>
                  {selectedNode.data.recommendations.map((rec: any, idx: number) => (
                    <motion.div 
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: idx * 0.1 }}
                      key={idx} 
                      className="p-4 rounded-xl bg-white/[0.03] border border-white/5 hover:border-secondary-container/30 transition-colors group"
                    >
                      <div className="text-xs font-bold text-secondary-container mb-2 group-hover:text-[#00f2ff] transition-colors">{rec.title}</div>
                      <p className="text-[11px] text-[#dee1f7]/60 leading-relaxed">{rec.description}</p>
                      <div className="mt-4 flex items-center justify-between pt-3 border-t border-white/5">
                        <span className="text-[9px] text-[#dee1f7]/30 font-label tracking-widest uppercase">Confidence</span>
                        <div className="flex items-center gap-2">
                           <div className="w-12 h-1 bg-white/5 rounded-full overflow-hidden">
                             <div className="h-full bg-secondary-container" style={{ width: `${rec.confidence * 100}%` }}></div>
                           </div>
                           <span className="text-[10px] font-mono text-secondary-container">{Math.round(rec.confidence * 100)}%</span>
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </div>
              )}
              
              {selectedNode.id === 'gateway' && (
                <div className="space-y-4 mt-8">
                  <h5 className="text-[10px] uppercase tracking-[0.2em] font-black text-[#dee1f7]/20 flex items-center gap-3">
                    <span className="shrink-0">Internal Stack</span>
                    <span className="h-px bg-white/5 flex-1"></span>
                  </h5>
                  <div className="p-4 rounded-xl bg-white/[0.03] border border-white/5">
                    <div className="text-xs font-bold text-primary-container mb-2 text-gradient">Route Mapping</div>
                    <p className="text-[11px] text-[#dee1f7]/60 leading-relaxed text-balance">Dynamic cluster routing based on weighted service discovery.</p>
                  </div>
                  <div className="p-4 rounded-xl bg-white/[0.03] border border-white/5">
                     <div className="text-xs font-bold text-primary-container mb-2 text-gradient">Rate Limiting</div>
                     <p className="text-[11px] text-[#dee1f7]/60 leading-relaxed text-balance">Leaky-bucket throttling with Redis-backed state management.</p>
                  </div>
                </div>
              )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
