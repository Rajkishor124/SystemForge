'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { PlusSquare, History, Network, Sliders, Book, Activity, Cpu, Layers, MessageSquare } from 'lucide-react';
import { motion } from 'motion/react';

const mainNavItems = [
  { name: 'New Session', href: '/create', icon: PlusSquare },
  { name: 'Architecture History', href: '/dashboard', icon: History },
  { name: 'Templates', href: '/templates', icon: Layers },
  { name: 'AI Chat', href: '/chat', icon: MessageSquare },
  { name: 'Resource Graph', href: '/architecture', icon: Network },
  { name: 'Env Variables', href: '/env', icon: Sliders },
];

const bottomNavItems = [
  { name: 'Documentation', href: '#', icon: Book },
  { name: 'API Status', href: '#', icon: Activity },
];

export function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden md:flex flex-col h-full py-6 bg-[#090e1c]/95 backdrop-blur-xl border-r border-outline-variant/10 w-64 shrink-0 fixed left-0 top-16 bottom-0 z-40 shadow-[4px_0_24px_rgba(0,0,0,0.2)]">
      <div className="px-6 mb-8 mt-2">
        <div className="flex flex-col gap-1">
          <span className="text-[10px] font-black text-[#dee1f7]/50 font-headline uppercase tracking-widest mb-1">Workspace</span>
          <div className="flex items-center gap-2 text-[#e1fdff] bg-white/5 px-3 py-2.5 rounded-lg border border-white/10 hover:bg-white/10 transition-colors cursor-pointer group">
            <div className="w-2 h-2 rounded-full bg-[#00f2ff] shadow-[0_0_8px_#00f2ff] animate-pulse"></div>
            <span className="text-xs font-bold font-headline truncate group-hover:text-[#00f2ff] transition-colors">Production Env</span>
          </div>
        </div>
      </div>

      <div className="px-4 mb-2">
        <span className="text-[10px] font-bold text-[#dee1f7]/40 uppercase tracking-widest px-2">Main Menu</span>
      </div>

      <nav className="flex-1 space-y-1 px-3">
        {mainNavItems.map((item) => {
          const isActive = pathname === item.href;
          const Icon = item.icon;
          
          return (
            <Link 
              key={item.name}
              href={item.href} 
              className={`group relative flex items-center gap-3 px-3 py-2.5 rounded-lg font-label uppercase text-[11px] tracking-widest font-bold transition-all duration-300 overflow-hidden ${
                isActive 
                  ? 'text-[#00f2ff] bg-[#00f2ff]/10' 
                  : 'text-[#dee1f7]/60 hover:text-[#dee1f7] hover:bg-white/5'
              }`}
            >
              {isActive && (
                <motion.div 
                  layoutId="sidebar-active-indicator"
                  className="absolute left-0 top-0 bottom-0 w-1 bg-[#00f2ff] shadow-[0_0_10px_#00f2ff] rounded-r-full"
                  initial={false}
                  transition={{ type: "spring", stiffness: 500, damping: 30 }}
                />
              )}
              <Icon className={`w-4 h-4 transition-transform duration-300 ${isActive ? 'scale-110' : 'group-hover:scale-110'}`} />
              <span className="relative z-10">{item.name}</span>
            </Link>
          );
        })}
      </nav>

      <div className="px-4 mt-auto space-y-4">
        <div className="p-4 rounded-xl bg-gradient-to-b from-surface-container-highest/50 to-transparent border border-outline-variant/10 relative overflow-hidden group">
          <div className="absolute inset-0 bg-gradient-to-r from-[#00f2ff]/0 via-[#00f2ff]/5 to-[#00f2ff]/0 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000"></div>
          <div className="flex items-center gap-2 mb-3">
            <Cpu className="w-4 h-4 text-[#00f2ff]" />
            <span className="text-xs font-bold text-[#e1fdff]">Engine Status</span>
          </div>
          <div className="space-y-2">
            <div className="flex justify-between items-center text-[10px]">
              <span className="text-[#dee1f7]/60">Model</span>
              <span className="text-[#00f2ff] font-mono">v2.4.0-kinetic</span>
            </div>
            <div className="flex justify-between items-center text-[10px]">
              <span className="text-[#dee1f7]/60">Latency</span>
              <span className="text-green-400 font-mono">24ms</span>
            </div>
          </div>
        </div>

        <Link href="/create" className="block text-center w-full bg-surface-container-highest/50 border border-primary-container/20 text-[#00f2ff] py-2.5 rounded-lg text-[11px] font-bold uppercase tracking-widest font-label hover:bg-primary-container/10 hover:shadow-[0_0_15px_rgba(0,242,255,0.15)] transition-all active:scale-95">
          Initialize Project
        </Link>
        
        <div className="pt-4 space-y-1 border-t border-outline-variant/10">
          {bottomNavItems.map((item) => {
            const Icon = item.icon;
            return (
              <Link 
                key={item.name}
                href={item.href}
                className="flex items-center gap-3 px-3 py-2 text-[#dee1f7]/40 text-[10px] font-bold uppercase font-label rounded-lg hover:text-[#dee1f7] hover:bg-white/5 transition-colors group"
              >
                <Icon className="w-3.5 h-3.5 group-hover:text-[#00f2ff] transition-colors" />
                <span>{item.name}</span>
              </Link>
            );
          })}
        </div>
      </div>
    </aside>
  );
}
