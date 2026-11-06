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

  // Native Android full-bleed layout
  return (
    <div className="w-full min-h-[100dvh] h-[100dvh] flex flex-col bg-slate-50 text-slate-800 overflow-hidden relative select-none antialiased font-sans">
      <div className="flex-1 w-full h-full flex flex-col overflow-hidden relative">
        {children}
      </div>
    </div>
  );
}
