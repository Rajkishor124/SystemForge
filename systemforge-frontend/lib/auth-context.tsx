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
  storeTokens,
  getStoredTokens,
  clearTokens,
  ApiError,
  type AuthTokens,
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
  const [state, setState] = useState<AuthState>(() => {
    if (typeof window !== 'undefined') {
      const stored = getStoredTokens();
      if (stored?.accessToken && stored.user) {
        return {
          user: {
            userId: stored.user.userId,
            role: stored.user.role,
          },
          isAuthenticated: true,
          isLoading: false,
        };
      }
    }
    return {
      user: null,
      isAuthenticated: false,
      isLoading: false,
    };
  });

  const login = useCallback(
    async (email: string, password: string) => {
      const res = await api<AuthTokens>('/api/v1/auth/login', {
        method: 'POST',
        body: { email, password },
        skipAuth: true,
      });

      const auth = res.data;
      storeTokens(auth);

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
      const res = await api<AuthTokens>('/api/v1/auth/register', {
        method: 'POST',
        body: { name, email, password },
        skipAuth: true,
      });

      const auth = res.data;
      storeTokens(auth);

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
      await api('/api/v1/auth/logout', { method: 'POST' });
    } catch {
      // Even if the server call fails, clear local state
    }
    clearTokens();
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
