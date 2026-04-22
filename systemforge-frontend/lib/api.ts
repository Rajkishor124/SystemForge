/**
 * Centralized API client for SystemForge backend.
 *
 * Security model (post-hardening):
 * - Access token is stored in an HttpOnly cookie (set by the backend)
 * - Refresh token is stored in a path-scoped HttpOnly cookie
 * - Frontend NEVER handles raw JWT strings
 * - All requests use `credentials: 'include'` to send cookies automatically
 * - Token refresh is handled transparently on 401
 */

// ─── Types ──────────────────────────────────────────────────────────────────────

export interface ApiResponse<T = unknown> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
  correlationId?: string;
  errorCode?: string;
}

export interface AuthUser {
  userId: string;
  role: string;
  tokenType?: string;
  accessTokenExpiresAt?: string;
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
 * Automatically sends cookies and retries once on 401 (token refresh).
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

  const url = `${BASE_URL}${path}`;

  const response = await fetch(url, {
    ...rest,
    headers,
    credentials: 'include', // Send HttpOnly cookies with every request
    body: body ? JSON.stringify(body) : undefined,
  });

  // 401 → attempt cookie-based token refresh (once)
  if (response.status === 401 && !skipAuth && !isRefreshing) {
    const refreshed = await tryRefreshToken();
    if (refreshed) {
      // Retry original request — fresh cookie is already set by the refresh response
      const retryResponse = await fetch(url, {
        ...rest,
        headers,
        credentials: 'include',
        body: body ? JSON.stringify(body) : undefined,
      });
      return handleResponse<T>(retryResponse);
    }
    // Refresh failed — let caller handle the 401
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
    const errorCode = json?.errorCode;
    const error = new ApiError(message, response.status, json || undefined, errorCode);
    throw error;
  }

  if (!json) {
    return {
      success: true,
      message: 'Resource processed successfully',
      data: null as unknown as T,
      timestamp: new Date().toISOString()
    };
  }

  return json;
}

/**
 * Attempts to refresh the session via the HttpOnly refresh cookie.
 * The backend reads the cookie automatically — no token in the body.
 */
async function tryRefreshToken(): Promise<boolean> {
  isRefreshing = true;
  try {
    const response = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include', // Sends the sf_refresh_token cookie
    });

    if (!response.ok) return false;

    const json: ApiResponse<AuthUser> = await response.json();
    return json.success;
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
  errorCode?: string;

  constructor(message: string, status: number, response?: ApiResponse<any>, errorCode?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.response = response;
    this.errorCode = errorCode;
  }
}
