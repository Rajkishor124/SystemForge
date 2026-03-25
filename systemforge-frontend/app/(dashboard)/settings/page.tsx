import { Save, User, Shield, Key, CreditCard } from 'lucide-react';

export default function SettingsPage() {
  return (
    <div className="p-8 max-w-4xl mx-auto w-full">
      <div className="mb-8">
        <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">Settings</h1>
        <p className="text-[#dee1f7]/60 text-sm">Manage your account preferences and API integrations.</p>
      </div>

      <div className="grid md:grid-cols-4 gap-8">
        <div className="md:col-span-1 space-y-2">
          <button className="w-full text-left px-4 py-2 rounded-lg bg-primary-container/10 text-primary-container font-bold text-sm flex items-center gap-3">
            <User className="w-4 h-4" /> Profile
          </button>
          <button className="w-full text-left px-4 py-2 rounded-lg hover:bg-surface-container text-[#dee1f7]/60 hover:text-[#dee1f7] font-bold text-sm flex items-center gap-3 transition-colors">
            <Shield className="w-4 h-4" /> Security
          </button>
          <button className="w-full text-left px-4 py-2 rounded-lg hover:bg-surface-container text-[#dee1f7]/60 hover:text-[#dee1f7] font-bold text-sm flex items-center gap-3 transition-colors">
            <Key className="w-4 h-4" /> API Keys
          </button>
          <button className="w-full text-left px-4 py-2 rounded-lg hover:bg-surface-container text-[#dee1f7]/60 hover:text-[#dee1f7] font-bold text-sm flex items-center gap-3 transition-colors">
            <CreditCard className="w-4 h-4" /> Billing
          </button>
        </div>

        <div className="md:col-span-3 space-y-6">
          <div className="glass-card rounded-xl p-6">
            <h2 className="text-xl font-bold font-headline mb-6 border-b border-outline-variant/20 pb-2">Profile Information</h2>
            <form className="space-y-4">
              <div className="grid sm:grid-cols-2 gap-4">
                <div>
                  <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">First Name</label>
                  <input type="text" defaultValue="Alex" className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]" />
                </div>
                <div>
                  <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Last Name</label>
                  <input type="text" defaultValue="Developer" className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]" />
                </div>
              </div>
              <div>
                <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Email Address</label>
                <input type="email" defaultValue="alex@systemforge.io" className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]" />
              </div>
              <div>
                <label className="font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 mb-2 block">Role</label>
                <select className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2.5 px-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7]">
                  <option>Backend Engineer</option>
                  <option>Full Stack Developer</option>
                  <option>Solutions Architect</option>
                  <option>CTO</option>
                </select>
              </div>
              <div className="pt-4 flex justify-end">
                <button type="button" className="cta-gradient text-on-primary px-6 py-2 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform flex items-center gap-2">
                  <Save className="w-4 h-4" /> Save Changes
                </button>
              </div>
            </form>
          </div>

          <div className="glass-card rounded-xl p-6">
            <h2 className="text-xl font-bold font-headline mb-6 border-b border-outline-variant/20 pb-2">Preferences</h2>
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-bold text-sm text-[#e1fdff]">Email Notifications</div>
                  <div className="text-xs text-[#dee1f7]/60">Receive updates on architecture generation status.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" value="" className="sr-only peer" defaultChecked />
                  <div className="w-11 h-6 bg-surface-container-highest peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-container"></div>
                </label>
              </div>
              <div className="flex items-center justify-between">
                <div>
                  <div className="font-bold text-sm text-[#e1fdff]">Advanced AI Mode</div>
                  <div className="text-xs text-[#dee1f7]/60">Use more tokens for deeper architectural analysis.</div>
                </div>
                <label className="relative inline-flex items-center cursor-pointer">
                  <input type="checkbox" value="" className="sr-only peer" />
                  <div className="w-11 h-6 bg-surface-container-highest peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-primary-container"></div>
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
