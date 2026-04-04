'use client';

import { ReactNode } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { AlertCircle, Trash2, X } from 'lucide-react';

interface ConfirmModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  title: string;
  description: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'warning' | 'info';
  icon?: ReactNode;
}

export function ConfirmModal({
  isOpen,
  onClose,
  onConfirm,
  title,
  description,
  confirmLabel = 'Confirm',
  cancelLabel = 'Cancel',
  variant = 'danger',
  icon
}: ConfirmModalProps) {
  
  const getVariantStyles = () => {
    switch (variant) {
      case 'danger':
        return {
          iconClass: 'text-red-400 bg-red-400/10 border-red-400/20',
          buttonClass: 'bg-red-500 hover:bg-red-600 text-white shadow-red-500/20',
          DefaultIcon: Trash2
        };
      case 'warning':
        return {
          iconClass: 'text-yellow-400 bg-yellow-400/10 border-yellow-400/20',
          buttonClass: 'bg-yellow-500 hover:bg-yellow-600 text-black shadow-yellow-500/20',
          DefaultIcon: AlertCircle
        };
      case 'info':
      default:
        return {
          iconClass: 'text-[#00f2ff] bg-[#00f2ff]/10 border-[#00f2ff]/20',
          buttonClass: 'bg-[#00f2ff] hover:bg-[#00f2ff]/80 text-[#0e1322] shadow-[#00f2ff]/20',
          DefaultIcon: AlertCircle
        };
    }
  };

  const styles = getVariantStyles();
  const Icon = icon ? () => <>{icon}</> : styles.DefaultIcon;

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 z-50 bg-[#0e1322]/80 backdrop-blur-sm"
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 20 }}
            className="fixed inset-0 z-50 flex items-center justify-center p-4 pointer-events-none"
          >
            <div className="glass-card rounded-2xl p-6 w-full max-w-md pointer-events-auto border border-outline-variant/20 shadow-2xl relative overflow-hidden">
              {/* Subtle top decoration */}
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-white/10 to-transparent"></div>
              
              <button 
                onClick={onClose}
                className="absolute top-4 right-4 p-1.5 rounded-lg text-[#dee1f7]/40 hover:text-[#e1fdff] hover:bg-white/5 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>

              <div className="flex flex-col items-center text-center">
                <div className={`p-4 rounded-full border mb-4 ${styles.iconClass}`}>
                  <Icon className="w-8 h-8" />
                </div>
                
                <h3 className="text-xl font-bold font-headline text-[#e1fdff] mb-2">
                  {title}
                </h3>
                
                <p className="text-sm text-[#dee1f7]/70 mb-8 leading-relaxed">
                  {description}
                </p>

                <div className="flex items-center gap-3 w-full">
                  <button
                    onClick={onClose}
                    className="flex-1 px-4 py-2.5 rounded-xl font-bold text-sm bg-surface-container-lowest text-[#dee1f7]/80 hover:bg-surface-container border border-outline-variant/10 transition-all focus:ring-2 focus:ring-white/10 outline-none"
                  >
                    {cancelLabel}
                  </button>
                  <button
                    onClick={() => {
                      onConfirm();
                      onClose();
                    }}
                    className={`flex-1 px-4 py-2.5 rounded-xl font-bold text-sm shadow-lg transition-all active:scale-95 focus:ring-2 focus:ring-offset-2 focus:ring-offset-[#0e1322] outline-none ${styles.buttonClass}`}
                  >
                    {confirmLabel}
                  </button>
                </div>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
