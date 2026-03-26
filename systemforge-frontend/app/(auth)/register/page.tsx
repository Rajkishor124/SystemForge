'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Eye, EyeOff, Terminal, Loader2 } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { ApiError } from '@/lib/api';

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();

  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    if (!/^(?=.*[A-Za-z])(?=.*\d).+$/.test(password)) {
      setError('Password must contain at least one letter and one number.');
      return;
    }

    setLoading(true);
    try {
      await register(name, email, password);
      router.push('/dashboard');
    } catch (err) {
      if (err instanceof ApiError) {
        // Extract specific field validation errors if they exist (422)
        if (err.status === 422 && err.response?.data && typeof err.response.data === 'object') {
          const fieldErrors = Object.values(err.response.data).join(', ');
          setError(fieldErrors || err.message);
        } else {
          setError(err.message);
        }
      } else {
        setError('Something went wrong. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="bg-background font-body text-on-surface mesh-bg min-h-screen flex flex-col">
      <header className="fixed top-0 left-0 w-full z-50 bg-[#0e1322]/80 backdrop-blur-md shadow-2xl shadow-[#000000]/50">
        <div className="flex justify-between items-center w-full px-8 py-4 max-w-7xl mx-auto">
          <Link href="/" className="font-label text-lg font-bold tracking-tighter text-[#e1fdff]">
            SystemForge
          </Link>
          <Link href="/" className="font-headline text-[0.875rem] leading-relaxed antialiased text-[#dee1f7]/70 hover:text-[#00f2ff] hover:bg-white/5 transition-all duration-300 px-3 py-1 rounded">
            Back to Site
          </Link>
        </div>
      </header>

      <main className="flex-grow flex items-center justify-center px-4 pt-20 pb-12">
        <div className="w-full max-w-md">
          <div className="text-center mb-8">
            <h1 className="font-headline text-3xl font-bold tracking-tight text-primary mb-2">Initialize Architect</h1>
            <p className="font-label text-sm tracking-wide uppercase opacity-60">Start building smarter systems today</p>
          </div>

          <div className="glass-card p-8 rounded-xl shadow-[0_20px_40px_rgba(0,0,0,0.4)] relative overflow-hidden">
            {error && (
              <div className="mb-6 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-sm text-center">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-5">
              <div className="space-y-1.5">
                <label className="font-label text-[0.75rem] tracking-wider uppercase opacity-60 px-1">Full Name</label>
                <div className="relative group">
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Enter your full name"
                    required
                    minLength={2}
                    maxLength={100}
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-3 px-4 text-sm focus:ring-1 focus:ring-primary-container focus:border-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="font-label text-[0.75rem] tracking-wider uppercase opacity-60 px-1">Work Email</label>
                <div className="relative group">
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="name@company.com"
                    required
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-3 px-4 text-sm focus:ring-1 focus:ring-primary-container focus:border-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
                  />
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="font-label text-[0.75rem] tracking-wider uppercase opacity-60 px-1">Password</label>
                <div className="relative group">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                    minLength={8}
                    maxLength={72}
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-3 px-4 pr-10 text-sm focus:ring-1 focus:ring-primary-container focus:border-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
                  />
                  <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant/40 hover:text-primary-container transition-colors">
                    {showPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
              </div>
              <div className="space-y-1.5">
                <label className="font-label text-[0.75rem] tracking-wider uppercase opacity-60 px-1">Confirm Password</label>
                <div className="relative group">
                  <input
                    type={showConfirmPassword ? "text" : "password"}
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                    minLength={8}
                    maxLength={72}
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-3 px-4 pr-10 text-sm focus:ring-1 focus:ring-primary-container focus:border-primary-container outline-none transition-all placeholder:text-on-surface-variant/40"
                  />
                  <button type="button" onClick={() => setShowConfirmPassword(!showConfirmPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-on-surface-variant/40 hover:text-primary-container transition-colors">
                    {showConfirmPassword ? <EyeOff className="w-5 h-5" /> : <Eye className="w-5 h-5" />}
                  </button>
                </div>
              </div>
              
              <button
                type="submit"
                disabled={loading}
                className="block text-center w-full py-4 rounded-lg bg-gradient-to-r from-secondary-container to-primary-container text-on-primary font-semibold text-sm tracking-wide shadow-lg shadow-primary-container/20 hover:shadow-primary-container/40 active:scale-[0.98] transition-all duration-300 mt-2 disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <span className="flex items-center justify-center gap-2">
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Creating Account...
                  </span>
                ) : (
                  'Create Account'
                )}
              </button>
            </form>

            <div className="flex items-center gap-4 my-6">
              <div className="flex-grow h-[1px] bg-outline-variant/20"></div>
              <span className="font-label text-[0.65rem] opacity-40 uppercase tracking-widest">or continue with</span>
              <div className="flex-grow h-[1px] bg-outline-variant/20"></div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <button className="flex items-center justify-center gap-2 py-2.5 rounded-lg border border-outline-variant/20 bg-surface-container-lowest hover:bg-white/5 transition-all text-[0.8rem] font-medium">
                Google
              </button>
              <button className="flex items-center justify-center gap-2 py-2.5 rounded-lg border border-outline-variant/20 bg-surface-container-lowest hover:bg-white/5 transition-all text-[0.8rem] font-medium">
                <Terminal className="w-[18px] h-[18px]" />
                GitHub
              </button>
            </div>
          </div>

          <p className="text-center mt-8 text-sm text-on-surface/60">
            Already have an account? 
            <Link href="/login" className="text-primary-container hover:underline decoration-primary-container/40 underline-offset-4 transition-all ml-1">Login</Link>
          </p>
        </div>
      </main>

      <footer className="w-full py-12">
        <div className="flex flex-col md:flex-row justify-between items-center px-12 max-w-7xl mx-auto gap-4">
          <p className="font-label text-[0.75rem] tracking-wide uppercase opacity-60">
            © 2024 SystemForge. Engineered for precision.
          </p>
          <div className="flex gap-6">
            <Link href="#" className="font-label text-[0.75rem] tracking-wide uppercase opacity-60 text-[#dee1f7]/40 hover:text-[#00f2ff] transition-opacity duration-200">Privacy Policy</Link>
            <Link href="#" className="font-label text-[0.75rem] tracking-wide uppercase opacity-60 text-[#dee1f7]/40 hover:text-[#00f2ff] transition-opacity duration-200">Terms of Service</Link>
            <Link href="#" className="font-label text-[0.75rem] tracking-wide uppercase opacity-60 text-[#dee1f7]/40 hover:text-[#00f2ff] transition-opacity duration-200">Security</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
