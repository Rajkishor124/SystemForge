'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import { Bot, User, Send, Plus, Search, Pencil, Trash2, Loader2, ListTree, CheckCircle2, CircleDashed, ChevronDown, ChevronUp, Copy, Check, Info } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { motion, AnimatePresence } from 'motion/react';
import { api } from '@/lib/api';
import { ConfirmModal } from '@/components/ui/confirm-modal';

// ─── Types ───────────────────────────────────────────────────────────────────

interface AgentStep {
  name: string;
  order: number;
  status: 'COMPLETED' | 'FAILED' | 'SKIPPED';
  output: string;
  durationMs: number;
}

interface ArchitectMessage {
  id: string;
  role: string;
  content: string;
  source?: string;
  intent?: string;
  processingTimeMs?: number;
  createdAt: string;
  steps: AgentStep[];
}

interface ArchitectSession {
  id: string;
  title: string;
  intent: string;
  status: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  lastMessagePreview?: string;
}

interface ArchitectSessionDetail {
  id: string;
  title: string;
  intent: string;
  status: string;
  createdAt: string;
  messages: ArchitectMessage[];
}

// ─── Code block with copy ────────────────────────────────────────────────────

function CodeBlock({ className, children, ...props }: any) {
  const [copied, setCopied] = useState(false);
  const match = /language-(\w+)/.exec(className || '');
  const code = String(children).replace(/\n$/, '');

  const handleCopy = () => {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  if (match) {
    return (
      <div className="relative group/code">
        <button
          onClick={handleCopy}
          className="absolute top-2 right-2 p-1.5 rounded-md bg-white/5 hover:bg-white/10 text-[#dee1f7]/40 hover:text-[#dee1f7]/80 transition-all opacity-0 group-hover/code:opacity-100 z-10"
          title="Copy code"
        >
          {copied ? <Check className="w-3.5 h-3.5 text-green-400" /> : <Copy className="w-3.5 h-3.5" />}
        </button>
        <SyntaxHighlighter
          {...props}
          style={vscDarkPlus}
          language={match[1]}
          PreTag="div"
          customStyle={{ margin: 0, background: 'transparent', padding: '0.75rem', fontSize: '0.85rem' }}
        >
          {code}
        </SyntaxHighlighter>
      </div>
    );
  }
  return (
    <code {...props} className={`${className || ''} bg-surface-container-high px-1.5 py-0.5 rounded-md text-primary-container font-mono text-[0.85em]`}>
      {children}
    </code>
  );
}

// ─── Time formatting ─────────────────────────────────────────────────────────

function formatTime(dateStr: string): string {
  try {
    const d = new Date(dateStr);
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  } catch {
    return '';
  }
}

function timeAgo(dateStr: string): string {
  try {
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
  } catch {
    return '';
  }
}

// ─── Main Component ──────────────────────────────────────────────────────────

export default function ArchitectPage() {
  // Sidebar state
  const [sessions, setSessions] = useState<ArchitectSession[]>([]);
  const [loadingSessions, setLoadingSessions] = useState(true);
  const [activeSessionId, setActiveSessionId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Chat state
  const [messages, setMessages] = useState<ArchitectMessage[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [activeTitle, setActiveTitle] = useState('New Design Session');
  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  // UI state
  const [editingSessionId, setEditingSessionId] = useState<string | null>(null);
  const [editTitleValue, setEditTitleValue] = useState('');
  const [sessionToDelete, setSessionToDelete] = useState<string | null>(null);
  const [creatingSession, setCreatingSession] = useState(false);
  
  // Right Panel state - show details of the last AI message
  const [expandedSteps, setExpandedSteps] = useState<Record<string, boolean>>({});

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // ─── Load sessions on mount ─────────────────────────────────────────────

  const fetchSessions = useCallback(async () => {
    try {
      const res = await api<ArchitectSession[]>('/api/v1/architect/sessions');
      setSessions(res.data);
      return res.data;
    } catch {
      return [];
    } finally {
      setLoadingSessions(false);
    }
  }, []);

  useEffect(() => { fetchSessions(); }, [fetchSessions]);

  // ─── Load messages when switching sessions ─────────────────────────────

  const loadSession = useCallback(async (sessionId: string) => {
    setActiveSessionId(sessionId);
    setLoadingMessages(true);
    setMessages([]);

    try {
      const res = await api<ArchitectSessionDetail>(`/api/v1/architect/sessions/${sessionId}`);
      setMessages(res.data.messages);
      setActiveTitle(res.data.title);
    } catch {
      setMessages([]);
    } finally {
      setLoadingMessages(false);
    }
  }, []);

  // ─── Auto-scroll ─────────────────────────────────────────────────────────

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isTyping]);

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 160) + 'px';
    }
  }, [input]);

  // ─── Send message ────────────────────────────────────────────────────────

  const handleSend = async () => {
    const text = input.trim();
    if (!text || isTyping) return;

    // Optimistically add user message
    const tempUserMsg: ArchitectMessage = {
      id: 'temp-' + Date.now(),
      role: 'user',
      content: text,
      createdAt: new Date().toISOString(),
      steps: []
    };
    setMessages(prev => [...prev, tempUserMsg]);
    setInput('');
    setIsTyping(true);

    try {
      const payload: any = { message: text };
      if (activeSessionId) {
        payload.sessionId = activeSessionId;
      }

      // API request to the AI Architect
      const res = await api<any>(`/api/v1/architect/chat`, {
        method: 'POST',
        body: payload,
      });

      const responseData = res.data;
      
      // Extract newly returned session ID if it was a new session
      if (!activeSessionId) {
        setActiveSessionId(responseData.sessionId);
        // Refresh sidebar
        fetchSessions();
      }

      const aiMsg: ArchitectMessage = {
        id: 'ai-' + Date.now(),
        role: 'assistant',
        content: responseData.reply,
        source: responseData.source,
        intent: responseData.intent,
        processingTimeMs: responseData.processingTimeMs,
        createdAt: responseData.createdAt,
        steps: responseData.reasoningSteps || []
      };

      setMessages(prev => {
        const withoutTemp = prev.filter(m => m.id !== tempUserMsg.id);
        return [...withoutTemp, { ...tempUserMsg, id: 'user-' + Date.now() }, aiMsg];
      });

    } catch (err: any) {
      setMessages(prev => [
        ...prev,
        {
          id: 'error-' + Date.now(),
          role: 'assistant',
          content: 'Sorry, the AI Architect encountered an error processing your request. Please try again.',
          source: 'ERROR',
          createdAt: new Date().toISOString(),
          steps: []
        }
      ]);
    } finally {
      setIsTyping(false);
    }
  };

  const handleFormSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    handleSend();
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleDeleteSession = async () => {
    if (!sessionToDelete) return;
    try {
      await api(`/api/v1/architect/sessions/${sessionToDelete}`, { method: 'DELETE' });
      setSessions(prev => prev.filter(c => c.id !== sessionToDelete));
      if (activeSessionId === sessionToDelete) {
        setActiveSessionId(null);
        setMessages([]);
        setActiveTitle('New Design Session');
      }
    } catch {
      // Silently fail
    } finally {
      setSessionToDelete(null);
    }
  };

  const toggleStep = (stepName: string) => {
    setExpandedSteps(prev => ({ ...prev, [stepName]: !prev[stepName] }));
  };

  // Find the steps of the last AI message
  const lastAiMessage = messages.slice().reverse().find(m => m.role === 'assistant');
  const activeSteps = lastAiMessage?.steps || [];

  const filteredSessions = sessions.filter(s =>
    !searchQuery || s.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <div className="flex h-[calc(100vh-4rem)] max-w-[1600px] mx-auto w-full p-4 gap-4 overflow-hidden">
      
      {/* ────── 1. Sidebar (Sessions) ────── */}
      <div className="w-[280px] flex-shrink-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden min-h-0 bg-surface-container-lowest/80 backdrop-blur-xl">
        <div className="p-4 border-b border-outline-variant/10">
          <button
            onClick={() => { setActiveSessionId(null); setMessages([]); setActiveTitle('New Design Session'); }}
            disabled={creatingSession}
            className="w-full flex items-center justify-center gap-2 bg-primary-container text-on-primary rounded-lg py-2.5 text-sm font-bold transition-transform hover:scale-[1.02] active:scale-[0.98] disabled:opacity-50 shadow-[0_0_15px_rgba(var(--primary-container),0.3)] hover:shadow-[0_0_25px_rgba(var(--primary-container),0.5)]"
          >
            {creatingSession ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            New System Design
          </button>
        </div>

        <div className="p-3">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50" />
            <input
              type="text"
              placeholder="Search sessions..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-surface-container/50 border border-outline-variant/20 rounded-lg py-2 pl-9 pr-3 text-xs focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto custom-scrollbar p-2 space-y-1">
          {loadingSessions ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-5 h-5 text-primary-container animate-spin" />
            </div>
          ) : filteredSessions.length === 0 ? (
            <div className="text-center py-12 px-4">
              <ListTree className="w-8 h-8 text-[#dee1f7]/15 mx-auto mb-3" />
              <p className="text-xs text-[#dee1f7]/40">No architecture designs yet</p>
            </div>
          ) : (
            filteredSessions.map(session => (
              <div
                key={session.id}
                onClick={() => loadSession(session.id)}
                className={`group w-full py-2.5 px-3 rounded-lg transition-all flex flex-col gap-1 cursor-pointer relative overflow-hidden ${
                  activeSessionId === session.id
                    ? 'bg-surface-container-high border border-outline-variant/20 shadow-sm'
                    : 'hover:bg-surface-container border border-transparent'
                }`}
              >
                {activeSessionId === session.id && (
                  <motion.div layoutId="sidebar-active" className="absolute left-0 top-0 bottom-0 w-1 bg-primary-container" />
                )}
                <div className="flex items-center justify-between w-full">
                  <span className={`text-sm truncate w-[90%] transition-colors ${activeSessionId === session.id ? 'font-bold text-[#e1fdff]' : 'text-[#dee1f7]/80'}`}>
                    {session.title}
                  </span>
                  
                  <button
                    onClick={(e) => { e.stopPropagation(); setSessionToDelete(session.id); }}
                    className="opacity-0 group-hover:opacity-100 p-1 text-on-surface-variant/40 hover:text-red-400 hover:bg-red-400/10 rounded transition-all"
                    title="Delete session"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
                <div className="flex justify-between items-center pl-1 text-[10px] text-on-surface-variant/50">
                  <span className="bg-surface-container-highest px-1.5 py-0.5 rounded text-[9px] uppercase tracking-wider">{session.intent?.replace('_', ' ') || 'DESIGN'}</span>
                  <span>{timeAgo(session.createdAt)}</span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>

      {/* ────── 2. Main Chat Area ────── */}
      <div className="flex-1 flex flex-col min-w-0 glass-card rounded-xl border border-outline-variant/20 overflow-hidden bg-surface bg-opacity-40 relative">
        {/* Header */}
        <div className="h-16 shrink-0 flex items-center px-6 border-b border-outline-variant/10 bg-surface-container-lowest/50 backdrop-blur-md z-10 w-full">
          <div className="flex items-center gap-3">
             <div className="w-8 h-8 rounded-lg bg-primary-container/20 border border-primary-container/30 flex items-center justify-center">
                <ListTree className="w-4 h-4 text-primary-container" />
             </div>
             <div>
               <h1 className="text-base font-bold text-[#e1fdff] leading-none">{activeTitle}</h1>
               <p className="text-[#dee1f7]/50 text-xs mt-1">AI System Architect Workspace</p>
             </div>
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto p-4 md:p-6 space-y-6 custom-scrollbar relative z-0">
          {loadingMessages ? (
            <div className="flex items-center justify-center h-full">
              <Loader2 className="w-6 h-6 text-primary-container animate-spin" />
            </div>
          ) : messages.length === 0 && !isTyping ? (
            <div className="flex flex-col items-center justify-center h-full text-center px-4">
              <div className="w-20 h-20 bg-primary-container/10 rounded-full flex items-center justify-center mb-6">
                 <ListTree className="w-10 h-10 text-primary-container mb-1 ml-1" />
              </div>
              <h2 className="text-2xl font-bold font-headline text-[#e1fdff] mb-2">Build Enterprise Architectures</h2>
              <p className="text-sm text-[#dee1f7]/60 mb-8 max-w-lg leading-relaxed">
                Describe your application requirements (scale, features, domain) and the AI Architect will design the optimal backend system using industry best practices.
              </p>
              
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 w-full max-w-2xl text-left">
                {[
                  "Design a backend for a ride-sharing app with 1M concurrent users, needing real-time location mapping.",
                  "I need an e-commerce platform that can handle flash sales with 10k orders per minute.",
                  "What database should I use for a high-frequency trading platform?",
                  "Analyze the tradeoffs between GraphQL and REST for a mobile-first SaaS platform."
                ].map((prompt, i) => (
                  <button
                    key={i}
                    onClick={() => { setInput(prompt); setTimeout(handleSend, 50); }}
                    className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/20 hover:border-primary-container/50 hover:bg-surface-container transition-all group"
                  >
                    <p className="text-xs text-[#dee1f7]/70 leading-relaxed group-hover:text-[#e1fdff] transition-colors">{prompt}</p>
                  </button>
                ))}
              </div>
            </div>
          ) : (
            <>
              <div className="max-w-4xl mx-auto space-y-6">
                <AnimatePresence mode="popLayout">
                  {messages.map((msg) => (
                    <motion.div
                      key={msg.id}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      className={`flex gap-4 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}
                    >
                      <div className={`w-8 h-8 rounded-lg flex items-center justify-center shrink-0 ${
                        msg.role === 'user'
                          ? 'bg-surface-container-high border border-outline-variant/30 text-[#dee1f7]/80'
                          : 'bg-primary-container/20 border border-primary-container/30 text-primary-container'
                      }`}>
                        {msg.role === 'user' ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                      </div>
                      
                      <div className={`flex flex-col gap-1.5 w-full ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                        {msg.role === 'assistant' && msg.intent && (
                          <div className="text-[10px] text-on-surface-variant/40 uppercase tracking-widest pl-1 font-bold flex gap-2">
                            <span>{msg.intent.replace('_', ' ')}</span>
                            {msg.processingTimeMs && <span>· {(msg.processingTimeMs / 1000).toFixed(1)}s</span>}
                          </div>
                        )}
                        
                        <div className={`w-full max-w-[85%] rounded-xl p-5 text-sm shadow-sm ${
                          msg.role === 'user'
                            ? 'bg-surface-container-highest text-[#dee1f7] rounded-tr-sm border border-outline-variant/10'
                            : 'bg-surface-container-lowest border border-outline-variant/10 text-[#dee1f7]/90 rounded-tl-sm'
                        }`}>
                          {msg.role === 'assistant' ? (
                            <div className="prose prose-invert prose-sm max-w-none prose-p:leading-relaxed prose-headings:font-headline prose-headings:text-[#e1fdff] prose-h2:border-b prose-h2:border-outline-variant/10 prose-h2:pb-2 prose-h2:mt-6 prose-a:text-primary-container prose-strong:text-[#e1fdff] prose-li:text-[#dee1f7]/80">
                              <ReactMarkdown remarkPlugins={[remarkGfm]} components={{ code: CodeBlock }}>
                                {msg.content}
                              </ReactMarkdown>
                            </div>
                          ) : (
                            <p className="whitespace-pre-wrap">{msg.content}</p>
                          )}
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>

                {isTyping && (
                  <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="flex gap-4">
                    <div className="w-8 h-8 rounded-lg bg-primary-container/20 border border-primary-container/30 text-primary-container flex items-center justify-center shrink-0">
                      <Bot className="w-4 h-4" />
                    </div>
                    <div className="rounded-xl px-5 py-6 bg-surface-container-lowest border border-outline-variant/10 rounded-tl-sm flex items-center gap-1.5 shadow-sm">
                      <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce [animation-delay:-0.3s]" />
                      <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce [animation-delay:-0.15s]" />
                      <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce" />
                    </div>
                  </motion.div>
                )}
                <div ref={messagesEndRef} className="h-4" />
              </div>
            </>
          )}
        </div>

        {/* Input Area */}
        <div className="p-4 bg-surface-container-lowest/80 backdrop-blur-md border-t border-outline-variant/10 shrink-0 z-10 w-full relative">
          <div className="max-w-4xl mx-auto">
            <form onSubmit={handleFormSubmit} className="relative flex items-end gap-2">
              <div className="flex-1 relative bg-surface-container border border-outline-variant/20 focus-within:border-primary-container/50 focus-within:shadow-[0_0_15px_rgba(var(--primary-container),0.1)] rounded-2xl transition-all">
                <textarea
                  ref={textareaRef}
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  placeholder="Describe your system requirements or ask an architecture question..."
                  rows={1}
                  className="w-full bg-transparent py-4 pl-5 pr-14 text-sm outline-none placeholder:text-on-surface-variant/40 resize-none custom-scrollbar leading-relaxed block"
                  disabled={isTyping}
                />
                <button
                  type="submit"
                  disabled={!input.trim() || isTyping}
                  className="absolute right-2 bottom-2 p-2 bg-primary-container text-on-primary rounded-xl hover:brightness-110 transition-all disabled:opacity-50 disabled:hover:brightness-100"
                >
                  {isTyping ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
                </button>
              </div>
            </form>
            <div className="text-center mt-2 flex justify-center gap-4 text-[10px] text-on-surface-variant/40 font-label uppercase tracking-widest">
              <span>Shift + Enter for new line</span>
            </div>
          </div>
        </div>
      </div>

      {/* ────── 3. Right Panel (Reasoning visualization) ────── */}
      <div className="w-[320px] flex-shrink-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden min-h-0 bg-surface-container-lowest/60 backdrop-blur-xl">
        <div className="p-4 border-b border-outline-variant/10 bg-surface-container-lowest">
          <h2 className="text-sm font-bold text-[#e1fdff] flex items-center gap-2">
            <Bot className="w-4 h-4 text-primary-container" />
            Agent Reasoning
          </h2>
        </div>

        <div className="flex-1 overflow-y-auto p-4 custom-scrollbar">
          {activeSteps.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-full text-center opacity-50 px-4">
              <CircleDashed className="w-8 h-8 text-on-surface-variant mb-2" />
              <p className="text-xs">Submit a design request to see step-by-step reasoning.</p>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center gap-2 text-xs text-[#00f2ff] font-bold pb-2 border-b border-[#00f2ff]/10">
                <CheckCircle2 className="w-3.5 h-3.5" /> Pipeline Execution
              </div>
              
              <div className="space-y-2 relative before:absolute before:inset-0 before:ml-2.5 before:-translate-x-px md:before:mx-auto md:before:translate-x-0 before:h-full before:w-0.5 before:bg-gradient-to-b before:from-transparent before:via-outline-variant/20 before:to-transparent">
                {activeSteps.map((step, idx) => {
                  const isExpanded = expandedSteps[step.name] || false;
                  return (
                    <motion.div 
                      initial={{ opacity: 0, x: 20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: idx * 0.1 }}
                      key={idx} 
                      className="relative flex items-start flex-col pl-8"
                    >
                      <div className={`absolute left-1.5 -translate-x-1/2 w-2.5 h-2.5 rounded-full border-2 ${
                        step.status === 'COMPLETED' ? 'bg-[#00f2ff] border-surface-container-lowest shadow-[0_0_8px_rgba(0,242,255,0.8)]' : 
                        step.status === 'FAILED' ? 'bg-red-400 border-surface-container-lowest' :
                        'bg-on-surface-variant/30 border-surface-container-lowest'
                      } z-10 top-1.5`} />
                      
                      <button 
                        onClick={() => toggleStep(step.name)}
                        className="w-full text-left bg-surface-container bg-opacity-70 border border-outline-variant/10 rounded-lg p-2.5 hover:bg-surface-container-highest hover:border-primary-container/30 transition-all flex justify-between items-center group shadow-sm z-10"
                      >
                        <div className="flex flex-col gap-0.5">
                          <span className="text-xs font-bold text-[#e1fdff]">{step.name}</span>
                          <span className="text-[9px] text-on-surface-variant/60 font-mono">
                            {step.durationMs}ms
                          </span>
                        </div>
                        {isExpanded ? <ChevronUp className="w-3.5 h-3.5 text-on-surface-variant/50" /> : <ChevronDown className="w-3.5 h-3.5 text-on-surface-variant/50" />}
                      </button>
                      
                      <AnimatePresence>
                        {isExpanded && (
                          <motion.div
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: 'auto', opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                            style={{ overflow: 'hidden' }}
                            className="w-full mt-1 z-10"
                          >
                            <div className="p-3 bg-surface-container-lowest border border-outline-variant/10 rounded-lg text-[11px] text-[#dee1f7]/70 mx-1 mb-1 prose prose-invert prose-sm max-w-none prose-p:leading-snug prose-ul:my-1 prose-li:my-0 shadow-inner">
                              <ReactMarkdown>{step.output || 'No specific output.'}</ReactMarkdown>
                            </div>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </motion.div>
                  );
                })}
              </div>
            </div>
          )}
        </div>
      </div>

      <ConfirmModal
        isOpen={!!sessionToDelete}
        onClose={() => setSessionToDelete(null)}
        onConfirm={handleDeleteSession}
        title="Delete Design Session?"
        description="This will permanently delete this architecture design session. This action cannot be undone."
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
      />
    </div>
  );
}
