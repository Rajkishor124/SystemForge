'use client';

import { useState, useEffect } from 'react';
import { Bell, Check, ExternalLink, Loader2, Sparkles } from 'lucide-react';
import { api } from '@/lib/api';
import { formatDistanceToNow } from 'date-fns';
import Link from 'next/link';
import { motion } from 'motion/react';
import { InAppNotification } from '@/components/layout/notification-bell';

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState<InAppNotification[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    fetchNotifications(true);
  }, []);

  async function fetchNotifications(reset = false) {
    if (reset) setLoading(true);
    
    try {
      const currentPage = reset ? 0 : page;
      const res = await api(`/api/v1/notifications?page=${currentPage}&size=20`);
      
      const data = res.data as any;
      const newNotifications = data?.content || [];
      if (reset) {
        setNotifications(newNotifications);
      } else {
        setNotifications(prev => [...prev, ...newNotifications]);
      }
      
      setHasMore(newNotifications.length === 20 && !data?.last);
      setPage(currentPage + 1);
    } catch (error) {
      console.error('Failed to fetch notifications:', error);
    } finally {
      if (reset) setLoading(false);
    }
  }

  async function handleMarkAsRead(id: string) {
    try {
      setNotifications(prev => prev.map(n => n.id === id ? { ...n, read: true } : n));
      await api(`/api/v1/notifications/${id}/read`, { method: 'PUT' });
    } catch (error) {
      console.error('Failed to mark notification as read:', error);
    }
  }

  async function handleMarkAllAsRead() {
    try {
      setNotifications(prev => prev.map(n => ({ ...n, read: true })));
      await api('/api/v1/notifications/read-all', { method: 'PUT' });
    } catch (error) {
      console.error('Failed to mark all as read:', error);
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

  if (loading) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-[50vh]">
        <div className="flex flex-col items-center gap-4">
          <Loader2 className="w-8 h-8 text-[#00f2ff] animate-spin" />
          <p className="text-[#dee1f7]/60 font-headline">Loading history...</p>
        </div>
      </div>
    );
  }

  const unreadCount = notifications.filter(n => !n.read).length;

  return (
    <div className="max-w-4xl mx-auto w-full p-4 sm:p-6 lg:p-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-[#e1fdff] font-headline tracking-tight flex items-center gap-3">
            <Bell className="w-8 h-8 text-[#00f2ff]" />
            Notifications
          </h1>
          <p className="text-sm text-[#dee1f7]/60 mt-1">
            Activity and alerts for your architecture configurations
          </p>
        </div>
        
        {unreadCount > 0 && (
          <button 
            onClick={handleMarkAllAsRead}
            className="flex items-center gap-2 px-4 py-2 bg-white/5 hover:bg-white/10 text-[#00f2ff] rounded-lg transition-colors border border-white/10 text-sm font-bold shadow-[0_4px_12px_rgba(0,0,0,0.1)] w-fit"
          >
            <Check className="w-4 h-4" />
            Mark all read
          </button>
        )}
      </div>

      {/* List */}
      <div className="bg-[#0e1322]/80 backdrop-blur-xl border border-outline-variant/10 rounded-2xl shadow-[0_8px_32px_rgba(0,0,0,0.4)] overflow-hidden">
        {notifications.length === 0 ? (
          <div className="py-20 flex flex-col items-center justify-center text-center px-4">
            <div className="w-16 h-16 rounded-full bg-[#00f2ff]/10 flex items-center justify-center mb-4">
              <Sparkles className="w-8 h-8 text-[#00f2ff] opacity-50" />
            </div>
            <h3 className="text-lg font-bold text-[#e1fdff] mb-2">You&apos;re all caught up!</h3>
            <p className="text-[#dee1f7]/50 max-w-sm">
              We&apos;ll notify you here when your system generation completes or if there are important alerts.
            </p>
          </div>
        ) : (
          <div className="divide-y divide-outline-variant/5">
            {notifications.map((n, i) => (
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: i * 0.05 }}
                key={n.id} 
                className={`p-5 sm:p-6 transition-colors group ${!n.read ? 'bg-[#00f2ff]/5' : 'hover:bg-white/5'}`}
              >
                <div className="flex gap-4">
                  <div className={`mt-1 shrink-0 ${getIconColor(n.type)}`}>
                    {!n.read ? (
                      <div className="w-2.5 h-2.5 mt-1.5 rounded-full bg-current shadow-[0_0_10px_currentColor] animate-pulse"></div>
                    ) : (
                      <Check className="w-5 h-5 opacity-40" />
                    )}
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 sm:gap-4 mb-1">
                      <h4 className={`text-base tracking-tight ${!n.read ? 'font-bold text-[#e1fdff]' : 'font-medium text-[#dee1f7]/80'}`}>
                        {n.title}
                      </h4>
                      <span className="text-[11px] text-[#dee1f7]/40 uppercase tracking-widest font-black shrink-0">
                        {formatDistanceToNow(new Date(n.createdAt), { addSuffix: true })}
                      </span>
                    </div>
                    <p className={`text-sm leading-relaxed mb-3 ${!n.read ? 'text-[#dee1f7]/90' : 'text-[#dee1f7]/60'}`}>
                      {n.message}
                    </p>
                    
                    <div className="flex items-center gap-3">
                      {!n.read && (
                        <button 
                          onClick={() => handleMarkAsRead(n.id)}
                          className="text-xs font-bold text-[#dee1f7]/50 hover:text-[#00f2ff] uppercase tracking-wider transition-colors"
                        >
                          Mark as Read
                        </button>
                      )}
                      
                      {n.link && (
                        <Link 
                          href={n.link}
                          onClick={() => { if (!n.read) handleMarkAsRead(n.id); }}
                          className="flex items-center gap-1.5 text-xs font-bold text-[#00f2ff] hover:text-[#e1fdff] uppercase tracking-wider transition-colors"
                        >
                          View Details
                          <ExternalLink className="w-3.5 h-3.5" />
                        </Link>
                      )}
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>
      
      {/* Load More Option */}
      {hasMore && notifications.length > 0 && (
        <div className="mt-8 text-center">
          <button 
            onClick={() => fetchNotifications()}
            className="px-6 py-2.5 bg-surface-container-highest/50 border border-primary-container/20 text-[#00f2ff] rounded-lg text-xs font-bold uppercase tracking-widest font-label hover:bg-primary-container/10 transition-colors"
          >
            Load Older Alerts
          </button>
        </div>
      )}
    </div>
  );
}
