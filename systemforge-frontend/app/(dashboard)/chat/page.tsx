'use client';

import { useState, useRef, useEffect } from 'react';
import { Bot, User, Send, Paperclip, MessageSquare, Plus, Search, Pencil, X, FileText, Image as ImageIcon, MoreVertical, Trash2, FileCode, FileArchive, FileSpreadsheet, FileAudio, FileVideo, File } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { vscDarkPlus } from 'react-syntax-highlighter/dist/esm/styles/prism';
import Image from 'next/image';

type Attachment = {
  name: string;
  type: string;
  url: string;
};

type Message = {
  role: string;
  content: string;
  timestamp: string;
  attachments?: Attachment[];
};

type Chat = {
  id: number;
  title: string;
  date: string;
  messages: Message[];
};

const getFileIcon = (type: string, name: string, className: string = "w-6 h-6 text-primary-container shrink-0") => {
  const t = type.toLowerCase();
  const n = name.toLowerCase();
  if (t.startsWith('audio/')) return <FileAudio className={className} />;
  if (t.startsWith('video/')) return <FileVideo className={className} />;
  if (t.includes('spreadsheet') || t.includes('csv') || t.includes('excel') || n.endsWith('.csv') || n.endsWith('.xlsx')) return <FileSpreadsheet className={className} />;
  if (t.includes('zip') || t.includes('tar') || t.includes('rar') || t.includes('archive') || n.endsWith('.zip') || n.endsWith('.tar.gz')) return <FileArchive className={className} />;
  if (t.includes('json') || t.includes('javascript') || t.includes('html') || t.includes('css') || n.match(/\.(js|ts|jsx|tsx|json|html|css|py|java|c|cpp|go|rs)$/i)) return <FileCode className={className} />;
  if (t.includes('pdf') || t.includes('text')) return <FileText className={className} />;
  return <File className={className} />;
};

const getFileExtension = (name: string) => {
  const parts = name.split('.');
  return parts.length > 1 ? parts.pop()?.toUpperCase() : 'FILE';
};

function AttachmentPreview({ file, onRemove }: { file: File; onRemove: () => void }) {
  const isImage = file.type.startsWith('image/');
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  useEffect(() => {
    if (!isImage) return;
    
    const url = URL.createObjectURL(file);
    
    // Use setTimeout to avoid synchronous setState in effect warning
    const timer = setTimeout(() => {
      setPreviewUrl(url);
    }, 0);
    
    return () => {
      clearTimeout(timer);
      URL.revokeObjectURL(url);
    };
  }, [file, isImage]);

  return (
    <div className="relative group flex flex-col items-center justify-center bg-surface-container-high border border-outline-variant/20 rounded-lg overflow-hidden w-20 h-20 shrink-0 shadow-sm">
      {isImage && previewUrl ? (
        <div className="relative w-full h-full">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={previewUrl} alt={file.name} className="object-cover w-full h-full" />
        </div>
      ) : (
        <div className="flex flex-col items-center justify-center w-full h-full p-2 gap-1 bg-surface-container-highest relative group/file" title={file.name}>
          <div className="relative">
            {getFileIcon(file.type, file.name, "w-6 h-6 text-primary-container shrink-0 mb-0.5")}
            <div className="absolute -bottom-1 -right-2 bg-surface-container-high border border-outline-variant/30 rounded px-1 py-0.5">
              <span className="text-[7px] font-bold text-primary-container leading-none block">{getFileExtension(file.name)}</span>
            </div>
          </div>
          <span className="text-[9px] font-medium text-[#dee1f7] text-center w-full truncate px-1 mt-1">{file.name}</span>
        </div>
      )}
      
      {isImage && (
        <div className="absolute inset-x-0 bottom-0 bg-black/60 p-1 translate-y-full group-hover:translate-y-0 transition-transform">
          <span className="text-[9px] text-white block truncate text-center">{file.name}</span>
        </div>
      )}

      <button 
        type="button"
        onClick={onRemove}
        className="absolute top-1 right-1 p-1 bg-black/60 text-white rounded-full hover:bg-red-500 transition-colors opacity-0 group-hover:opacity-100"
      >
        <X className="w-3 h-3" />
      </button>
    </div>
  );
}

export default function ChatPage() {
  const [activeChatId, setActiveChatId] = useState(1);
  const [input, setInput] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [editingChatId, setEditingChatId] = useState<number | null>(null);
  const [editTitleValue, setEditTitleValue] = useState('');
  const [openDropdownId, setOpenDropdownId] = useState<number | string | null>(null);
  const [chatToDelete, setChatToDelete] = useState<number | null>(null);
  const [isTyping, setIsTyping] = useState(false);
  const [attachments, setAttachments] = useState<File[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [dragCounter, setDragCounter] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [chatHistory, setChatHistory] = useState<Chat[]>([
    {
      id: 1,
      title: 'Current Architecture Refinement',
      date: 'Today',
      messages: [
        { role: 'assistant', content: 'Hello! I am your SystemForge AI assistant. How can I help you refine your architecture today?', timestamp: '10:00 AM' }
      ]
    },
    {
      id: 2,
      title: 'Database Scaling Strategy',
      date: 'Yesterday',
      messages: [
        { role: 'user', content: 'How should I scale my PostgreSQL database for 10k concurrent reads?', timestamp: '2:30 PM' },
        { role: 'assistant', content: 'For 10k concurrent reads, I recommend implementing a **read replica architecture** with `PgBouncer` for connection pooling.\n\nAdditionally, adding a Redis caching layer for frequently accessed data will significantly reduce the load on your primary database.', timestamp: '2:31 PM' }
      ]
    },
    {
      id: 3,
      title: 'Microservices vs Monolith',
      date: 'Mar 15, 2024',
      messages: [
        { role: 'user', content: 'Should I start with microservices or a monolith for my new e-commerce app?', timestamp: '11:15 AM' },
        { role: 'assistant', content: 'For a new e-commerce application, starting with a modular monolith is often the best approach. It allows for faster initial development and easier deployment. Once specific domains (like inventory or payments) require independent scaling, you can extract them into microservices.', timestamp: '11:16 AM' }
      ]
    }
  ]);

  const activeChat = chatHistory.find(chat => chat.id === activeChatId) || chatHistory[0];

  const handleDragEnter = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter(prev => prev + 1);
    setIsDragging(true);
  };

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter(prev => prev - 1);
    if (dragCounter - 1 === 0) {
      setIsDragging(false);
    }
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragCounter(0);
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
      const newFiles = Array.from(e.dataTransfer.files);
      setAttachments(prev => [...prev, ...newFiles]);
    }
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const newFiles = Array.from(e.target.files);
      setAttachments(prev => [...prev, ...newFiles]);
    }
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const removeAttachment = (index: number) => {
    setAttachments(prev => prev.filter((_, i) => i !== index));
  };

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() && attachments.length === 0) return;

    const currentTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    const userMessage = input;
    
    const messageAttachments = attachments.map(file => ({
      name: file.name,
      type: file.type,
      url: URL.createObjectURL(file)
    }));

    const newMessages = [...activeChat.messages, { 
      role: 'user', 
      content: userMessage, 
      timestamp: currentTime,
      attachments: messageAttachments
    }];
    
    setChatHistory(prev => prev.map(chat => 
      chat.id === activeChatId ? { ...chat, messages: newMessages } : chat
    ));
    
    setInput('');
    setAttachments([]);
    setIsTyping(true);

    // Simulate AI response with a random delay between 1 and 3 seconds
    const delay = Math.floor(Math.random() * 2000) + 1000;
    setTimeout(() => {
      const aiTime = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
      const responses = [
        "That's an interesting approach. Have you considered the trade-offs with **data consistency**?",
        "I can help you design that. Let's break down the components:\n\n1. API Gateway\n2. Auth Service\n3. Core Domain",
        "Based on best practices, I'd recommend using a message broker like `Kafka` or `RabbitMQ` for that.",
        "Could you elaborate on the expected traffic volume for this service?",
        `Regarding "${userMessage}", I suggest we start by defining the core domain models.`
      ];
      const randomResponse = responses[Math.floor(Math.random() * responses.length)];

      setChatHistory(prev => prev.map(chat => {
        if (chat.id === activeChatId) {
          return {
            ...chat,
            messages: [...chat.messages, { 
              role: 'assistant', 
              content: randomResponse,
              timestamp: aiTime
            }]
          };
        }
        return chat;
      }));
      setIsTyping(false);
    }, delay);
  };

  const createNewChat = () => {
    const newId = chatHistory.length > 0 ? Math.max(...chatHistory.map(c => c.id)) + 1 : 1;
    const newChat = {
      id: newId,
      title: 'New Conversation',
      date: 'Just now',
      messages: []
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
          const newChat = {
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

        <div 
          className="flex-1 glass-card rounded-xl border border-outline-variant/20 flex flex-col overflow-hidden min-h-0 relative"
          onDragEnter={handleDragEnter}
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
        >
          {isDragging && (
            <div className="absolute inset-0 z-50 bg-surface-container-highest/80 backdrop-blur-sm flex items-center justify-center border-2 border-dashed border-primary-container rounded-xl">
              <div className="flex flex-col items-center gap-4 pointer-events-none">
                <div className="w-16 h-16 rounded-full bg-primary-container/20 flex items-center justify-center">
                  <Paperclip className="w-8 h-8 text-primary-container" />
                </div>
                <h3 className="text-xl font-bold text-[#e1fdff] font-headline">Drop files to attach</h3>
                <p className="text-[#dee1f7]/70 text-sm">Images, documents, and code files are supported</p>
              </div>
            </div>
          )}
          {/* Chat Messages */}
          <div className="flex-1 overflow-y-auto p-6 space-y-6 custom-scrollbar">
            {activeChat.messages.map((msg, i) => (
              <div key={i} className={`flex gap-4 ${msg.role === 'user' ? 'flex-row-reverse' : ''}`}>
                <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${msg.role === 'user' ? 'bg-surface-container-high border border-outline-variant/30' : 'bg-primary-container/20 text-primary-container'}`}>
                  {msg.role === 'user' ? <User className="w-4 h-4 text-[#dee1f7]/80" /> : <Bot className="w-4 h-4" />}
                </div>
                <div className={`max-w-[80%] flex flex-col gap-1 ${msg.role === 'user' ? 'items-end' : 'items-start'}`}>
                  {msg.attachments && msg.attachments.length > 0 && (
                    <div className="flex flex-wrap gap-2 mb-1 justify-end">
                      {msg.attachments.map((att, idx) => (
                        <div key={idx} className="relative group overflow-hidden rounded-lg border border-outline-variant/20 bg-surface-container-high">
                          {att.type.startsWith('image/') ? (
                            <div className="relative w-32 h-32">
                              <Image src={att.url} alt={att.name} fill className="object-cover" />
                            </div>
                          ) : (
                            <div className="flex items-center gap-2 p-3 w-48">
                              {getFileIcon(att.type, att.name)}
                              <span className="text-xs text-[#dee1f7] truncate">{att.name}</span>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                  <div className={`rounded-2xl p-4 text-sm ${msg.role === 'user' ? 'bg-surface-container-high text-[#dee1f7] rounded-tr-sm' : 'bg-surface-container-lowest border border-outline-variant/10 text-[#dee1f7]/90 rounded-tl-sm'}`}>
                    {msg.role === 'assistant' ? (
                      <div className="prose prose-invert prose-sm max-w-none prose-p:leading-relaxed prose-pre:bg-surface-container-high prose-pre:border prose-pre:border-outline-variant/20">
                        <ReactMarkdown 
                          remarkPlugins={[remarkGfm]}
                          components={{
                            code({node, className, children, ...props}: any) {
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
                  {msg.timestamp && (
                    <span className="text-[10px] text-on-surface-variant/50 px-1">{msg.timestamp}</span>
                  )}
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
          </div>

          {/* Input Area */}
          <div className="p-4 bg-surface-container-lowest border-t border-outline-variant/10 shrink-0">
            {attachments.length > 0 && (
              <div className="flex flex-wrap gap-3 mb-3 p-3 bg-surface-container/50 rounded-xl border border-outline-variant/10">
                {attachments.map((file, idx) => (
                  <AttachmentPreview 
                    key={idx} 
                    file={file} 
                    onRemove={() => removeAttachment(idx)} 
                  />
                ))}
              </div>
            )}
            <form 
              onSubmit={handleSend} 
              className={`relative flex items-center rounded-2xl transition-colors ${isDragging ? 'bg-primary-container/10 border-primary-container/50 ring-2 ring-primary-container/30' : ''}`}
            >
              <input 
                type="file" 
                multiple 
                ref={fileInputRef} 
                onChange={handleFileSelect} 
                className="hidden" 
              />
              <button 
                type="button" 
                onClick={() => fileInputRef.current?.click()}
                className="absolute left-3 text-on-surface-variant/50 hover:text-primary-container transition-colors z-10"
                title="Attach files or images"
              >
                <Paperclip className="w-5 h-5" />
              </button>
              <input 
                type="text" 
                value={input}
                onChange={(e) => setInput(e.target.value)}
                placeholder={isDragging ? "Drop files here..." : "Ask about caching strategies, database scaling, or security..."}
                className={`w-full bg-surface-container border border-outline-variant/20 rounded-2xl py-3 pl-12 pr-14 text-sm focus:ring-1 focus:ring-primary-container outline-none transition-all placeholder:text-on-surface-variant/40 ${isDragging ? 'bg-transparent border-transparent' : ''}`}
              />
              <button 
                type="submit" 
                disabled={!input.trim() && attachments.length === 0}
                className="absolute right-2 p-2 bg-primary-container text-on-primary rounded-xl hover:brightness-110 transition-all disabled:opacity-50 disabled:hover:brightness-100 z-10"
              >
                <Send className="w-4 h-4" />
              </button>
            </form>
            <div className="text-center mt-2">
              <span className="text-[10px] text-on-surface-variant/40 font-label uppercase tracking-widest">SystemForge AI can make mistakes. Verify critical architecture decisions.</span>
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
