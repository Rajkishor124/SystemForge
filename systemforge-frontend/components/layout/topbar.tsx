'use client';

import { useState, useRef, useEffect } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Settings, Bell, Terminal, Menu, LogOut, User, Loader2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { useAuth } from '@/lib/auth-context';

const navLinks = [
  { name: 'Dashboard', href: '/dashboard' },
  { name: 'Architecture', href: '/architecture' },
  { name: 'AI Chat', href: '/chat' },
  { name: 'Schema', href: '/result' },
];

export function Topbar() {
  const pathname = usePathname();
  const { logout } = useAuth();
  const [profileOpen, setProfileOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Close dropdown on outside click
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setProfileOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  async function handleLogout() {
    setLoggingOut(true);
    try {
      await logout();
    } catch {
      // logout() in auth-context already handles cleanup even on failure
    } finally {
      setLoggingOut(false);
    }
  }

  return (
    <header className="bg-[#090e1c]/80 backdrop-blur-md shadow-[0_4px_20px_rgba(0,0,0,0.3)] fixed top-0 z-50 flex justify-between items-center h-16 px-4 md:px-6 w-full border-b border-outline-variant/10 transition-all duration-300">
      <div className="flex items-center gap-4 md:gap-8">
        <Link href="/dashboard" className="text-xl font-bold tracking-tighter text-[#00f2ff] drop-shadow-[0_0_8px_rgba(0,242,255,0.5)] font-headline flex items-center gap-2 group">
          <Terminal className="w-6 h-6 group-hover:rotate-12 transition-transform duration-300" />
          <span className="hidden sm:inline-block">SystemForge</span>
        </Link>
        
        <nav className="hidden md:flex items-center gap-1 ml-4">
          {navLinks.map((link) => {
            const isActive = pathname === link.href || (pathname.startsWith(link.href) && link.href !== '/dashboard');
            return (
              <Link 
                key={link.name}
                href={link.href} 
                className={`relative px-4 py-2 font-headline tracking-tight text-sm font-medium transition-colors rounded-md ${
                  isActive ? 'text-[#00f2ff]' : 'text-[#dee1f7]/60 hover:text-[#dee1f7] hover:bg-white/5'
                }`}
              >
                {link.name}
                {isActive && (
                  <motion.div
                    layoutId="topbar-active-indicator"
                    className="absolute bottom-0 left-0 right-0 h-0.5 bg-[#00f2ff] shadow-[0_0_8px_#00f2ff]"
                    initial={false}
                    transition={{ type: "spring", stiffness: 500, damping: 30 }}
                  />
                )}
              </Link>
            );
          })}
        </nav>
      </div>

      <div className="flex items-center gap-3 md:gap-4">
        <div className="hidden sm:flex items-center gap-1">
          <Link href="/settings" className="p-2 text-[#dee1f7]/60 hover:text-[#00f2ff] transition-all active:scale-95 rounded-lg hover:bg-primary-container/10">
            <Settings className="w-5 h-5" />
          </Link>
          <button className="relative p-2 text-[#dee1f7]/60 hover:text-[#00f2ff] transition-all active:scale-95 rounded-lg hover:bg-primary-container/10">
            <Bell className="w-5 h-5" />
            <span className="absolute top-2 right-2 w-2 h-2 bg-red-500 rounded-full border-2 border-[#090e1c]"></span>
          </button>
        </div>
        
        <button className="hidden sm:block bg-gradient-to-br from-[#00f2ff] to-[#0566d9] text-on-primary px-4 py-1.5 rounded-lg text-sm font-bold shadow-[0_0_15px_rgba(0,242,255,0.3)] hover:shadow-[0_0_25px_rgba(0,242,255,0.5)] hover:brightness-110 transition-all active:scale-95">
          Deploy System
        </button>

        <button className="md:hidden p-2 text-[#dee1f7]/60 hover:text-[#00f2ff] transition-colors">
          <Menu className="w-6 h-6" />
        </button>

        {/* Profile Avatar + Dropdown */}
        <div className="relative" ref={dropdownRef}>
          <button
            onClick={() => setProfileOpen(!profileOpen)}
            className="w-8 h-8 md:w-9 md:h-9 rounded-full overflow-hidden border-2 border-primary-container/20 hover:border-[#00f2ff] transition-colors block shrink-0 bg-gradient-to-br from-[#00f2ff]/20 to-[#0566d9]/20 flex items-center justify-center"
          >
            <User className="w-4 h-4 md:w-5 md:h-5 text-[#00f2ff]" />
          </button>

          <AnimatePresence>
            {profileOpen && (
              <motion.div
                initial={{ opacity: 0, y: -8, scale: 0.95 }}
                animate={{ opacity: 1, y: 0, scale: 1 }}
                exit={{ opacity: 0, y: -8, scale: 0.95 }}
                transition={{ duration: 0.15 }}
                className="absolute right-0 top-12 w-48 bg-[#0e1322] border border-outline-variant/20 rounded-xl shadow-2xl overflow-hidden z-50"
              >
                <Link
                  href="/settings"
                  onClick={() => setProfileOpen(false)}
                  className="flex items-center gap-3 px-4 py-3 text-sm text-[#dee1f7]/80 hover:bg-white/5 hover:text-[#e1fdff] transition-colors"
                >
                  <Settings className="w-4 h-4" />
                  Settings
                </Link>
                <div className="h-px bg-outline-variant/20" />
                <button
                  onClick={handleLogout}
                  disabled={loggingOut}
                  className="w-full flex items-center gap-3 px-4 py-3 text-sm text-red-400 hover:bg-red-500/10 transition-colors disabled:opacity-50"
                >
                  {loggingOut ? <Loader2 className="w-4 h-4 animate-spin" /> : <LogOut className="w-4 h-4" />}
                  {loggingOut ? 'Logging out...' : 'Logout'}
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </header>
  );
}
