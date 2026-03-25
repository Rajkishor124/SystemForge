'use client';

import { useState, useEffect } from 'react';
import { useAuth } from '@/lib/auth-context';
import { api } from '@/lib/api';
import { useRouter } from 'next/navigation';
import { motion, AnimatePresence } from 'motion/react';
import { 
  ShieldAlert, Users, Server, FileCode2, Layers, Send,
  UserCheck, UserX, UserSearch, Target, Award, Loader2,
  ChevronLeft, ChevronRight
} from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';

interface PlatformStats {
  totalUsers: number;
  activeUsers: number;
  totalSystemConfigs: number;
  generatedArchitectures: number;
  totalTemplates: number;
  totalNotificationsSent: number;
  computedAt: string;
}

interface UserDto {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: 'DEVELOPER' | 'ADMIN';
  accountStatus: 'ACTIVE' | 'DEACTIVATED' | 'LOCKED';
  createdAt: string;
  lastLoginAt: string;
}

export default function AdminDashboardPage() {
  const { user, isLoading } = useAuth();
  const router = useRouter();
  
  const [activeTab, setActiveTab] = useState<'STATS' | 'USERS'>('STATS');
  
  // Stats State
  const [stats, setStats] = useState<PlatformStats | null>(null);
  const [loadingStats, setLoadingStats] = useState(true);

  // Users State
  const [users, setUsers] = useState<UserDto[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [actionLoading, setActionLoading] = useState<string | null>(null);

  // Role Protection
  useEffect(() => {
    if (!isLoading && user && user.role !== 'ADMIN') {
      router.push('/dashboard');
    }
  }, [user, isLoading, router]);

  // Fetch Data
  useEffect(() => {
    if (user?.role !== 'ADMIN') return;

    if (activeTab === 'STATS') {
      fetchStats();
    } else {
      fetchUsers(0);
    }
  }, [activeTab, user]);

  async function fetchStats() {
    setLoadingStats(true);
    try {
      const res = await api('/api/v1/admin/stats');
      setStats(res.data as PlatformStats);
    } catch (error) {
      console.error('Failed to fetch stats:', error);
    } finally {
      setLoadingStats(false);
    }
  }

  async function fetchUsers(pageIdx: number) {
    setLoadingUsers(true);
    try {
      const res = await api(`/api/v1/admin/users?page=${pageIdx}&size=10`);
      const data = res.data as any;
      setUsers(data.content || []);
      setTotalPages(data.totalPages || 1);
      setPage(pageIdx);
    } catch (error) {
      console.error('Failed to fetch users:', error);
    } finally {
      setLoadingUsers(false);
    }
  }

  async function toggleUserStatus(userId: string, currentStatus: string) {
    setActionLoading(userId);
    try {
      const endpoint = currentStatus === 'ACTIVE' ? 'deactivate' : 'activate';
      const res = await api(`/api/v1/admin/users/${userId}/${endpoint}`, { method: 'PATCH' });
      const updatedUser = res.data as UserDto;
      setUsers(prev => prev.map(u => u.id === userId ? updatedUser : u));
    } catch (error) {
      console.error('Failed to toggle user status:', error);
    } finally {
      setActionLoading(null);
    }
  }

  async function toggleUserRole(userId: string, currentRole: string) {
    setActionLoading(userId);
    try {
      const newRole = currentRole === 'ADMIN' ? 'DEVELOPER' : 'ADMIN';
      const res = await api(`/api/v1/admin/users/${userId}/role?role=${newRole}`, { method: 'PATCH' });
      const updatedUser = res.data as UserDto;
      setUsers(prev => prev.map(u => u.id === userId ? updatedUser : u));
    } catch (error) {
      console.error('Failed to toggle user role:', error);
    } finally {
      setActionLoading(null);
    }
  }

  if (isLoading || (user && user.role !== 'ADMIN')) {
    return (
      <div className="flex-1 flex items-center justify-center min-h-[50vh]">
        <Loader2 className="w-8 h-8 text-[#00f2ff] animate-spin" />
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto w-full p-4 sm:p-6 lg:p-8">
      {/* Header */}
      <div className="mb-8 flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 bg-red-500/10 text-red-400 rounded-full text-xs font-bold uppercase tracking-widest mb-3 border border-red-500/20">
            <ShieldAlert className="w-3.5 h-3.5" />
            Admin Console
          </div>
          <h1 className="text-3xl font-black text-[#e1fdff] font-headline tracking-tight">
            Platform Management
          </h1>
          <p className="text-sm text-[#dee1f7]/60 mt-1">
            Global statistics and user administration controls
          </p>
        </div>
        
        {/* Tabs */}
        <div className="flex bg-[#090e1c] rounded-xl p-1 border border-outline-variant/10">
          <button
            onClick={() => setActiveTab('STATS')}
            className={`flex-1 sm:flex-none px-6 py-2.5 rounded-lg text-sm font-bold transition-all ${activeTab === 'STATS' ? 'bg-[#00f2ff]/10 text-[#00f2ff]' : 'text-[#dee1f7]/50 hover:text-[#dee1f7] hover:bg-white/5'}`}
          >
            Statistics
          </button>
          <button
            onClick={() => setActiveTab('USERS')}
            className={`flex-1 sm:flex-none px-6 py-2.5 rounded-lg text-sm font-bold transition-all ${activeTab === 'USERS' ? 'bg-[#00f2ff]/10 text-[#00f2ff]' : 'text-[#dee1f7]/50 hover:text-[#dee1f7] hover:bg-white/5'}`}
          >
            User Management
          </button>
        </div>
      </div>

      <AnimatePresence mode="wait">
        <motion.div
          key={activeTab}
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -10 }}
          transition={{ duration: 0.2 }}
        >
          {/* STATS VIEW */}
          {activeTab === 'STATS' && (
            loadingStats ? (
              <div className="flex justify-center py-20"><Loader2 className="w-8 h-8 text-[#00f2ff] animate-spin" /></div>
            ) : stats ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                <StatCard icon={<Users />} label="Total Users" value={stats.totalUsers} />
                <StatCard icon={<UserCheck />} label="Active Users" value={stats.activeUsers} highlight />
                <StatCard icon={<Server />} label="System Configs Saved" value={stats.totalSystemConfigs} />
                <StatCard icon={<FileCode2 />} label="Architectures Generated" value={stats.generatedArchitectures} highlight />
                <StatCard icon={<Layers />} label="Templates Available" value={stats.totalTemplates} />
                <StatCard icon={<Send />} label="Notifications Sent" value={stats.totalNotificationsSent} />
              </div>
            ) : (
              <p className="text-red-400">Failed to load statistics.</p>
            )
          )}

          {/* USERS VIEW */}
          {activeTab === 'USERS' && (
            <div className="bg-[#0e1322]/80 backdrop-blur-xl border border-outline-variant/10 rounded-2xl shadow-xl overflow-hidden">
              <div className="p-4 border-b border-outline-variant/10 flex items-center justify-between">
                <h3 className="font-bold text-[#e1fdff] flex items-center gap-2">
                  <UserSearch className="w-5 h-5 text-[#00f2ff]" />
                  Registered Users
                </h3>
              </div>
              
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-[#dee1f7]">
                  <thead className="bg-black/20 text-[#dee1f7]/60 text-xs uppercase tracking-wider font-bold border-b border-outline-variant/10">
                    <tr>
                      <th className="px-6 py-4">User</th>
                      <th className="px-6 py-4">Role</th>
                      <th className="px-6 py-4">Status</th>
                      <th className="px-6 py-4">Joined</th>
                      <th className="px-6 py-4 text-right">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-outline-variant/5">
                    {loadingUsers ? (
                      <tr><td colSpan={5} className="px-6 py-12 text-center"><Loader2 className="w-6 h-6 text-[#00f2ff] animate-spin mx-auto" /></td></tr>
                    ) : users.map(u => (
                      <tr key={u.id} className="hover:bg-white/5 transition-colors group">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-3">
                            <div className="w-8 h-8 rounded-full bg-gradient-to-br from-[#00f2ff]/20 to-blue-500/20 border border-[#00f2ff]/30 flex items-center justify-center text-xs font-bold text-[#00f2ff]">
                              {u.firstName?.[0] || '?'}{u.lastName?.[0] || ''}
                            </div>
                            <div>
                              <p className="font-bold text-[#e1fdff]">{u.firstName} {u.lastName}</p>
                              <p className="text-xs text-[#dee1f7]/50">{u.email}</p>
                            </div>
                          </div>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`inline-flex items-center gap-1 px-2.5 py-1 rounded text-[10px] uppercase font-black tracking-widest border ${u.role === 'ADMIN' ? 'bg-red-500/10 text-red-400 border-red-500/20' : 'bg-[#00f2ff]/10 text-[#00f2ff] border-[#00f2ff]/20'}`}>
                            {u.role === 'ADMIN' && <ShieldAlert className="w-3 h-3" />}
                            {u.role}
                          </span>
                        </td>
                        <td className="px-6 py-4">
                          <span className={`text-[11px] uppercase font-black tracking-widest flex items-center gap-1.5 ${u.accountStatus === 'ACTIVE' ? 'text-green-400' : 'text-yellow-400'}`}>
                            <div className={`w-1.5 h-1.5 rounded-full ${u.accountStatus === 'ACTIVE' ? 'bg-green-400 shadow-[0_0_8px_rgba(74,222,128,0.8)]' : 'bg-yellow-400'}`}></div>
                            {u.accountStatus}
                          </span>
                        </td>
                        <td className="px-6 py-4 text-xs text-[#dee1f7]/50 font-medium">
                          {formatDistanceToNow(new Date(u.createdAt), { addSuffix: true })}
                        </td>
                        <td className="px-6 py-4 text-right">
                          <div className="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                            {actionLoading === u.id ? (
                              <Loader2 className="w-4 h-4 text-[#00f2ff] animate-spin" />
                            ) : u.id !== user?.userId && (
                              <>
                                <button 
                                  onClick={() => toggleUserRole(u.id, u.role)}
                                  className="p-2 bg-white/5 hover:bg-white/10 rounded border border-white/5 text-[#dee1f7]/70 hover:text-white transition-colors"
                                  title={`Change to ${u.role === 'ADMIN' ? 'DEVELOPER' : 'ADMIN'}`}
                                >
                                  {u.role === 'ADMIN' ? <Target className="w-4 h-4" /> : <Award className="w-4 h-4 text-[#00f2ff]" />}
                                </button>
                                <button 
                                  onClick={() => toggleUserStatus(u.id, u.accountStatus)}
                                  className={`p-2 bg-white/5 hover:bg-white/10 rounded border border-white/5 transition-colors ${u.accountStatus === 'ACTIVE' ? 'text-yellow-400 hover:bg-yellow-400/10 hover:border-yellow-400/30' : 'text-green-400 hover:bg-green-400/10 hover:border-green-400/30'}`}
                                  title={u.accountStatus === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                                >
                                  {u.accountStatus === 'ACTIVE' ? <UserX className="w-4 h-4" /> : <UserCheck className="w-4 h-4" />}
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              
              {/* Pagination */}
              {totalPages > 1 && (
                <div className="p-4 border-t border-outline-variant/10 flex items-center justify-between bg-black/20">
                  <span className="text-xs text-[#dee1f7]/50 font-bold uppercase tracking-widest">
                    Page {page + 1} of {totalPages}
                  </span>
                  <div className="flex gap-2">
                    <button 
                      disabled={page === 0}
                      onClick={() => fetchUsers(page - 1)}
                      className="p-1.5 rounded bg-white/5 hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors border border-white/10"
                    >
                      <ChevronLeft className="w-4 h-4" />
                    </button>
                    <button 
                      disabled={page >= totalPages - 1}
                      onClick={() => fetchUsers(page + 1)}
                      className="p-1.5 rounded bg-white/5 hover:bg-white/10 disabled:opacity-30 disabled:cursor-not-allowed transition-colors border border-white/10"
                    >
                      <ChevronRight className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              )}
            </div>
          )}
        </motion.div>
      </AnimatePresence>
    </div>
  );
}

function StatCard({ icon, label, value, highlight = false }: { icon: React.ReactNode, label: string, value: number, highlight?: boolean }) {
  return (
    <div className={`p-6 bg-[#0e1322]/80 backdrop-blur-xl rounded-2xl border transition-all duration-300 hover:-translate-y-1 hover:shadow-xl ${highlight ? 'border-[#00f2ff]/30 shadow-[0_0_30px_rgba(0,242,255,0.05)] hover:border-[#00f2ff]/50' : 'border-outline-variant/10'}`}>
      <div className={`w-12 h-12 rounded-xl flex items-center justify-center mb-4 ${highlight ? 'bg-[#00f2ff]/10 text-[#00f2ff]' : 'bg-primary-container/10 text-[#dee1f7]/60'}`}>
        {icon}
      </div>
      <p className="text-3xl font-black text-[#e1fdff] tracking-tight">{value.toLocaleString()}</p>
      <p className="text-sm font-medium text-[#dee1f7]/60 mt-1">{label}</p>
    </div>
  );
}
