'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Settings, Bell, Terminal, Menu } from 'lucide-react';
import Image from 'next/image';
import { motion } from 'motion/react';

const navLinks = [
  { name: 'Dashboard', href: '/dashboard' },
  { name: 'Architecture', href: '/architecture' },
  { name: 'AI Chat', href: '/chat' },
  { name: 'Schema', href: '/result' },
];

export function Topbar() {
  const pathname = usePathname();

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

        <Link href="/settings" className="w-8 h-8 md:w-9 md:h-9 rounded-full overflow-hidden border-2 border-primary-container/20 hover:border-[#00f2ff] transition-colors block shrink-0">
          <Image 
            src="https://picsum.photos/seed/dev/100/100" 
            alt="Developer Profile" 
            width={36} 
            height={36} 
            className="w-full h-full object-cover"
            referrerPolicy="no-referrer"
          />
        </Link>
      </div>
    </header>
  );
}
