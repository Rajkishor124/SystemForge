'use client';

import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import { useRouter } from 'next/navigation';
import {
  api,
  ApiError,
  type AuthUser,
} from './api';

// ─── Types ──────────────────────────────────────────────────────────────────────

interface User {
  userId: string;
  role: string;
}

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

// ─── Provider ───────────────────────────────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
  });

  /**
   * Hydrate auth state on mount by calling GET /auth/me.
   *
   * This replaces the old localStorage-based hydration. The browser
   * automatically sends the HttpOnly access_token cookie, and the
   * backend returns userId + role if the session is valid.
   */
  useEffect(() => {
    let cancelled = false;

    async function hydrate() {
      try {
        const res = await api<AuthUser>('/api/v1/auth/me');
        if (!cancelled && res.data) {
          setState({
            user: { userId: res.data.userId, role: res.data.role },
            isAuthenticated: true,
            isLoading: false,
          });
        }
      } catch {
        // No valid session — user is not authenticated
        if (!cancelled) {
          setState({
            user: null,
            isAuthenticated: false,
            isLoading: false,
          });
        }
      }
    }

    hydrate();
    return () => { cancelled = true; };
  }, []);

  const login = useCallback(
    async (email: string, password: string) => {
      // Backend sets HttpOnly cookies via Set-Cookie headers
      const res = await api<AuthUser>('/api/v1/auth/login', {
        method: 'POST',
        body: { email, password },
        skipAuth: true,
      });

      const auth = res.data;
      setState({
        user: { userId: auth.userId, role: auth.role },
        isAuthenticated: true,
        isLoading: false,
      });
    },
    []
  );

  const register = useCallback(
    async (name: string, email: string, password: string) => {
      // Backend sets HttpOnly cookies via Set-Cookie headers
      const res = await api<AuthUser>('/api/v1/auth/register', {
        method: 'POST',
        body: { name, email, password },
        skipAuth: true,
      });

      const auth = res.data;
      setState({
        user: { userId: auth.userId, role: auth.role },
        isAuthenticated: true,
        isLoading: false,
      });
    },
    []
  );

  const logout = useCallback(async () => {
    try {
      // Backend clears cookies via Set-Cookie with maxAge=0
      await api('/api/v1/auth/logout', { method: 'POST' });
    } catch {
      // Even if the server call fails, clear local state
    }
    setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
    router.push('/login');
  }, [router]);

  return (
    <AuthContext.Provider
      value={{
        ...state,
        login,
        register,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

// ─── Hook ───────────────────────────────────────────────────────────────────────

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
