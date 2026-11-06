import React, { Component, ReactNode, StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.tsx';
import './index.css';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

class GlobalErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error("Global React Crash Caught:", error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="w-full min-h-screen bg-emerald-950 text-white p-6 flex flex-col justify-center items-center font-sans select-none">
          <div className="w-16 h-16 rounded-full bg-emerald-800/80 border border-emerald-500/40 flex items-center justify-center mb-4 text-2xl shadow-lg">
            🍃
          </div>
          <h1 className="text-xl font-bold mb-2 text-center text-emerald-100">ЭкоБерёза ФА</h1>
          <p className="text-xs text-emerald-300/80 text-center max-w-xs mb-6">
            Инициализация компонентов приложения...
          </p>
          <div className="w-full max-w-sm bg-black/50 p-4 rounded-xl text-left border border-white/10 overflow-auto max-h-48 mb-6">
            <p className="text-[11px] font-mono text-red-300 break-all">
              {this.state.error?.message || "Ошибка загрузки скриптов"}
            </p>
          </div>
          <button
            onClick={() => {
              if (typeof localStorage !== "undefined") localStorage.clear();
              window.location.reload();
            }}
            className="w-full max-w-sm py-3 px-6 rounded-xl bg-emerald-600 hover:bg-emerald-500 active:scale-95 text-white font-bold text-sm shadow-lg transition-all text-center cursor-pointer"
          >
            Сбросить кэш и перезапустить
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <GlobalErrorBoundary>
      <App />
    </GlobalErrorBoundary>
  </StrictMode>,
);

