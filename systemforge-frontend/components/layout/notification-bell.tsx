'use client';

import { useState, useRef, useEffect } from 'react';
import { Bell, Check, ExternalLink, Loader2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { api } from '@/lib/api';
import { useRouter } from 'next/navigation';
import { formatDistanceToNow } from 'date-fns';

export interface InAppNotification {
  id: string;
  title: string;
  message: string;
  type: string;
  link: string | null;
  read: boolean;
  createdAt: string;
}

export function NotificationBell() {
  const router = useRouter();
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  
  const [unreadCount, setUnreadCount] = useState(0);
  const [notifications, setNotifications] = useState<InAppNotification[]>([]);
  const [loading, setLoading] = useState(false);

  // Close when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  // Fetch unread count on mount and every minute
  useEffect(() => {
    fetchUnreadCount();
    const interval = setInterval(fetchUnreadCount, 60000);
    return () => clearInterval(interval);
  }, []);

  // Fetch notifications when opening the dropdown
  useEffect(() => {
    if (isOpen) {
      fetchNotifications();
    }
  }, [isOpen]);

  async function fetchUnreadCount() {
    try {
      const res = await api('/api/v1/notifications/unread-count');
      setUnreadCount(Number(res.data) || 0);
    } catch (error: any) {
      if (error?.status !== 401) {
        console.warn('Failed to fetch unread count:', error.message || error);
      }
    }
  }

  async function fetchNotifications() {
    setLoading(true);
    try {
      const res = await api('/api/v1/notifications?page=0&size=10');
      const data = res.data as any;
      if (data?.content) {
        setNotifications(data.content);
      }
    } catch (error: any) {
      if (error?.status !== 401) {
        console.warn('Failed to fetch notifications:', error.message || error);
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleMarkAsRead(id: string) {
    try {
      // Optimistic update
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
      setUnreadCount(prev => Math.max(0, prev - 1));
      
      await api(`/api/v1/notifications/${id}/read`, { method: 'PUT' });
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  }

  async function handleMarkAllAsRead(e: React.MouseEvent) {
    e.stopPropagation();
    try {
      // Optimistic update
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      setUnreadCount(0);

      await api('/api/v1/notifications/read-all', { method: 'PUT' });
    } catch (error) {
      console.error('Failed to mark all as read:', error);
    }
  }

  function handleNotificationClick(n: InAppNotification) {
    if (!n.read) {
      handleMarkAsRead(n.id);
    }
    setIsOpen(false);
    if (n.link) {
      router.push(n.link);
    }
  }

  function getIconColor(type: string) {
    switch (type) {
      case 'SUCCESS': return 'text-green-400';
      case 'WARNING': return 'text-yellow-400';
      case 'ERROR': return 'text-red-400';
      default: return 'text-[#00f2ff]';
    }
  }

  return (
    <div className="relative" ref={dropdownRef}>
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 text-[#dee1f7]/60 hover:text-[#00f2ff] transition-all active:scale-95 rounded-lg hover:bg-primary-container/10"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-4 h-4 text-[9px] font-bold flex items-center justify-center bg-red-500 text-white rounded-full border-2 border-[#090e1c]">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: -8, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: -8, scale: 0.95 }}
            transition={{ duration: 0.15 }}
            className="absolute right-0 top-12 w-80 sm:w-96 max-h-[80vh] flex flex-col bg-[#0e1322] border border-outline-variant/20 rounded-xl shadow-2xl overflow-hidden z-50"
          >
            {/* Header */}
            <div className="flex items-center justify-between p-4 border-b border-outline-variant/10">
              <h3 className="text-sm font-bold text-[#e1fdff]">Notifications</h3>
              {unreadCount > 0 && (
                <button 
                  onClick={handleMarkAllAsRead}
                  className="text-xs text-[#00f2ff] hover:underline"
                >
                  Mark all as read
                </button>
              )}
            </div>

            {/* List */}
            <div className="flex-1 overflow-y-auto custom-scrollbar">
              {loading ? (
                <div className="flex justify-center items-center py-8">
                  <Loader2 className="w-5 h-5 text-[#00f2ff] animate-spin" />
                </div>
              ) : notifications.length === 0 ? (
                <div className="py-8 text-center text-sm text-[#dee1f7]/50">
                  <p>You have no notifications.</p>
                </div>
              ) : (
                <div className="flex flex-col">
                  {notifications.map(n => (
                    <div 
                      key={n.id} 
                      onClick={() => handleNotificationClick(n)}
                      className={`p-4 border-b border-outline-variant/5 hover:bg-white/5 transition-colors cursor-pointer group ${!n.read ? 'bg-[#00f2ff]/5' : ''}`}
                    >
                      <div className="flex gap-3">
                        <div className={`mt-0.5 shrink-0 ${getIconColor(n.type)}`}>
                          {!n.read ? (
                            <div className="w-2 h-2 mt-1 rounded-full bg-current shadow-[0_0_8px_currentColor] animate-pulse"></div>
                          ) : (
                            <Check className="w-4 h-4 opacity-50" />
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className={`text-sm tracking-tight ${!n.read ? 'font-bold text-[#e1fdff]' : 'font-medium text-[#dee1f7]/80'}`}>
                            {n.title}
                          </p>
                          <p className="text-xs text-[#dee1f7]/60 mt-1 line-clamp-2 leading-relaxed">
                            {n.message}
                          </p>
                          <div className="flex items-center gap-2 mt-2">
                            <span className="text-[10px] text-[#dee1f7]/40 uppercase tracking-widest font-black">
                              {formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })}
                            </span>
                            {n.link && (
                              <div className="opacity-0 group-hover:opacity-100 transition-opacity ml-auto text-[#00f2ff] flex items-center gap-1 text-[10px] font-bold uppercase">
                                <span>View</span>
                                <ExternalLink className="w-3 h-3" />
                              </div>
                            )}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            {/* Footer */}
            <div className="p-2 border-t border-outline-variant/10 bg-black/20 text-center">
              <button 
                onClick={() => { setIsOpen(false); router.push('/notifications'); }}
                className="text-xs font-bold uppercase tracking-widest text-[#00f2ff] hover:text-[#e1fdff] transition-colors p-2"
              >
                View All History
              </button>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
