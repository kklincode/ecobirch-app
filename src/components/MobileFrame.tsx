import React, { useState, useEffect } from "react";
import { isTelegramWebApp, initTelegramWebApp } from "../telegram";
import { Smartphone, Maximize2 } from "lucide-react";

interface MobileFrameProps {
  children: React.ReactNode;
}

export default function MobileFrame({ children }: MobileFrameProps) {
  const [isTelegram, setIsTelegram] = useState(false);
  const [forceTmaFullscreen, setForceTmaFullscreen] = useState(false);

  useEffect(() => {
    // Initialize Telegram WebApp SDK if available
    initTelegramWebApp();

    const inTg = isTelegramWebApp();
    setIsTelegram(inTg);

    // If running in iframe or standalone mobile webview, default to seamless full screen
    const isMobileDevice = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
      navigator.userAgent
    );
    if (inTg || isMobileDevice) {
      setForceTmaFullscreen(true);
    }
  }, []);

  // Full-bleed mode for Telegram Mini App or full-width mobile view
  if (isTelegram || forceTmaFullscreen) {
    return (
      <div className="w-full min-h-[100dvh] h-[100dvh] flex flex-col bg-slate-50 text-slate-800 overflow-hidden relative select-none antialiased font-sans">
        {/* Subtle toggle for desktop testing to return to phone frame view */}
        {!isTelegram && (
          <div className="hidden lg:flex fixed top-2 right-2 z-50">
            <button
              onClick={() => setForceTmaFullscreen(false)}
              className="bg-slate-900/80 hover:bg-slate-900 text-white text-[10px] font-bold px-2.5 py-1.5 rounded-full shadow-lg backdrop-blur-md flex items-center gap-1.5 cursor-pointer transition-all"
              title="Переключить в режим рамки телефона"
            >
              <Smartphone size={12} />
              <span>Рамка смартфона</span>
            </button>
          </div>
        )}
        <div className="flex-1 w-full h-full flex flex-col overflow-hidden relative">
          {children}
        </div>
      </div>
    );
  }

  // Desktop simulator mode with phone bezel
  return (
    <div className="min-h-screen w-full bg-slate-950 bg-[linear-gradient(rgba(15,23,42,0.85),rgba(15,23,42,0.95)),url('https://images.unsplash.com/photo-1502082553048-f009c37129b9?q=80&w=1200&auto=format&fit=crop')] bg-cover bg-center flex items-center justify-center p-0 md:p-6 text-slate-100 antialiased font-sans transition-all duration-300">
      
      {/* Phone Mockup Wrapper */}
      <div className="relative w-full max-w-md h-screen md:h-[860px] md:rounded-[44px] md:border-[10px] md:border-slate-900 md:shadow-[0_25px_60px_-15px_rgba(0,0,0,0.8)] overflow-hidden bg-slate-950 flex flex-col transition-all duration-300">
        
        {/* Desktop indicator for Telegram Mini App preview */}
        <div className="hidden md:flex items-center justify-between px-5 py-2 bg-slate-900/90 border-b border-slate-800 text-[10px] text-slate-400 font-mono">
          <a
            href="https://t.me/bpaqi_bot"
            target="_blank"
            rel="noreferrer"
            className="flex items-center gap-1.5 text-emerald-400 font-bold hover:underline transition-colors"
            title="Открыть бота @bpaqi_bot в Telegram"
          >
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            @bpaqi_bot Mini App
          </a>
          <button
            onClick={() => setForceTmaFullscreen(true)}
            className="hover:text-white flex items-center gap-1 cursor-pointer transition-colors"
            title="Развернуть на весь экран"
          >
            <Maximize2 size={11} />
            <span>На весь экран</span>
          </button>
        </div>

        {/* Dynamic Display Screen Content */}
        <div className="flex-1 w-full flex flex-col overflow-hidden bg-slate-50 text-slate-800 relative">
          {children}
        </div>

      </div>
    </div>
  );
}
