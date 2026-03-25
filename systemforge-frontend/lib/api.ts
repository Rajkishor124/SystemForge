/**
 * Centralized API client for SystemForge backend.
 *
 * - Injects Authorization header from localStorage tokens
 * - Auto-refreshes on 401 (one retry)
 * - Returns typed ApiResponse<T> matching the backend envelope
 */

// ─── Types ──────────────────────────────────────────────────────────────────────

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  correlationId?: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  accessTokenExpiresAt: string;
  userId: string;
  role: string;
}

// ─── Storage keys ───────────────────────────────────────────────────────────────

const TOKEN_KEY = 'sf_access_token';
const REFRESH_KEY = 'sf_refresh_token';
const USER_KEY = 'sf_user';

export function getStoredTokens() {
  if (typeof window === 'undefined') return null;
  const accessToken = localStorage.getItem(TOKEN_KEY);
  const refreshToken = localStorage.getItem(REFRESH_KEY);
  const user = localStorage.getItem(USER_KEY);
  if (!accessToken) return null;
  return {
    accessToken,
    refreshToken: refreshToken || '',
    user: user ? JSON.parse(user) : null,
  };
}

export function storeTokens(auth: AuthTokens) {
  localStorage.setItem(TOKEN_KEY, auth.accessToken);
  localStorage.setItem(REFRESH_KEY, auth.refreshToken);
  localStorage.setItem(
    USER_KEY,
    JSON.stringify({
      userId: auth.userId,
      role: auth.role,
      tokenType: auth.tokenType,
      accessTokenExpiresAt: auth.accessTokenExpiresAt,
    })
  );
}

export function clearTokens() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_KEY);
  localStorage.removeItem(USER_KEY);
}

// ─── Base fetch ─────────────────────────────────────────────────────────────────

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

interface FetchOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  skipAuth?: boolean;
}

let isRefreshing = false;

/**
 * Generic API call. Returns `ApiResponse<T>`.
 * Automatically injects auth header and retries once on 401.
 */
export async function api<T = unknown>(
  path: string,
  options: FetchOptions = {}
): Promise<ApiResponse<T>> {
  const { body, skipAuth, ...rest } = options;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(rest.headers as Record<string, string>),
  };

  // Inject auth token unless explicitly skipped
  if (!skipAuth) {
    const stored = getStoredTokens();
    if (stored?.accessToken) {
      headers['Authorization'] = `Bearer ${stored.accessToken}`;
    }
  }

  const url = `${BASE_URL}${path}`;

  const response = await fetch(url, {
    ...rest,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  });

  // 401 → attempt token refresh (once)
  if (response.status === 401 && !skipAuth && !isRefreshing) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      // Retry original request with new token
      const newStored = getStoredTokens();
      if (newStored?.accessToken) {
        headers['Authorization'] = `Bearer ${newStored.accessToken}`;
      }
      const retryResponse = await fetch(url, {
        ...rest,
        headers,
        body: body ? JSON.stringify(body) : undefined,
      });
      return handleResponse<T>(retryResponse);
    } else {
      // Refresh failed — clear tokens, let caller handle
      clearTokens();
    }
  }

  return handleResponse<T>(response);
}

async function handleResponse<T>(response: Response): Promise<ApiResponse<T>> {
  let json: ApiResponse<T> | null = null;
  const contentType = response.headers.get('content-type');

  if (contentType && contentType.includes('application/json')) {
    try {
      json = await response.json();
    } catch (e) {
      console.error('Failed to parse response as JSON:', e);
    }
  }

  if (!response.ok || (json && !json.success)) {
    const message = json?.message || `Request failed with status ${response.status}`;
    const error = new ApiError(message, response.status, json || undefined);
    throw error;
  }

  if (!json) {
    // If we're here, response.ok is true but no JSON was parsed
    // Return a dummy success wrapper if no data expected, or throw if data required
    return {
      success: true,
      message: 'Resource processed successfully',
      data: null as unknown as T,
      timestamp: new Date().toISOString()
    };
  }

  return json;
}

async function tryRefreshToken(): Promise<boolean> {
  isRefreshing = true;
  try {
    const stored = getStoredTokens();
    if (!stored?.refreshToken) return false;

    const response = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Refresh-Token': stored.refreshToken,
      },
    });

    if (!response.ok) return false;

    const json: ApiResponse<AuthTokens> = await response.json();
    if (json.success && json.data) {
      storeTokens(json.data);
      return true;
    }
    return false;
  } catch {
    return false;
  } finally {
    isRefreshing = false;
  }
}

// ─── Custom error class ─────────────────────────────────────────────────────────

export class ApiError extends Error {
  status: number;
  response?: ApiResponse<any>;

  constructor(message: string, status: number, response?: ApiResponse<any>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.response = response;
  }
}
