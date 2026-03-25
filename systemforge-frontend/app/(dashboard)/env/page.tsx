'use client';

import { useState } from 'react';
import { Save, Plus, Trash2, Eye, EyeOff, Key } from 'lucide-react';

export default function EnvVariablesPage() {
  const [variables, setVariables] = useState([
    { id: 1, key: 'DATABASE_URL', value: 'postgresql://user:password@localhost:5432/db', isSecret: true, show: false },
    { id: 2, key: 'API_KEY', value: 'sk_test_123456789', isSecret: true, show: false },
    { id: 3, key: 'NODE_ENV', value: 'development', isSecret: false, show: true },
    { id: 4, key: 'NEXT_PUBLIC_APP_URL', value: 'https://systemforge.io', isSecret: false, show: true },
  ]);

  const toggleVisibility = (id: number) => {
    setVariables(variables.map(v => v.id === id ? { ...v, show: !v.show } : v));
  };

  const removeVariable = (id: number) => {
    setVariables(variables.filter(v => v.id !== id));
  };

  const addVariable = () => {
    const newId = Math.max(...variables.map(v => v.id), 0) + 1;
    setVariables([...variables, { id: newId, key: '', value: '', isSecret: false, show: true }]);
  };

  return (
    <div className="p-8 max-w-5xl mx-auto w-full">
      <div className="mb-8 flex justify-between items-end">
        <div>
          <h1 className="text-3xl font-bold tracking-tight font-headline text-[#e1fdff] mb-2">Environment Variables</h1>
          <p className="text-[#dee1f7]/60 text-sm">Manage configuration and secrets for your generated architectures.</p>
        </div>
        <button 
          onClick={addVariable}
          className="bg-surface-container-highest hover:bg-primary-container/20 text-[#00f2ff] px-4 py-2 rounded-lg font-bold text-sm transition-all flex items-center gap-2 border border-primary-container/20"
        >
          <Plus className="w-4 h-4" /> Add Variable
        </button>
      </div>

      <div className="glass-card rounded-xl overflow-hidden border border-outline-variant/20">
        <div className="bg-surface-container-high/50 px-6 py-4 border-b border-outline-variant/20 grid grid-cols-12 gap-4 items-center">
          <div className="col-span-4 font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 font-bold">Key</div>
          <div className="col-span-6 font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 font-bold">Value</div>
          <div className="col-span-2 font-label text-xs uppercase tracking-widest text-[#dee1f7]/60 font-bold text-right">Actions</div>
        </div>
        
        <div className="divide-y divide-outline-variant/10">
          {variables.map((variable) => (
            <div key={variable.id} className="px-6 py-4 grid grid-cols-12 gap-4 items-center hover:bg-white/[0.02] transition-colors group">
              <div className="col-span-4">
                <div className="relative">
                  <Key className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-[#dee1f7]/40" />
                  <input 
                    type="text" 
                    value={variable.key}
                    onChange={(e) => setVariables(variables.map(v => v.id === variable.id ? { ...v, key: e.target.value } : v))}
                    placeholder="e.g. API_KEY"
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2 pl-9 pr-3 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#e1fdff] font-mono placeholder:text-[#dee1f7]/20 transition-all"
                  />
                </div>
              </div>
              <div className="col-span-6">
                <div className="relative">
                  <input 
                    type={variable.show || !variable.isSecret ? "text" : "password"} 
                    value={variable.value}
                    onChange={(e) => setVariables(variables.map(v => v.id === variable.id ? { ...v, value: e.target.value } : v))}
                    placeholder="Value"
                    className="w-full bg-surface-container-lowest border border-outline-variant/20 rounded-lg py-2 pl-3 pr-10 text-sm focus:ring-1 focus:ring-primary-container outline-none text-[#dee1f7] font-mono placeholder:text-[#dee1f7]/20 transition-all"
                  />
                  {variable.isSecret && (
                    <button 
                      onClick={() => toggleVisibility(variable.id)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-[#dee1f7]/40 hover:text-[#00f2ff] transition-colors"
                    >
                      {variable.show ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                    </button>
                  )}
                </div>
              </div>
              <div className="col-span-2 flex justify-end items-center gap-3">
                <label className="flex items-center gap-2 cursor-pointer group/secret">
                  <input 
                    type="checkbox" 
                    checked={variable.isSecret}
                    onChange={(e) => setVariables(variables.map(v => v.id === variable.id ? { ...v, isSecret: e.target.checked, show: !e.target.checked } : v))}
                    className="w-4 h-4 rounded border-outline-variant/30 text-primary-container focus:ring-primary-container/50 bg-surface-container-lowest cursor-pointer"
                  />
                  <span className="text-xs text-[#dee1f7]/60 group-hover/secret:text-[#dee1f7] transition-colors">Secret</span>
                </label>
                <button 
                  onClick={() => removeVariable(variable.id)}
                  className="p-2 text-[#dee1f7]/40 hover:text-red-400 hover:bg-red-400/10 rounded-lg transition-all opacity-0 group-hover:opacity-100"
                  title="Remove variable"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            </div>
          ))}
          
          {variables.length === 0 && (
            <div className="px-6 py-12 text-center text-[#dee1f7]/40 text-sm">
              No environment variables defined. Click &quot;Add Variable&quot; to create one.
            </div>
          )}
        </div>
        
        <div className="bg-surface-container-high/30 px-6 py-4 border-t border-outline-variant/20 flex justify-between items-center">
          <p className="text-xs text-[#dee1f7]/50">
            Variables marked as <span className="text-[#dee1f7]/80 font-bold">Secret</span> are encrypted at rest and hidden by default.
          </p>
          <button className="bg-gradient-to-br from-[#00f2ff] to-[#0566d9] text-on-primary px-6 py-2 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 hover:shadow-primary-container/40 active:scale-95 transition-all flex items-center gap-2">
            <Save className="w-4 h-4" /> Save Variables
          </button>
        </div>
      </div>
    </div>
  );
}
