'use client';

import { useEffect } from 'react';
import { AlertTriangle, RefreshCw, Home } from 'lucide-react';

/**
 * Global error boundary for the entire application.
 *
 * Catches any unhandled error in the React component tree and
 * renders a branded error page with recovery options.
 *
 * Next.js automatically wraps page components in this boundary
 * when it exists as `app/error.tsx`.
 */
export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // Log to console (future: send to Sentry/DataDog)
    console.error('[GlobalError]', error);
  }, [error]);

  return (
    <div className="min-h-screen flex items-center justify-center p-8 bg-[#0c0e14]">
      <div className="max-w-md w-full text-center">
        {/* Error Icon */}
        <div className="w-20 h-20 mx-auto mb-8 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center">
          <AlertTriangle className="w-10 h-10 text-red-400" />
        </div>

        <h1 className="text-2xl font-bold font-headline mb-3 text-[#e1fdff]">
          Something went wrong
        </h1>

        <p className="text-sm text-[#dee1f7]/60 mb-2">
          An unexpected error occurred. Our team has been notified.
        </p>

        {error.digest && (
          <p className="text-[10px] font-mono text-[#dee1f7]/20 mb-8">
            Error ID: {error.digest}
          </p>
        )}

        {!error.digest && <div className="mb-8" />}

        {/* Recovery Actions */}
        <div className="flex gap-3 justify-center">
          <a
            href="/"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors text-[#dee1f7]"
          >
            <Home className="w-4 h-4" />
            Go Home
          </a>
          <button
            onClick={() => reset()}
            className="inline-flex items-center gap-2 cta-gradient text-on-primary px-5 py-2.5 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform"
          >
            <RefreshCw className="w-4 h-4" />
            Try Again
          </button>
        </div>
      </div>
    </div>
  );
}
