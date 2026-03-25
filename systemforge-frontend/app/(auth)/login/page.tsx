'use client';

import { useState, type FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { Terminal, ArrowRight, Mail, Lock, Loader2 } from 'lucide-react';
import { useAuth } from '@/lib/auth-context';
import { ApiError } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const { login } = useAuth();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await login(email, password);
      router.push('/dashboard');
    } catch (err) {
      if (err instanceof ApiError) {
        // Extract specific field validation errors if they exist (422)
        if (err.status === 422 && err.response.data && typeof err.response.data === 'object') {
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
    <div className="font-body text-on-surface min-h-screen flex flex-col selection:bg-primary-container selection:text-on-primary-container bg-[#0e1322] bg-[radial-gradient(at_0%_0%,rgba(5,102,217,0.15)_0%,transparent_50%),radial-gradient(at_100%_0%,rgba(0,242,255,0.1)_0%,transparent_50%),radial-gradient(at_50%_100%,rgba(15,20,35,1)_0%,transparent_80%)]">
      <header className="fixed top-0 left-0 w-full z-50 flex justify-between items-center px-8 py-6">
        <Link href="/" className="font-label text-lg font-bold tracking-tighter text-[#e1fdff] flex items-center gap-2">
          <Terminal className="text-primary-container w-6 h-6" />
          SystemForge
        </Link>
        <Link href="/" className="font-label text-[0.75rem] tracking-wide uppercase opacity-60 hover:opacity-100 transition-opacity">
          Back to Site
        </Link>
      </header>

      <main className="flex-grow flex items-center justify-center px-6 py-24">
        <div className="w-full max-w-[440px]">
          <div className="bg-[#2f3445]/40 backdrop-blur-md border border-outline-variant/20 rounded-xl p-8 md:p-10 shadow-2xl">
            <div className="mb-10 text-center">
              <h1 className="font-headline text-3xl font-semibold tracking-tight text-primary mb-2">Welcome Back</h1>
              <p className="font-body text-on-surface-variant text-sm">Access your engineering environment</p>
            </div>

            {error && (
              <div className="mb-6 p-3 rounded-lg bg-red-500/10 border border-red-500/30 text-red-400 text-sm text-center">
                {error}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label htmlFor="email" className="block font-label text-[0.7rem] uppercase tracking-widest text-on-surface-variant mb-2">Work Email</label>
                <div className="relative group">
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant w-5 h-5 group-focus-within:text-primary-container transition-colors" />
                  <input
                    type="email"
                    id="email"
                    name="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="engineer@systemforge.io"
                    required
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-3.5 pl-12 pr-4 text-sm focus:outline-none focus:ring-1 focus:ring-primary-container focus:border-primary-container/40 transition-all placeholder:text-on-surface-variant/30"
                  />
                </div>
              </div>
              <div>
                <div className="flex justify-between items-center mb-2">
                  <label htmlFor="password" className="block font-label text-[0.7rem] uppercase tracking-widest text-on-surface-variant">Password</label>
                  <Link href="#" className="font-label text-[0.7rem] uppercase tracking-widest text-primary-container hover:text-primary transition-colors">Forgot Password?</Link>
                </div>
                <div className="relative group">
                  <Lock className="absolute left-4 top-1/2 -translate-y-1/2 text-on-surface-variant w-5 h-5 group-focus-within:text-primary-container transition-colors" />
                  <input
                    type="password"
                    id="password"
                    name="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="••••••••"
                    required
                    minLength={8}
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-3.5 pl-12 pr-4 text-sm focus:outline-none focus:ring-1 focus:ring-primary-container focus:border-primary-container/40 transition-all placeholder:text-on-surface-variant/30"
                  />
                </div>
              </div>
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-gradient-to-br from-[#00f2ff] to-[#0566d9] hover:shadow-[0_0_20px_rgba(0,242,255,0.4)] text-on-primary font-semibold py-4 rounded-lg flex items-center justify-center gap-2 active:scale-[0.98] transition-all duration-300 shadow-lg disabled:opacity-60 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    <span>Signing In...</span>
                  </>
                ) : (
                  <>
                    <span>Sign In</span>
                    <ArrowRight className="w-5 h-5" />
                  </>
                )}
              </button>
            </form>
            
            <div className="mt-8 mb-8 flex items-center gap-4">
              <div className="h-[1px] flex-grow bg-outline-variant/20"></div>
              <span className="font-label text-[0.65rem] uppercase tracking-widest text-on-surface-variant">or continue with</span>
              <div className="h-[1px] flex-grow bg-outline-variant/20"></div>
            </div>
            
            <button className="w-full bg-surface-container-high hover:bg-surface-variant border border-outline-variant/10 py-3.5 rounded-lg flex items-center justify-center gap-3 transition-colors active:scale-[0.98]">
              <span className="text-sm font-medium">Google</span>
            </button>
            
            <p className="mt-10 text-center font-body text-sm text-on-surface-variant">
              Don&apos;t have an account? 
              <Link href="/register" className="text-primary-container font-medium hover:underline underline-offset-4 decoration-primary-container/40 ml-1">Sign Up</Link>
            </p>
          </div>
          
          <div className="mt-12 text-center">
            <p className="font-label text-[0.75rem] tracking-widest uppercase opacity-40">
              Precision Engineered for Backend Architects
            </p>
          </div>
        </div>
      </main>

      <footer className="w-full py-12">
        <div className="flex flex-col md:flex-row justify-between items-center px-12 max-w-7xl mx-auto gap-4">
          <p className="font-label text-[0.75rem] tracking-wide uppercase opacity-60">
            © 2024 SystemForge. Engineered for precision.
          </p>
          <div className="flex gap-8">
            <Link href="#" className="font-label text-[0.75rem] tracking-wide uppercase opacity-60 hover:text-[#00f2ff] transition-opacity duration-200">Privacy Policy</Link>
            <Link href="#" className="font-label text-[0.75rem] tracking-wide uppercase opacity-60 hover:text-[#00f2ff] transition-opacity duration-200">Terms of Service</Link>
            <Link href="#" className="font-label text-[0.75rem] tracking-wide uppercase opacity-60 hover:text-[#00f2ff] transition-opacity duration-200">Security</Link>
          </div>
        </div>
      </footer>
    </div>
  );
}
