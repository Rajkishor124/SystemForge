import { Sidebar } from '@/components/layout/sidebar';
import { Topbar } from '@/components/layout/topbar';
import { AuthGuard } from '@/lib/auth-guard';

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthGuard>
      <div className="min-h-screen bg-background flex flex-col">
        <Topbar />
        <div className="flex flex-1 pt-16 h-[calc(100vh-64px)] overflow-hidden">
          <Sidebar />
          <main className="flex-1 flex flex-col bg-surface-container-lowest relative md:ml-64 overflow-y-auto custom-scrollbar">
            {children}
          </main>
        </div>
      </div>
    </AuthGuard>
  );
}
