'use client';

import { useState, useRef, useEffect } from 'react';
import { Bot, User, Send, Paperclip, MessageSquare, Plus, Search, Pencil, X, MoreVertical, Trash2, Loader2 } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import { api } from '@/lib/api';

// ─── Types ───────────────────────────────────────────────────────────────────

type Message = {
  role: string;
  content: string;
  timestamp: string;
  source?: string; // "AI" | "RULE_ENGINE"
};

type Chat = {
  id: number;
  title: string;
  date: string;
  messages: Message[];
};

export default function ChatPage() {
  const [activeChatId, setActiveChatId] = useState(1);
  const [input, setInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [editingChatId, setEditingChatId] = useState<number | null>(null);
  const [editTitleValue, setEditTitleValue] = useState('');
  const [openDropdownId, setOpenDropdownId] = useState<number | string | null>(null);
  const [chatToDelete, setChatToDelete] = useState<number | null>(null);
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const [chatHistory, setChatHistory] = useState<Chat[]>([
    {
      id: 1,
      title: 'New Conversation',
      date: 'Today',
      messages: [
        { role: 'assistant', content: 'Hello! I\'m your **SystemForge AI** assistant. I can help you design scalable, production-ready backend systems.\n\nAsk me about:\n- Architecture patterns\n- Database design\n- Scaling strategies\n- Authentication flows\n- API design\n- Cloud infrastructure\n\nHow can I help you today?', timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }
      ]
    }
  ]);

  const activeChat = chatHistory.find(chat => chat.id === activeChatId) || chatHistory[0];

  // Auto-scroll to bottom on new messages
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [activeChat.messages, isTyping]);

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isTyping) return;

    const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userMessage = input.trim();

    // Add user message immediately
    const newMessages: Message[] = [...activeChat.messages, {
      role: 'user',
      content: userMessage,
      timestamp: currentTime,
    }];

    setChatHistory(prev => prev.map(chat =>
      chat.id === activeChatId ? { ...chat, messages: newMessages } : chat
    ));

    setInput('');
    setIsTyping(true);

    // Auto-set title from first user message
    if (activeChat.messages.filter(m => m.role === 'user').length === 0) {
      const title = userMessage.length > 40 ? userMessage.substring(0, 40) + '...' : userMessage;
      setChatHistory(prev => prev.map(chat =>
        chat.id === activeChatId ? { ...chat, title } : chat
      ));
    }

    try {
      // Build conversation history for context
      const history = newMessages
        .filter(m => m.content)
        .slice(-10) // Last 10 messages for context
        .map(m => ({ role: m.role, content: m.content }));

      const res = await api<{ reply: string; source: string }>('/api/v1/chat', {
        method: 'POST',
        body: {
          message: userMessage,
          history: history.slice(0, -1), // Exclude the current message from history
        },
      });

      const aiTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

      setChatHistory(prev => prev.map(chat => {
        if (chat.id === activeChatId) {
          return {
            ...chat,
            messages: [...chat.messages, {
              role: 'assistant',
              content: res.data.reply,
              timestamp: aiTime,
              source: res.data.source,
            }]
          };
        }
        return chat;
      }));
    } catch {
      const aiTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      setChatHistory(prev => prev.map(chat => {
        if (chat.id === activeChatId) {
          return {
            ...chat,
            messages: [...chat.messages, {
              role: 'assistant',
              content: 'Sorry, I encountered an error processing your request. Please try again.',
              timestamp: aiTime,
            }]
          };
        }
        return chat;
      }));
    } finally {
      setIsTyping(false);
    }
  };

  const createNewChat = () => {
    const newId = chatHistory.length > 0 ? Math.max(...chatHistory.map(c => c.id)) + 1 : 1;
    const newChat: Chat = {
      id: newId,
      title: 'New Conversation',
      date: 'Just now',
      messages: [
        { role: 'assistant', content: 'Hello! How can I help you design your system today?', timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }
      ]
    };
    setChatHistory([newChat, ...chatHistory]);
    setActiveChatId(newId);
  };

  const filteredChatHistory = chatHistory.filter(chat =>
    chat.title.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const requestDeleteChat = (id: number) => {
    setChatToDelete(id);
    setOpenDropdownId(null);
  };

  const confirmDeleteChat = () => {
    if (chatToDelete === null) return;
    setChatHistory(prev => {
      const newHistory = prev.filter(chat => chat.id !== chatToDelete);
      if (activeChatId === chatToDelete) {
        if (newHistory.length > 0) {
          setActiveChatId(newHistory[0].id);
        } else {
          const newId = Date.now();
          const newChat: Chat = {
            id: newId,
            title: 'New Conversation',
            date: 'Just now',
            messages: []
          };
          setActiveChatId(newId);
          return [newChat];
        }
      }
      return newHistory;
    });
    setChatToDelete(null);
  };

  const cancelDeleteChat = () => {
    setChatToDelete(null);
  };

  const saveEdit = () => {
    if (editingChatId !== null) {
      setChatHistory(prev => prev.map(chat =>
        chat.id === editingChatId ? { ...chat, title: editTitleValue.trim() || 'Untitled Chat' } : chat
      ));
      setEditingChatId(null);
    }
  };

  const handleEditKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      saveEdit();
    } else if (e.key === 'Escape') {
      setEditingChatId(null);
    }
  };

  return (
    <div className="flex h-full max-w-7xl mx-auto w-full p-6 gap-6">
      {/* Sidebar - Chat History */}
      <div className="w-80 flex-shrink-0 flex flex-col glass-card rounded-xl border border-outline-variant/20 overflow-hidden">
        <div className="p-4 border-b border-outline-variant/10 bg-surface-container-lowest">
          <button
            onClick={createNewChat}
            className="w-full flex items-center justify-center gap-2 bg-primary-container/10 hover:bg-primary-container/20 text-primary-container border border-primary-container/30 rounded-lg py-2.5 text-sm font-bold transition-colors"
          >
            <Plus className="w-4 h-4" /> New Chat
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
          {filteredChatHistory.map(chat => (
            <div
              key={chat.id}
              onClick={() => setActiveChatId(chat.id)}
              className={`group w-full text-left p-3 rounded-lg transition-colors flex flex-col gap-1 cursor-pointer ${
                activeChatId === chat.id
                  ? 'bg-surface-container-high border border-outline-variant/20'
                  : 'hover:bg-surface-container border border-transparent'
              }`}
            >
              <div className="flex items-center justify-between w-full">
                <div className="flex items-center gap-2 overflow-hidden flex-1">
                  <MessageSquare className={`w-4 h-4 shrink-0 ${activeChatId === chat.id ? 'text-primary-container' : 'text-on-surface-variant/60'}`} />
                  {editingChatId === chat.id ? (
                    <input
                      type="text"
                      value={editTitleValue}
                      onChange={(e) => setEditTitleValue(e.target.value)}
                      onBlur={saveEdit}
                      onKeyDown={handleEditKeyDown}
                      autoFocus
                      onClick={(e) => e.stopPropagation()}
                      className="flex-1 text-sm bg-surface-container-lowest border border-primary-container/50 rounded px-1 py-0.5 outline-none text-[#e1fdff] min-w-0"
                    />
                  ) : (
                    <span
                      className={`text-sm truncate transition-colors ${activeChatId === chat.id ? 'font-bold text-[#e1fdff]' : 'text-[#dee1f7]/80'}`}
                    >
                      {chat.title}
                    </span>
                  )}
                </div>
                {editingChatId !== chat.id && (
                  <div className="relative shrink-0 ml-1">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setOpenDropdownId(openDropdownId === chat.id ? null : chat.id);
                      }}
                      className={`p-1.5 hover:bg-surface-container-highest rounded transition-all ${openDropdownId === chat.id ? 'opacity-100 text-primary-container bg-surface-container-highest' : 'opacity-0 group-hover:opacity-100 text-on-surface-variant/60 hover:text-primary-container'}`}
                      title="More options"
                    >
                      <MoreVertical className="w-4 h-4" />
                    </button>

                    {openDropdownId === chat.id && (
                      <>
                        <div
                          className="fixed inset-0 z-40"
                          onClick={(e) => {
                            e.stopPropagation();
                            setOpenDropdownId(null);
                          }}
                        />
                        <div className="absolute right-0 top-full mt-1 w-36 bg-surface-container-high border border-outline-variant/20 rounded-lg shadow-xl z-50 overflow-hidden py-1">
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              setActiveChatId(chat.id);
                              setEditingChatId(chat.id);
                              setEditTitleValue(chat.title);
                              setOpenDropdownId(null);
                            }}
                            className="w-full text-left px-3 py-2 text-xs text-[#dee1f7] hover:bg-surface-container-highest flex items-center gap-2 transition-colors"
                          >
                            <Pencil className="w-3 h-3" /> Edit Title
                          </button>
                          <button
                            onClick={(e) => {
                              e.stopPropagation();
                              requestDeleteChat(chat.id);
                            }}
                            className="w-full text-left px-3 py-2 text-xs text-red-400 hover:bg-red-500/10 flex items-center gap-2 transition-colors"
                          >
                            <Trash2 className="w-3 h-3" /> Delete Chat
                          </button>
                        </div>
                      </>
                    )}
                  </div>
                )}
              </div>
              <span className="text-[10px] text-on-surface-variant/50 pl-6">{chat.date}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col min-w-0">
        <div className="mb-6 shrink-0 flex justify-between items-start">
          <div className="flex-1 mr-4">
            {editingChatId === activeChat.id ? (
              <input
                type="text"
                value={editTitleValue}
                onChange={(e) => setEditTitleValue(e.target.value)}
                onBlur={saveEdit}
                onKeyDown={handleEditKeyDown}
                autoFocus
                className="text-2xl font-bold tracking-tight font-headline bg-surface-container-lowest border border-primary-container/50 rounded px-2 py-0.5 outline-none text-[#e1fdff] mb-1 w-full max-w-md"
              />
            ) : (
              <h1 className="text-2xl font-bold tracking-tight font-headline text-[#e1fdff] mb-1">{activeChat.title}</h1>
            )}
            <p className="text-[#dee1f7]/60 text-xs">Chat with the engine to refine your system design.</p>
          </div>

          <div className="relative shrink-0">
            <button
              onClick={() => setOpenDropdownId(openDropdownId === 'main' ? null : 'main')}
              className={`p-2 rounded-lg transition-all ${openDropdownId === 'main' ? 'bg-surface-container-high text-primary-container' : 'text-on-surface-variant/60 hover:bg-surface-container-high hover:text-primary-container'}`}
              title="Chat options"
            >
              <MoreVertical className="w-5 h-5" />
            </button>

            {openDropdownId === 'main' && (
              <>
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setOpenDropdownId(null)}
                />
                <div className="absolute right-0 top-full mt-1 w-40 bg-surface-container-high border border-outline-variant/20 rounded-lg shadow-xl z-50 overflow-hidden py-1">
                  <button
                    onClick={() => {
                      setEditingChatId(activeChat.id);
                      setEditTitleValue(activeChat.title);
                      setOpenDropdownId(null);
                    }}
                    className="w-full text-left px-4 py-2.5 text-sm text-[#dee1f7] hover:bg-surface-container-highest flex items-center gap-2 transition-colors"
                  >
                    <Pencil className="w-4 h-4" /> Edit Title
                  </button>
                  <button
                    onClick={() => {
                      requestDeleteChat(activeChat.id);
                    }}
                    className="w-full text-left px-4 py-2.5 text-sm text-red-400 hover:bg-red-500/10 flex items-center gap-2 transition-colors"
                  >
                    <Trash2 className="w-4 h-4" /> Delete Chat
                  </button>
                </div>
              </>
            )}
          </div>
        </div>

        <div className="flex-1 glass-card rounded-xl border border-outline-variant/20 flex flex-col overflow-hidden min-h-0 relative">
          {/* Chat Messages */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
            {activeChat.messages.map((msg, i) => (
              <div key={i} className={`flex gap-4 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${msg.role === 'user' ? 'bg-surface-container-high border border-outline-variant/30' : 'bg-primary-container/20 text-primary-container'}`}>
                  {msg.role === 'user' ? <User className="w-4 h-4 text-[#dee1f7]/80" /> : <Bot className="w-4 h-4" />}
                </div>
                <div className={`max-w-[80%] flex flex-col gap-1 ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                  <div className={`rounded-2xl p-4 text-sm ${msg.role === 'user' ? 'bg-surface-container-high text-[#dee1f7] rounded-tr-sm' : 'bg-surface-container-lowest border border-outline-variant/10 text-[#dee1f7]/90 rounded-tl-sm'}`}>
                    {msg.role === 'assistant' ? (
                      <div className="prose prose-invert prose-sm max-w-none prose-p:leading-relaxed prose-pre:bg-surface-container-high prose-pre:border prose-pre:border-outline-variant/20">
                        <ReactMarkdown
                          remarkPlugins={[remarkGfm]}
                          components={{
                            code({className, children, ...props}: any) {
                              const match = /language-(\w+)/.exec(className || '');
                              return match ? (
                                <SyntaxHighlighter
                                  {...props}
                                  style={vscDarkPlus}
                                  language={match[1]}
                                  PreTag="div"
                                  customStyle={{ margin: 0, background: 'transparent', padding: 0 }}
                                >
                                  {String(children).replace(/\n$/, '')}
                                </SyntaxHighlighter>
                              ) : (
                                <code {...props} className={`${className || ''} bg-surface-container-high px-1.5 py-0.5 rounded-md text-primary-container font-mono text-[0.85em]`}>
                                  {children}
                                </code>
                              );
                            }
                          }}
                        >
                          {msg.content}
                        </ReactMarkdown>
                      </div>
                    ) : (
                      msg.content
                    )}
                  </div>
                  <div className="flex items-center gap-2 px-1">
                    {msg.timestamp && (
                      <span className="text-[10px] text-on-surface-variant/50">{msg.timestamp}</span>
                    )}
                    {msg.source && (
                      <span className="text-[9px] text-on-surface-variant/30 uppercase tracking-wider">{msg.source === 'AI' ? '⚡ AI' : '🔧 Engine'}</span>
                    )}
                  </div>
                </div>
              </div>
            ))}
            {isTyping && (
              <div className="flex gap-4">
                <div className="w-8 h-8 rounded-full flex items-center justify-center shrink-0 bg-primary-container/20 text-primary-container">
                  <Bot className="w-4 h-4" />
                </div>
                <div className="max-w-[80%] flex flex-col gap-1 items-start">
                  <div className="rounded-2xl px-4 py-5 text-sm bg-surface-container-lowest border border-outline-variant/10 text-[#dee1f7]/90 rounded-tl-sm flex items-center gap-1.5 h-[52px]">
                    <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce [animation-delay:-0.3s]"></span>
                    <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce [animation-delay:-0.15s]"></span>
                    <span className="w-1.5 h-1.5 bg-primary-container rounded-full animate-bounce"></span>
                  </div>
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <div className="p-4 bg-surface-container-lowest border-t border-outline-variant/10 shrink-0">
            <form onSubmit={handleSend} className="relative flex items-center rounded-2xl">
              <input
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder="Ask about database design, scaling strategies, architecture patterns..."
                className="w-full bg-surface-container border border-outline-variant/20 rounded-2xl py-3 pl-5 pr-14 text-sm focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
                disabled={isTyping}
              />
              <button
                type="submit"
                disabled={!input.trim() || isTyping}
                className="absolute right-2 p-2 bg-primary-container text-on-primary rounded-xl hover:brightness-110 transition-all disabled:opacity-50 disabled:hover:brightness-100 z-10"
              >
                {isTyping ? <Loader2 className="w-4 h-4 animate-spin" /> : <Send className="w-4 h-4" />}
              </button>
            </form>
            <div className="text-center mt-2">
              <span className="text-[10px] text-on-surface-variant/40 font-label uppercase tracking-widest">SystemForge AI · Verify critical architecture decisions</span>
            </div>
          </div>
        </div>
      </div>

      {/* Delete Confirmation Modal */}
      {chatToDelete !== null && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
          <div className="bg-surface-container-high border border-outline-variant/20 rounded-xl p-6 max-w-sm w-full shadow-2xl">
            <h3 className="text-lg font-bold text-[#e1fdff] mb-2 font-headline">Delete Chat</h3>
            <p className="text-sm text-[#dee1f7]/70 mb-6">
              Are you sure you want to delete this chat? This action cannot be undone.
            </p>
            <div className="flex justify-end gap-3">
              <button
                onClick={cancelDeleteChat}
                className="px-4 py-2 rounded-lg text-sm font-bold text-[#dee1f7] hover:bg-surface-container-highest transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={confirmDeleteChat}
                className="px-4 py-2 rounded-lg text-sm font-bold bg-red-500/20 text-red-400 hover:bg-red-500/30 transition-colors"
              >
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
