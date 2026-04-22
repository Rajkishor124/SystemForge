import { toast } from 'sonner';
import { ApiError } from './api';

// ─── Error Code → User Action Mapping ────────────────────────────────────────

interface ErrorConfig {
  title: string;
  description?: string;
  type: 'error' | 'warning' | 'info';
  action?: () => void;
}

/**
 * Maps backend ErrorCode values to user-facing toast notifications.
 *
 * Usage:
 * ```tsx
 * try {
 *   await api('/some/endpoint', { method: 'POST' });
 * } catch (err) {
 *   handleApiError(err);
 * }
 * ```
 *
 * Uses the machine-readable `errorCode` field (e.g., "AUTH_001") for
 * type-safe handling. Falls back to `err.message` for unknown codes.
 */
export function handleApiError(
  error: unknown,
  options?: { onSessionExpired?: () => void }
): void {
  if (!(error instanceof ApiError)) {
    toast.error('Something went wrong', {
      description: error instanceof Error ? error.message : 'An unexpected error occurred',
    });
    return;
  }

  const config = resolveErrorConfig(error, options);

  switch (config.type) {
    case 'error':
      toast.error(config.title, { description: config.description });
      break;
    case 'warning':
      toast.warning(config.title, { description: config.description });
      break;
    case 'info':
      toast.info(config.title, { description: config.description });
      break;
  }

  config.action?.();
}

// ─── Error Code Resolution ───────────────────────────────────────────────────

function resolveErrorConfig(
  error: ApiError,
  options?: { onSessionExpired?: () => void }
): ErrorConfig {
  const code = error.errorCode;

  switch (code) {
    // ─── Auth ──────────────────────────────────────────────────
    case 'AUTH_001':
      return {
        title: 'Invalid credentials',
        description: 'Please check your email and password.',
        type: 'error',
      };

    case 'AUTH_002':
    case 'AUTH_006':
      return {
        title: 'Session expired',
        description: 'Please sign in again to continue.',
        type: 'warning',
        action: options?.onSessionExpired,
      };

    case 'AUTH_003':
      return {
        title: 'Authentication error',
        description: 'Your session is invalid. Please sign in again.',
        type: 'error',
        action: options?.onSessionExpired,
      };

    case 'AUTH_004':
      return {
        title: 'Account locked',
        description: 'Your account has been temporarily locked. Contact support.',
        type: 'error',
      };

    case 'AUTH_007':
      return {
        title: 'OTP expired',
        description: 'The verification code has expired. Request a new one.',
        type: 'warning',
      };

    case 'AUTH_008':
      return {
        title: 'Too many attempts',
        description: 'You\'ve entered too many incorrect codes. Try again later.',
        type: 'error',
      };

    // ─── Rate Limiting ────────────────────────────────────────
    case 'RATE_001':
      return {
        title: 'Slow down',
        description: 'Too many requests. Please wait a moment and try again.',
        type: 'warning',
      };

    // ─── System / Generation ──────────────────────────────────
    case 'SYS_001':
      return {
        title: 'Config not found',
        description: 'The system configuration could not be found.',
        type: 'error',
      };

    case 'SYS_002':
      return {
        title: 'Already generated',
        description: 'This architecture has already been generated. View your results.',
        type: 'info',
      };

    case 'SYS_003':
      return {
        title: 'Generation in progress',
        description: 'Your architecture is already being generated. Please wait.',
        type: 'info',
      };

    case 'SYS_004':
      return {
        title: 'Generation failed',
        description: 'Something went wrong during AI generation. Please try again.',
        type: 'error',
      };

    // ─── AI / Resilience ──────────────────────────────────────
    case 'AI_001':
      return {
        title: 'AI service unavailable',
        description: 'The AI service is temporarily down. Using rule-based fallback.',
        type: 'warning',
      };

    case 'AI_002':
      return {
        title: 'AI request timed out',
        description: 'The AI took too long to respond. Please try again.',
        type: 'warning',
      };

    // ─── Validation ───────────────────────────────────────────
    case 'VAL_001':
      return {
        title: 'Validation failed',
        description: 'Please check the form fields and try again.',
        type: 'error',
      };

    case 'VAL_003':
      return {
        title: 'Request too large',
        description: 'The data you\'re sending is too large. Reduce the size and try again.',
        type: 'error',
      };

    // ─── General ──────────────────────────────────────────────
    case 'GEN_001':
      return {
        title: 'Not found',
        description: 'The requested resource could not be found.',
        type: 'error',
      };

    case 'GEN_002':
      return {
        title: 'Duplicate entry',
        description: 'This resource already exists.',
        type: 'warning',
      };

    case 'GEN_003':
      return {
        title: 'Access denied',
        description: 'You don\'t have permission to perform this action.',
        type: 'error',
      };

    // ─── Fallback ─────────────────────────────────────────────
    default:
      return resolveByHttpStatus(error);
  }
}

function resolveByHttpStatus(error: ApiError): ErrorConfig {
  switch (error.status) {
    case 401:
      return {
        title: 'Unauthorized',
        description: 'Please sign in to continue.',
        type: 'error',
      };
    case 403:
      return {
        title: 'Forbidden',
        description: 'You don\'t have permission to do this.',
        type: 'error',
      };
    case 429:
      return {
        title: 'Too many requests',
        description: 'Please slow down and try again shortly.',
        type: 'warning',
      };
    case 500:
    case 502:
    case 503:
      return {
        title: 'Server error',
        description: 'Something went wrong on our end. Please try again later.',
        type: 'error',
      };
    default:
      return {
        title: 'Error',
        description: error.message || 'An unexpected error occurred.',
        type: 'error',
      };
  }
}
