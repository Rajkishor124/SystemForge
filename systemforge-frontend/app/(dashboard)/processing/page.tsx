'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Brain, Database, Server, Shield, Zap, CheckCircle } from 'lucide-react';

export default function ProcessingPage() {
  const router = useRouter();
  const [step, setStep] = useState(0);

  const steps = [
    { icon: Brain, text: "Analyzing requirements & constraints..." },
    { icon: Database, text: "Designing data models & storage strategy..." },
    { icon: Server, text: "Mapping microservices & API gateways..." },
    { icon: Shield, text: "Applying security & auth patterns..." },
    { icon: Zap, text: "Optimizing for high availability & scale..." },
    { icon: CheckCircle, text: "Finalizing blueprint..." }
  ];

  useEffect(() => {
    const timer = setInterval(() => {
      setStep((prev) => {
        if (prev >= steps.length - 1) {
          clearInterval(timer);
          setTimeout(() => router.push('/result'), 1000);
          return prev;
        }
        return prev + 1;
      });
    }, 1200);

    return () => clearInterval(timer);
  }, [router, steps.length]);

  return (
    <div className="flex-1 flex items-center justify-center p-8">
      <div className="max-w-md w-full text-center">
        <div className="relative w-32 h-32 mx-auto mb-12">
          <div className="absolute inset-0 rounded-full border-4 border-surface-container-highest"></div>
          <div className="absolute inset-0 rounded-full border-4 border-primary-container border-t-transparent animate-spin"></div>
          <div className="absolute inset-0 flex items-center justify-center">
            <Brain className="w-12 h-12 text-primary-container animate-pulse" />
          </div>
          
          {/* Decorative nodes */}
          <div className="absolute -top-4 -left-4 w-8 h-8 rounded-full bg-surface-container border border-primary-container/30 flex items-center justify-center animate-bounce" style={{ animationDelay: '0ms' }}>
            <Database className="w-3 h-3 text-primary-container" />
          </div>
          <div className="absolute top-1/2 -right-6 w-8 h-8 rounded-full bg-surface-container border border-secondary-container/30 flex items-center justify-center animate-bounce" style={{ animationDelay: '200ms' }}>
            <Server className="w-3 h-3 text-secondary-container" />
          </div>
          <div className="absolute -bottom-4 left-4 w-8 h-8 rounded-full bg-surface-container border border-tertiary-container/30 flex items-center justify-center animate-bounce" style={{ animationDelay: '400ms' }}>
            <Zap className="w-3 h-3 text-tertiary-container" />
          </div>
        </div>

        <h2 className="text-2xl font-bold font-headline mb-8 text-[#e1fdff]">Synthesizing Architecture</h2>

        <div className="space-y-4 text-left">
          {steps.map((s, i) => {
            const Icon = s.icon;
            const isActive = i === step;
            const isPast = i < step;
            
            return (
              <div 
                key={i} 
                className={`flex items-center gap-4 p-3 rounded-lg transition-all duration-500 ${
                  isActive ? 'bg-primary-container/10 border border-primary-container/30' : 
                  isPast ? 'opacity-50' : 'opacity-20'
                }`}
              >
                <div className={`p-2 rounded-full ${isActive ? 'bg-primary-container text-on-primary' : isPast ? 'bg-surface-container-high text-primary-container' : 'bg-surface-container text-on-surface-variant'}`}>
                  {isPast ? <CheckCircle className="w-4 h-4" /> : <Icon className="w-4 h-4" />}
                </div>
                <span className={`text-sm font-medium ${isActive ? 'text-primary-container' : 'text-on-surface'}`}>
                  {s.text}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
