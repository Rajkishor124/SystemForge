'use client';

import { useState, useEffect, type FormEvent } from 'react';
import { Save, User, Shield, Key, CreditCard, Loader2, CheckCircle, AlertCircle, Eye, EyeOff } from 'lucide-react';
import { api, ApiError } from '@/lib/api';

// ─── Types ──────────────────────────────────────────────────────────────────────

interface UserProfile {
  id: string;
  name: string;
  email: string;
  role: string;
  accountStatus: string;
  authProvider: string;
  emailVerified: boolean;
  lastLoginAt: string | null;
  createdAt: string;
  updatedAt: string;
}

type ActiveTab = 'profile' | 'security';

// ─── Component ──────────────────────────────────────────────────────────────────

export default function SettingsPage() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('profile');

  // Profile state
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(true);
  const [profileError, setProfileError] = useState('');

  // Profile form
  const [name, setName] = useState('');
  const [saving, setSaving] = useState(false);
  const [saveMessage, setSaveMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Password form
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showCurrentPassword, setShowCurrentPassword] = useState(false);
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);
  const [passwordMessage, setPasswordMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // ─── Fetch Profile ──────────────────────────────────────────────────────────

  useEffect(() => {
    fetchProfile();
  }, []);

  async function fetchProfile() {
    setProfileLoading(true);
    setProfileError('');
    try {
      const res = await api<UserProfile>('/api/v1/users/me');
      setProfile(res.data);
      setName(res.data.name);
    } catch (err) {
      if (err instanceof ApiError) {
        setProfileError(err.message);
      } else {
        setProfileError('Failed to load profile. Please try again.');
      }
    } finally {
      setProfileLoading(false);
    }
  }

  // ─── Update Profile ─────────────────────────────────────────────────────────

  async function handleSaveProfile(e: FormEvent) {
    e.preventDefault();
    setSaveMessage(null);
    setSaving(true);
    try {
      const res = await api<UserProfile>('/api/v1/users/profile', {
        method: 'PUT',
        body: { name: name.trim() },
      });
      setProfile(res.data);
      setName(res.data.name);
      setSaveMessage({ type: 'success', text: 'Profile updated successfully!' });
    } catch (err) {
      if (err instanceof ApiError) {
        setSaveMessage({ type: 'error', text: err.message });
      } else {
        setSaveMessage({ type: 'error', text: 'Failed to update profile.' });
      }
    } finally {
      setSaving(false);
      setTimeout(() => setSaveMessage(null), 4000);
    }
  }

  // ─── Change Password ───────────────────────────────────────────────────────

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault();
    setPasswordMessage(null);

    if (newPassword !== confirmPassword) {
      setPasswordMessage({ type: 'error', text: 'New passwords do not match.' });
      return;
    }

    if (!/^(?=.*[A-Za-z])(?=.*\d).{8,}$/.test(newPassword)) {
      setPasswordMessage({ type: 'error', text: 'Password must be at least 8 characters with a letter and a number.' });
      return;
    }

    setChangingPassword(true);
    try {
      await api('/api/v1/users/password', {
        method: 'PUT',
        body: { currentPassword, newPassword },
      });
      setPasswordMessage({ type: 'success', text: 'Password changed successfully!' });
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      if (err instanceof ApiError) {
        setPasswordMessage({ type: 'error', text: err.message });
      } else {
        setPasswordMessage({ type: 'error', text: 'Failed to change password.' });
      }
    } finally {
      setChangingPassword(false);
      setTimeout(() => setPasswordMessage(null), 4000);
    }
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  function formatDate(iso: string | null): string {
    if (!iso) return 'Never';
    return new Date(iso).toLocaleDateString('en-US', {
      year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit',
    });
  }

  function StatusBadge({ status }: { status: string }) {
    const colors: Record<string, string> = {
      ACTIVE: 'bg-green-500/15 text-green-400 border-green-500/30',
      SUSPENDED: 'bg-yellow-500/15 text-yellow-400 border-yellow-500/30',
      BANNED: 'bg-red-500/15 text-red-400 border-red-500/30',
    };
    return (
      <span className={`text-xs font-bold uppercase tracking-wider px-2.5 py-1 rounded-full border ${colors[status] || 'bg-white/10 text-white/60 border-white/20'}`}>
        {status}
      </span>
    );
  }

  // ─── Render ─────────────────────────────────────────────────────────────────

  return (
    <div className="p-8 max-w-4xl mx-auto w-full">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">Settings</h1>
        <p className="text-[#dee1f7]/60 text-sm">Manage your account preferences and security.</p>
      </div>

      <div className="grid md:grid-cols-4 gap-8">
        {/* Sidebar */}
        <div className="md:col-span-1 space-y-2">
          <button
            onClick={() => setActiveTab('profile')}
            className={`w-full text-left px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-3 transition-colors ${
              activeTab === 'profile'
                ? 'bg-primary-container/10 text-primary-container'
                : 'hover:bg-surface-container text-[#dee1f7]/60 hover:text-[#dee1f7]'
            }`}
          >
            <User className="w-4 h-4" /> Profile
          </button>
          <button
            onClick={() => setActiveTab('security')}
            className={`w-full text-left px-4 py-2 rounded-lg font-bold text-sm flex items-center gap-3 transition-colors ${
              activeTab === 'security'
                ? 'bg-primary-container/10 text-primary-container'
                : 'hover:bg-surface-container text-[#dee1f7]/60 hover:text-[#dee1f7]'
            }`}
          >
            <Shield className="w-4 h-4" /> Security
          </button>
          <button className="w-full text-left px-4 py-2 rounded-lg hover:bg-surface-container text-[#dee1f7]/60 hover:text-[#dee1f7] font-bold text-sm flex items-center gap-3 transition-colors opacity-50 cursor-not-allowed">
            <Key className="w-4 h-4" /> API Keys
          </button>
          <button className="w-full text-left px-4 py-2 rounded-lg hover:bg-surface-container text-[#dee1f7]/60 hover:text-[#dee1f7] font-bold text-sm flex items-center gap-3 transition-colors opacity-50 cursor-not-allowed">
            <CreditCard className="w-4 h-4" /> Billing
          </button>
        </div>

        {/* Content Area */}
        <div className="md:col-span-3 space-y-6">
          {/* Loading State */}
          {profileLoading && (
            <div className="glass-card rounded-xl p-12 flex items-center justify-center">
              <Loader2 className="w-8 h-8 animate-spin text-primary-container" />
            </div>
          )}

          {/* Error State */}
          {profileError && !profileLoading && (
            <div className="glass-card rounded-xl p-6">
              <div className="flex items-center gap-3 text-red-400">
                <AlertCircle className="w-5 h-5 shrink-0" />
                <span className="text-sm">{profileError}</span>
              </div>
              <button onClick={fetchProfile} className="mt-4 text-primary-container text-sm font-bold hover:underline">
                Try again
              </button>
            </div>
          )}

          {/* Profile Tab */}
          {!profileLoading && !profileError && profile && activeTab === 'profile' && (
            <>
              {/* Profile Form */}
              <div className="glass-card rounded-xl p-6">
                <h2 className="text-xl font-bold font-headline mb-6 border-b border-outline-variant/20 pb-2">Profile Information</h2>

                {saveMessage && (
                  <div className={`mb-4 p-3 rounded-lg text-sm flex items-center gap-2 ${
                    saveMessage.type === 'success'
                      ? 'bg-green-500/10 border border-green-500/30 text-green-400'
                      : 'bg-red-500/10 border border-red-500/30 text-red-400'
                  }`}>
                    {saveMessage.type === 'success' ? <CheckCircle className="w-4 h-4 shrink-0" /> : <AlertCircle className="w-4 h-4 shrink-0" />}
                    {saveMessage.text}
                  </div>
                )}

                <form onSubmit={handleSaveProfile} className="space-y-4">
                  <div>
                    <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Full Name</label>
                    <input
                      type="text"
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      required
                      minLength={2}
                      maxLength={100}
                      className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]"
                    />
                  </div>
                  <div>
                    <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Email Address</label>
                    <input
                      type="email"
                      value={profile.email}
                      disabled
                      className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm outline-none text-[#dee1f7]/50 cursor-not-allowed"
                    />
                  </div>
                  <div className="grid sm:grid-cols-2 gap-4">
                    <div>
                      <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Role</label>
                      <input
                        type="text"
                        value={profile.role}
                        disabled
                        className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm outline-none text-[#dee1f7]/50 cursor-not-allowed"
                      />
                    </div>
                    <div>
                      <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Auth Provider</label>
                      <input
                        type="text"
                        value={profile.authProvider}
                        disabled
                        className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm outline-none text-[#dee1f7]/50 cursor-not-allowed"
                      />
                    </div>
                  </div>
                  <div className="pt-4 flex justify-end">
                    <button
                      type="submit"
                      disabled={saving || name.trim() === profile.name}
                      className="cta-gradient text-on-primary px-6 py-2 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      {saving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                      {saving ? 'Saving...' : 'Save Changes'}
                    </button>
                  </div>
                </form>
              </div>

              {/* Account Details */}
              <div className="glass-card rounded-xl p-6">
                <h2 className="text-xl font-bold font-headline mb-6 border-b border-outline-variant/20 pb-2">Account Details</h2>
                <div className="grid sm:grid-cols-2 gap-y-4 gap-x-8 text-sm">
                  <div>
                    <div className="text-[#dee1f7]/40 text-xs uppercase tracking-widest mb-1">Account Status</div>
                    <StatusBadge status={profile.accountStatus} />
                  </div>
                  <div>
                    <div className="text-[#dee1f7]/40 text-xs uppercase tracking-widest mb-1">Email Verified</div>
                    <span className={`text-sm font-medium ${profile.emailVerified ? 'text-green-400' : 'text-yellow-400'}`}>
                      {profile.emailVerified ? '✓ Verified' : '✗ Not Verified'}
                    </span>
                  </div>
                  <div>
                    <div className="text-[#dee1f7]/40 text-xs uppercase tracking-widest mb-1">Last Login</div>
                    <div className="text-[#dee1f7]/80">{formatDate(profile.lastLoginAt)}</div>
                  </div>
                  <div>
                    <div className="text-[#dee1f7]/40 text-xs uppercase tracking-widest mb-1">Member Since</div>
                    <div className="text-[#dee1f7]/80">{formatDate(profile.createdAt)}</div>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* Security Tab */}
          {!profileLoading && !profileError && profile && activeTab === 'security' && (
            <div className="glass-card rounded-xl p-6">
              <h2 className="text-xl font-bold font-headline mb-6 border-b border-outline-variant/20 pb-2">Change Password</h2>

              {profile.authProvider !== 'LOCAL' && (
                <div className="p-3 rounded-lg bg-yellow-500/10 border border-yellow-500/30 text-yellow-400 text-sm mb-4">
                  Password change is not available for {profile.authProvider} accounts.
                </div>
              )}

              {passwordMessage && (
                <div className={`mb-4 p-3 rounded-lg text-sm flex items-center gap-2 ${
                  passwordMessage.type === 'success'
                    ? 'bg-green-500/10 border border-green-500/30 text-green-400'
                    : 'bg-red-500/10 border border-red-500/30 text-red-400'
                }`}>
                  {passwordMessage.type === 'success' ? <CheckCircle className="w-4 h-4 shrink-0" /> : <AlertCircle className="w-4 h-4 shrink-0" />}
                  {passwordMessage.text}
                </div>
              )}

              <form onSubmit={handleChangePassword} className="space-y-4">
                <div>
                  <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Current Password</label>
                  <div className="relative">
                    <input
                      type={showCurrentPassword ? 'text' : 'password'}
                      value={currentPassword}
                      onChange={(e) => setCurrentPassword(e.target.value)}
                      required
                      disabled={profile.authProvider !== 'LOCAL'}
                      className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 pr-10 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7] disabled:opacity-50 disabled:cursor-not-allowed"
                    />
                    <button type="button" onClick={() => setShowCurrentPassword(!showCurrentPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#dee1f7]/40 hover:text-primary-container transition-colors">
                      {showCurrentPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">New Password</label>
                  <div className="relative">
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      required
                      minLength={8}
                      maxLength={64}
                      disabled={profile.authProvider !== 'LOCAL'}
                      className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 pr-10 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7] disabled:opacity-50 disabled:cursor-not-allowed"
                    />
                    <button type="button" onClick={() => setShowNewPassword(!showNewPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#dee1f7]/40 hover:text-primary-container transition-colors">
                      {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  </div>
                </div>
                <div>
                  <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Confirm New Password</label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    required
                    minLength={8}
                    maxLength={64}
                    disabled={profile.authProvider !== 'LOCAL'}
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7] disabled:opacity-50 disabled:cursor-not-allowed"
                  />
                </div>
                <div className="pt-4 flex justify-end">
                  <button
                    type="submit"
                    disabled={changingPassword || profile.authProvider !== 'LOCAL' || !currentPassword || !newPassword || !confirmPassword}
                    className="cta-gradient text-on-primary px-6 py-2 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {changingPassword ? <Loader2 className="w-4 h-4 animate-spin" /> : <Key className="w-4 h-4" />}
                    {changingPassword ? 'Changing...' : 'Change Password'}
                  </button>
                </div>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
