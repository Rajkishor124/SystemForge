'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import { Bot, User, Send, MessageSquare, Plus, Search, Pencil, X, MoreVertical, Trash2, Loader2, Sparkles, Copy, Check } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { motion, AnimatePresence } from 'motion/react';
import { api } from '@/lib/api';
import { ConfirmModal } from '@/components/ui/confirm-modal';

// ─── Types ───────────────────────────────────────────────────────────────────

interface ChatMessage {
  id: string;
  role: string;
  content: string;
  source?: string;
  createdAt: string;
}

interface Conversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messageCount: number;
  lastMessagePreview?: string;
}

interface ConversationDetail {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: ChatMessage[];
}

// ─── Suggestion chips ────────────────────────────────────────────────────────

const SUGGESTIONS = [
  { label: 'Database design', prompt: 'How should I design the database schema for my application?' },
  { label: 'Auth architecture', prompt: 'What is the best authentication strategy with JWT and refresh tokens?' },
  { label: 'Scaling to 100k users', prompt: 'How do I scale my backend to handle 100k concurrent users?' },
  { label: 'Microservices vs Monolith', prompt: 'Should I use a microservices or monolith architecture for my MVP?' },
  { label: 'Caching with Redis', prompt: 'How should I implement a Redis caching layer for my API?' },
  { label: 'CI/CD pipeline', prompt: 'How do I set up a CI/CD pipeline with Docker and GitHub Actions?' },
];

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
          customStyle={{ margin: 0, background: 'transparent', padding: '0.75rem', fontSize: '0.8rem' }}
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

export default function ChatPage() {
  // Sidebar state
  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [loadingConvos, setLoadingConvos] = useState(true);
  const [activeConvoId, setActiveConvoId] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');

  // Chat state
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [loadingMessages, setLoadingMessages] = useState(false);
  const [activeTitle, setActiveTitle] = useState('New Conversation');
  const [input, setInput] = useState('');
  const [isTyping, setIsTyping] = useState(false);

  // UI state
  const [editingConvoId, setEditingConvoId] = useState<string | null>(null);
  const [editTitleValue, setEditTitleValue] = useState('');
  const [openDropdownId, setOpenDropdownId] = useState<string | null>(null);
  const [convoToDelete, setConvoToDelete] = useState<string | null>(null);
  const [creatingConvo, setCreatingConvo] = useState(false);

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // ─── Load conversations on mount ─────────────────────────────────────────

  const fetchConversations = useCallback(async () => {
    try {
      const res = await api<Conversation[]>('/api/v1/chat/conversations');
      setConversations(res.data);
      return res.data;
    } catch {
      // Silently handle — user sees empty sidebar
      return [];
    } finally {
      setLoadingConvos(false);
    }
  }, []);

  useEffect(() => { fetchConversations(); }, [fetchConversations]);

  // ─── Load messages when switching conversations ──────────────────────────

  const loadConversation = useCallback(async (convoId: string) => {
    setActiveConvoId(convoId);
    setLoadingMessages(true);
    setMessages([]);

    try {
      const res = await api<ConversationDetail>(`/api/v1/chat/conversations/${convoId}`);
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

  // ─── Auto-resize textarea ────────────────────────────────────────────────

  useEffect(() => {
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
      textareaRef.current.style.height = Math.min(textareaRef.current.scrollHeight, 160) + 'px';
    }
  }, [input]);

  // ─── Create new conversation ─────────────────────────────────────────────

  const createNewConversation = async () => {
    if (creatingConvo) return;
    setCreatingConvo(true);
    try {
      const res = await api<Conversation>('/api/v1/chat/conversations', { method: 'POST' });
      const newConvo = res.data;
      setConversations(prev => [newConvo, ...prev]);
      setActiveConvoId(newConvo.id);
      setActiveTitle(newConvo.title);
      setMessages([]);
    } catch {
      // Silently fail
    } finally {
      setCreatingConvo(false);
    }
  };

  // ─── Send message ────────────────────────────────────────────────────────

  const handleSend = async (messageText?: string) => {
    const text = (messageText || input).trim();
    if (!text || isTyping) return;

    // If no conversation is active, create one first
    let convoId = activeConvoId;
    if (!convoId) {
      try {
        const res = await api<Conversation>('/api/v1/chat/conversations', { method: 'POST' });
        convoId = res.data.id;
        setConversations(prev => [res.data, ...prev]);
        setActiveConvoId(convoId);
        setActiveTitle(res.data.title);
      } catch {
        return;
      }
    }

    // Optimistically add user message
    const tempUserMsg: ChatMessage = {
      id: 'temp-' + Date.now(),
      role: 'user',
      content: text,
      createdAt: new Date().toISOString(),
    };
    setMessages(prev => [...prev, tempUserMsg]);
    setInput('');
    setIsTyping(true);

    try {
      const res = await api<ChatMessage>(`/api/v1/chat/conversations/${convoId}/messages`, {
        method: 'POST',
        body: { message: text },
      });

      // Add AI response
      setMessages(prev => {
        // Replace temp user message with a stable version, then add AI
        const withoutTemp = prev.filter(m => m.id !== tempUserMsg.id);
        return [
          ...withoutTemp,
          { ...tempUserMsg, id: 'user-' + Date.now() },
          res.data,
        ];
      });

      // Auto-title update: refresh sidebar
      const updatedConvos = await fetchConversations();
      // Keep active convo selected
      const updated = updatedConvos.find((c: Conversation) => c.id === convoId);
      if (updated) setActiveTitle(updated.title);
    } catch {
      setMessages(prev => [
        ...prev,
        {
          id: 'error-' + Date.now(),
          role: 'assistant',
          content: 'Sorry, I encountered an error processing your request. Please try again.',
          createdAt: new Date().toISOString(),
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

  // ─── Rename conversation ─────────────────────────────────────────────────

  const saveEdit = async () => {
    if (!editingConvoId || !editTitleValue.trim()) {
      setEditingConvoId(null);
      return;
    }
    try {
      await api(`/api/v1/chat/conversations/${editingConvoId}`, {
        method: 'PUT',
        body: { title: editTitleValue.trim() },
      });
      setConversations(prev => prev.map(c =>
        c.id === editingConvoId ? { ...c, title: editTitleValue.trim() } : c
      ));
      if (activeConvoId === editingConvoId) setActiveTitle(editTitleValue.trim());
    } catch {
      // Silently fail
    } finally {
      setEditingConvoId(null);
    }
  };

  // ─── Delete conversation ─────────────────────────────────────────────────

  const handleDeleteConvo = async () => {
    if (!convoToDelete) return;
    try {
      await api(`/api/v1/chat/conversations/${convoToDelete}`, { method: 'DELETE' });
      setConversations(prev => prev.filter(c => c.id !== convoToDelete));
      if (activeConvoId === convoToDelete) {
        setActiveConvoId(null);
        setMessages([]);
        setActiveTitle('New Conversation');
      }
    } catch {
      // Silently fail
    } finally {
      setConvoToDelete(null);
    }
  };

  // ─── Filtered sidebar ────────────────────────────────────────────────────

  const filteredConversations = conversations.filter(c =>
    !searchQuery || c.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // ─── Render ──────────────────────────────────────────────────────────────

  return (
    <div className="flex h-full max-w-7xl mx-auto w-full p-6 gap-6">
      {/* ────── Sidebar ────── */}
      <div className="w-80 flex-shrink-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden">
        <div className="p-4 border-b border-outline-variant/10 bg-surface-container-lowest">
          <button
            onClick={createNewConversation}
            disabled={creatingConvo}
            className="w-full flex items-center justify-center gap-2 bg-primary-container/10 hover:bg-primary-container/20 text-primary-container border border-primary-container/30 rounded-lg py-2.5 text-sm font-bold transition-colors disabled:opacity-50"
          >
            {creatingConvo ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            New Chat
          </button>
        </div>

        <div className="p-4 border-b border-outline-variant/10">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant/50" />
            <input
              type="text"
              placeholder="Search conversations..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2 pl-9 pr-3 text-xs focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto custom-scrollbar p-2 space-y-1">
          {loadingConvos ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="w-5 h-5 text-primary-container animate-spin" />
            </div>
          ) : filteredConversations.length === 0 ? (
            <div className="text-center py-12 px-4">
              <MessageSquare className="w-8 h-8 text-[#dee1f7]/15 mx-auto mb-3" />
              <p className="text-xs text-[#dee1f7]/40">No conversations yet</p>
            </div>
          ) : (
            filteredConversations.map(convo => (
              <div
                key={convo.id}
                onClick={() => loadConversation(convo.id)}
                className={`group w-full text-left p-3 rounded-lg transition-colors flex flex-col gap-1 cursor-pointer ${
                  activeConvoId === convo.id
                    ? 'bg-surface-container-high border border-outline-variant/20'
                    : 'hover:bg-surface-container border border-transparent'
                }`}
              >
                <div className="flex items-center justify-between w-full">
                  <div className="flex items-center gap-2 overflow-hidden flex-1">
                    <MessageSquare className={`w-4 h-4 shrink-0 ${activeConvoId === convo.id ? 'text-primary-container' : 'text-on-surface-variant/60'}`} />
                    {editingConvoId === convo.id ? (
                      <input
                        type="text"
                        value={editTitleValue}
                        onChange={(e) => setEditTitleValue(e.target.value)}
                        onBlur={saveEdit}
                        onKeyDown={(e) => { if (e.key === 'Enter') saveEdit(); if (e.key === 'Escape') setEditingConvoId(null); }}
                        autoFocus
                        onClick={(e) => e.stopPropagation()}
                        className="flex-1 text-sm bg-surface-container-lowest border border-primary-container/50 rounded px-1 py-0.5 outline-none text-[#e1fdff] min-w-0"
                      />
                    ) : (
                      <span className={`text-sm truncate transition-colors ${activeConvoId === convo.id ? 'font-bold text-[#e1fdff]' : 'text-[#dee1f7]/80'}`}>
                        {convo.title}
                      </span>
                    )}
                  </div>
                  {editingConvoId !== convo.id && (
                    <div className="relative shrink-0 ml-1">
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setOpenDropdownId(openDropdownId === convo.id ? null : convo.id);
                        }}
                        className={`p-1.5 hover:bg-surface-container-highest rounded transition-all ${openDropdownId === convo.id ? 'opacity-100 text-primary-container bg-surface-container-highest' : 'opacity-0 group-hover:opacity-100 text-on-surface-variant/60 hover:text-primary-container'}`}
                      >
                        <MoreVertical className="w-4 h-4" />
                      </button>
                      {openDropdownId === convo.id && (
                        <>
                          <div className="fixed inset-0 z-40" onClick={(e) => { e.stopPropagation(); setOpenDropdownId(null); }} />
                          <div className="absolute right-0 top-full mt-1 w-36 bg-surface-container-high border border-outline-variant/20 rounded-lg shadow-xl z-50 overflow-hidden py-1">
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setEditingConvoId(convo.id);
                                setEditTitleValue(convo.title);
                                setOpenDropdownId(null);
                              }}
                              className="w-full text-left px-3 py-2 text-xs text-[#dee1f7] hover:bg-surface-container-highest flex items-center gap-2 transition-colors"
                            >
                              <Pencil className="w-3 h-3" /> Rename
                            </button>
                            <button
                              onClick={(e) => {
                                e.stopPropagation();
                                setConvoToDelete(convo.id);
                                setOpenDropdownId(null);
                              }}
                              className="w-full text-left px-3 py-2 text-xs text-red-400 hover:bg-red-500/10 flex items-center gap-2 transition-colors"
                            >
                              <Trash2 className="w-3 h-3" /> Delete
                            </button>
                          </div>
                        </>
                      )}
                    </div>
                  )}
                </div>
                <span className="text-[10px] text-on-surface-variant/50 pl-6">{timeAgo(convo.createdAt)}</span>
              </div>
            ))
          )}
        </div>
      </div>

      {/* ────── Main Chat Area ────── */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Header */}
        <div className="mb-4 shrink-0 flex justify-between items-center">
          <div>
            <h1 className="text-2xl font-bold tracking-tight font-headline text-[#e1fdff] mb-0.5">{activeTitle}</h1>
            <p className="text-[#dee1f7]/60 text-xs">Chat with the engine to refine your system design.</p>
          </div>
        </div>

        <div className="flex-1 glass-card rounded-xl border border-outline-variant/20 flex flex-col overflow-hidden min-h-0 relative">
          {/* Messages */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
            {loadingMessages ? (
              <div className="flex items-center justify-center h-full">
                <Loader2 className="w-6 h-6 text-primary-container animate-spin" />
              </div>
            ) : messages.length === 0 && !isTyping ? (
              /* ─── Empty state with suggestions ─── */
              <div className="flex flex-col items-center justify-center h-full text-center px-4">
                <div className="relative mb-6">
                  <div className="absolute inset-0 bg-[#00f2ff]/10 blur-2xl rounded-full" />
                  <Sparkles className="w-14 h-14 text-[#00f2ff]/40 relative z-10" />
                </div>
                <h2 className="text-xl font-bold font-headline text-[#e1fdff] mb-2">SystemForge AI</h2>
                <p className="text-sm text-[#dee1f7]/50 mb-8 max-w-md leading-relaxed">
                  Your expert architecture assistant. Ask about database design, scaling strategies, authentication flows, and more.
                </p>
                <div className="grid grid-cols-2 gap-3 w-full max-w-lg">
                  {SUGGESTIONS.map((s, i) => (
                    <button
                      key={i}
                      onClick={() => handleSend(s.prompt)}
                      className="text-left p-3.5 rounded-xl bg-surface-container-lowest border border-outline-variant/10 hover:border-primary-container/30 hover:bg-surface-container transition-all group"
                    >
                      <span className="text-xs font-bold text-[#e1fdff] group-hover:text-[#00f2ff] transition-colors">{s.label}</span>
                      <p className="text-[10px] text-[#dee1f7]/40 mt-1 line-clamp-2">{s.prompt}</p>
                    </button>
                  ))}
                </div>
              </div>
            ) : (
              <>
                <AnimatePresence mode="popLayout">
                  {messages.map((msg) => (
                    <motion.div
                      key={msg.id}
                      initial={{ opacity: 0, y: 10 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ duration: 0.3 }}
                      className={`flex gap-4 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}
                    >
                      <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${
                        msg.role === 'user'
                          ? 'bg-surface-container-high border border-outline-variant/30'
                          : 'bg-primary-container/20 text-primary-container'
                      }`}>
                        {msg.role === 'user' ? <User className="w-4 h-4 text-[#dee1f7]/80" /> : <Bot className="w-4 h-4" />}
                      </div>
                      <div className={`max-w-[80%] flex flex-col gap-1 ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                        <div className={`rounded-2xl p-4 text-sm ${
                          msg.role === 'user'
                            ? 'bg-surface-container-high text-[#dee1f7] rounded-tr-sm'
                            : 'bg-surface-container-lowest border border-outline-variant/10 text-[#dee1f7]/90 rounded-tl-sm'
                        }`}>
                          {msg.role === 'assistant' ? (
                            <div className="prose prose-invert prose-sm max-w-none prose-p:leading-relaxed prose-pre:bg-surface-container-high prose-pre:border prose-pre:border-outline-variant/20 prose-pre:rounded-lg">
                              <ReactMarkdown
                                remarkPlugins={[remarkGfm]}
                                components={{ code: CodeBlock }}
                              >
                                {msg.content}
                              </ReactMarkdown>
                            </div>
                          ) : (
                            <p className="whitespace-pre-wrap">{msg.content}</p>
                          )}
                        </div>
                        <div className="flex items-center gap-2 px-1">
                          <span className="text-[10px] text-on-surface-variant/50">{formatTime(msg.createdAt)}</span>
                          {msg.source && (
                            <span className="text-[9px] text-on-surface-variant/30 uppercase tracking-wider">
                              {msg.source === 'AI' ? '⚡ AI' : '🔧 Engine'}
                            </span>
                          )}
                        </div>
                      </div>
                    </motion.div>
                  ))}
                </AnimatePresence>
                {isTyping && (
                  <motion.div
                    initial={{ opacity: 0, y: 10 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="flex gap-4"
                  >
                    <div className="w-8 h-8 rounded-full flex items-center justify-center shrink-0 bg-primary-container/20 text-primary-container">
                      <Bot className="w-4 h-4" />
                    </div>
                    <div className="rounded-2xl px-4 py-5 bg-surface-container-lowest border border-outline-variant/10 rounded-tl-sm flex items-center gap-1.5">
                      <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce [animation-delay:-0.3s]" />
                      <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce [animation-delay:-0.15s]" />
                      <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce" />
                    </div>
                  </motion.div>
                )}
              </>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <div className="p-4 bg-surface-container-lowest border-t border-outline-variant/10 shrink-0">
            <form onSubmit={handleFormSubmit} className="relative flex items-end gap-2 rounded-2xl">
              <textarea
                ref={textareaRef}
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Ask about database design, scaling strategies, architecture patterns..."
                rows={1}
                className="flex-1 bg-surface-container border border-outline-variant/20 rounded-2xl py-3 pl-5 pr-4 text-sm focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/40 resize-none custom-scrollbar leading-relaxed"
                disabled={isTyping}
              />
              <button
                type="submit"
                disabled={!input.trim() || isTyping}
                className="p-3 bg-primary-container text-on-primary rounded-xl hover:brightness-110 transition-all disabled:opacity-50 disabled:hover:brightness-100 shrink-0"
              >
                {isTyping ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              </button>
            </form>
            <div className="text-center mt-2">
              <span className="text-[10px] text-on-surface-variant/40 font-label uppercase tracking-widest">
                SystemForge AI · Verify critical architecture decisions · <span className="text-on-surface-variant/25">Shift+Enter for new line</span>
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={!!convoToDelete}
        onClose={() => setConvoToDelete(null)}
        onConfirm={handleDeleteConvo}
        title="Delete Conversation?"
        description="This will permanently delete the conversation and all its messages. This action cannot be undone."
        confirmLabel="Delete"
        cancelLabel="Cancel"
        variant="danger"
      />
    </div>
  );
}
