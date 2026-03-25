'use client';

import { useEffect, useState, useRef } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Brain, Database, Server, Shield, Zap, CheckCircle, AlertCircle } from 'lucide-react';
import { api, ApiError } from '@/lib/api';

export default function ProcessingPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const configId = searchParams.get('configId');

  const [step, setStep] = useState(0);
  const [error, setError] = useState<string | null>(null);
  const [generationDone, setGenerationDone] = useState(false);
  const apiCalledRef = useRef(false);

  const steps = [
    { icon: Brain, text: "Analyzing requirements & constraints..." },
    { icon: Database, text: "Designing data models & storage strategy..." },
    { icon: Server, text: "Mapping microservices & API gateways..." },
    { icon: Shield, text: "Applying security & auth patterns..." },
    { icon: Zap, text: "Optimizing for high availability & scale..." },
    { icon: CheckCircle, text: "Finalizing blueprint..." }
  ];

  // Trigger the real AI generation
  useEffect(() => {
    if (!configId || apiCalledRef.current) return;
    apiCalledRef.current = true;

    async function generate() {
      try {
        await api(`/api/v1/systems/configs/${configId}/generate`, {
          method: 'POST',
        });
        setGenerationDone(true);
      } catch (err) {
        if (err instanceof ApiError) {
          // If already generated, treat as success
          if (err.message?.includes('already generated')) {
            setGenerationDone(true);
          } else {
            setError(err.message);
          }
        } else {
          setError('Architecture generation failed. Please try again.');
        }
      }
    }

    generate();
  }, [configId]);

  // Step animation (visual only — runs in parallel with the API call)
  useEffect(() => {
    const timer = setInterval(() => {
      setStep((prev) => {
        if (prev >= steps.length - 1) {
          clearInterval(timer);
          return prev;
        }
        return prev + 1;
      });
    }, 1200);

    return () => clearInterval(timer);
  }, [steps.length]);

  // Redirect once BOTH animation is done AND API is done
  useEffect(() => {
    if (generationDone && step >= steps.length - 1) {
      const timeout = setTimeout(() => {
        router.push(`/result?configId=${configId}`);
      }, 800);
      return () => clearTimeout(timeout);
    }
  }, [generationDone, step, steps.length, configId, router]);

  // No configId → redirect back
  useEffect(() => {
    if (!configId) {
      router.push('/create');
    }
  }, [configId, router]);

  if (error) {
    return (
      <div className="flex-1 flex items-center justify-center p-8">
        <div className="max-w-md w-full text-center glass-card rounded-xl p-8">
          <div className="w-16 h-16 mx-auto mb-6 rounded-full bg-red-500/10 flex items-center justify-center">
            <AlertCircle className="w-8 h-8 text-red-400" />
          </div>
          <h2 className="text-xl font-bold font-headline mb-3 text-[#e1fdff]">Generation Failed</h2>
          <p className="text-sm text-[#dee1f7]/60 mb-6">{error}</p>
          <div className="flex gap-3 justify-center">
            <button
              onClick={() => router.push('/create')}
              className="px-5 py-2.5 rounded-lg bg-surface-container border border-outline-variant/20 text-sm font-bold hover:bg-surface-container-high transition-colors"
            >
              Back to Create
            </button>
            <button
              onClick={() => { setError(null); apiCalledRef.current = false; setStep(0); }}
              className="cta-gradient text-on-primary px-5 py-2.5 rounded-lg font-bold text-sm shadow-lg shadow-primary-container/20 active:scale-95 transition-transform"
            >
              Retry
            </button>
          </div>
        </div>
      </div>
    );
  }

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

        {generationDone && step >= steps.length - 1 && (
          <p className="mt-6 text-sm text-primary-container animate-pulse">Redirecting to results...</p>
        )}
      </div>
    </div>
  );
}
