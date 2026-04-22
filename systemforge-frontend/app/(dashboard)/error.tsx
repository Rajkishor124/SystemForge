'use client';

import { useEffect } from 'react';
import { AlertTriangle, RefreshCw, LayoutDashboard } from 'lucide-react';

/**
 * Dashboard-scoped error boundary.
 *
 * Catches errors within the (dashboard) layout group.
 * The sidebar and navigation remain visible since this boundary
 * is nested inside the dashboard layout.
 */
export default function DashboardError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error('[DashboardError]', error);
  }, [error]);

  return (
    <div className="flex-1 flex items-center justify-center p-8">
      <div className="max-w-md w-full text-center glass-card rounded-xl p-8">
        {/* Error Icon */}
        <div className="w-16 h-16 mx-auto mb-6 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center">
          <AlertTriangle className="w-8 h-8 text-red-400" />
        </div>

        <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">
          Page Error
        </h2>

        <p className="text-sm text-[#dee1f7]/60 mb-2">
          This page encountered an error. Your data is safe.
        </p>

        {error.digest && (
          <p className="text-[10px] font-mono text-[#dee1f7]/20 mb-6">
            Error ID: {error.digest}
          </p>
        )}

        {!error.digest && <div className="mb-6" />}

        {/* Recovery Actions */}
        <div className="flex gap-3 justify-center">
          <a
            href="/dashboard"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors text-[#dee1f7]"
          >
            <LayoutDashboard className="w-4 h-4" />
            Dashboard
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
