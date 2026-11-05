import React, { useState, useEffect } from "react";
import {
  BOT_USERNAME,
  BOT_URL,
  BOT_APP_URL,
  getTelegramBotInfo,
  openBpaqiBotChat,
  shareReportToTelegram,
  sendDataToTelegramBot,
  telegramHaptic,
} from "../telegram";
import {
  Send,
  ExternalLink,
  Share2,
  CheckCircle2,
  AlertCircle,
  Copy,
  Check,
  Vibrate,
  Smartphone,
  Info,
  X,
  Bot,
  Sparkles,
} from "lucide-react";

interface TelegramBotModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export default function TelegramBotModal({ isOpen, onClose }: TelegramBotModalProps) {
  const [botInfo, setBotInfo] = useState(getTelegramBotInfo());
  const [copiedUrl, setCopiedUrl] = useState(false);
  const [testResult, setTestResult] = useState<string | null>(null);
  const [hapticTested, setHapticTested] = useState(false);

  useEffect(() => {
    if (isOpen) {
      setBotInfo(getTelegramBotInfo());
      setTestResult(null);
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const currentAppUrl = typeof window !== "undefined" ? window.location.origin : BOT_APP_URL;

  const handleCopyAppUrl = () => {
    telegramHaptic("light");
    if (navigator.clipboard) {
      navigator.clipboard.writeText(currentAppUrl);
      setCopiedUrl(true);
      setTimeout(() => setCopiedUrl(false), 2200);
    }
  };

  const handleTestHaptic = () => {
    telegramHaptic("heavy");
    setHapticTested(true);
    setTimeout(() => setHapticTested(false), 1500);
  };

  const handleTestDataSend = () => {
    const payload = {
      event: "diagnostic_ping",
      bot: BOT_USERNAME,
      timestamp: new Date().toISOString(),
      source: "Birch Leaf FA Analyzer",
      status: "integration_verified",
      message: "Тестовое сообщение из мини-приложения в бот t.me/bpaqi_bot",
    };

    const res = sendDataToTelegramBot(payload);
    setTestResult(res.message);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3.5 bg-slate-950/70 backdrop-blur-xs animate-fade-in">
      <div className="bg-white rounded-3xl w-full max-w-md max-h-[90vh] shadow-2xl border border-slate-200 overflow-hidden flex flex-col animate-scale-up">
        {/* Modal Header */}
        <div className="p-4 bg-gradient-to-r from-emerald-800 to-teal-800 text-white flex items-center justify-between shrink-0">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-white/15 flex items-center justify-center text-white backdrop-blur-sm border border-white/20">
              <Bot className="w-5 h-5 text-emerald-300" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <h3 className="font-bold text-sm tracking-tight">Бот @{BOT_USERNAME}</h3>
                <span className="text-[10px] px-1.5 py-0.2 rounded-full bg-emerald-500/30 text-emerald-200 font-mono">
                  Telegram
                </span>
              </div>
              <p className="text-[11px] text-emerald-100/90">Интеграция с Telegram Mini App</p>
            </div>
          </div>
          <button
            onClick={() => {
              telegramHaptic("light");
              onClose();
            }}
            className="w-8 h-8 rounded-full bg-white/10 hover:bg-white/20 flex items-center justify-center text-white/90 cursor-pointer transition-colors"
            title="Закрыть"
          >
            <X size={16} />
          </button>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4 text-slate-700 text-xs">
          {/* Status banner */}
          <div
            className={`p-3 rounded-2xl border flex items-start gap-3 ${
              botInfo.isTMA
                ? "bg-emerald-50/80 border-emerald-200 text-emerald-900"
                : "bg-amber-50/80 border-amber-200 text-amber-900"
            }`}
          >
            {botInfo.isTMA ? (
              <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0 mt-0.5" />
            ) : (
              <Smartphone className="w-5 h-5 text-amber-600 shrink-0 mt-0.5" />
            )}
            <div className="flex-1">
              <div className="font-bold text-xs flex items-center gap-1.5">
                {botInfo.isTMA
                  ? "Приложение запущено внутри Telegram!"
                  : "Режим браузера / предпросмотра"}
              </div>
              <p className="text-[11px] opacity-90 mt-0.5 leading-snug">
                {botInfo.isTMA
                  ? `Пользователь: ${botInfo.user?.first_name || "Пользователь"} ${botInfo.user?.username ? `(@${botInfo.user.username})` : ""} (ID: ${botInfo.user?.id || "N/A"}). Связь с ботом @${BOT_USERNAME} активна.`
                  : `Для запуска внутри Telegram перейдите в бота @${BOT_USERNAME} или настройте меню Web App в @BotFather.`}
              </p>
            </div>
          </div>

          {/* Quick Actions */}
          <div className="space-y-2">
            <label className="text-[11px] font-bold text-slate-500 uppercase tracking-wider block">
              Быстрые действия
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() => openBpaqiBotChat()}
                className="p-3 rounded-2xl bg-emerald-600 hover:bg-emerald-700 active:scale-97 text-white font-bold flex flex-col items-center justify-center gap-1.5 shadow-sm cursor-pointer transition-all"
              >
                <ExternalLink size={18} />
                <span className="text-[11px]">Чат с @{BOT_USERNAME}</span>
              </button>

              <button
                onClick={() =>
                  shareReportToTelegram(
                    "🍃 Мониторинг качества воздуха по листьям березы в боте @" + BOT_USERNAME,
                    currentAppUrl
                  )
                }
                className="p-3 rounded-2xl bg-teal-600 hover:bg-teal-700 active:scale-97 text-white font-bold flex flex-col items-center justify-center gap-1.5 shadow-sm cursor-pointer transition-all"
              >
                <Share2 size={18} />
                <span className="text-[11px]">Поделиться ссылкой</span>
              </button>
            </div>
          </div>

          {/* Diagnostics and Testing */}
          <div className="bg-slate-50 rounded-2xl p-3.5 border border-slate-200/90 space-y-2.5">
            <div className="flex items-center justify-between">
              <span className="font-bold text-slate-800 text-xs flex items-center gap-1.5">
                <Sparkles size={13} className="text-emerald-600" />
                Диагностика API Telegram
              </span>
              <span className="text-[10px] font-mono text-slate-500 bg-white px-2 py-0.5 rounded border border-slate-200">
                v{botInfo.version}
              </span>
            </div>

            <div className="space-y-1.5 text-[11px]">
              <div className="flex items-center justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Целевой бот:</span>
                <a
                  href={BOT_URL}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="font-mono font-bold text-emerald-700 hover:underline flex items-center gap-1"
                >
                  t.me/{BOT_USERNAME}
                  <ExternalLink size={10} />
                </a>
              </div>
              <div className="flex items-center justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Платформа:</span>
                <span className="font-mono text-slate-700">{botInfo.platform}</span>
              </div>
              <div className="flex items-center justify-between py-1 border-b border-slate-200/60">
                <span className="text-slate-500">Виброотклик (Haptics):</span>
                <button
                  onClick={handleTestHaptic}
                  className="text-emerald-700 hover:text-emerald-800 font-bold inline-flex items-center gap-1 cursor-pointer"
                >
                  <Vibrate size={12} />
                  {hapticTested ? "Сработало!" : "Проверить вибро"}
                </button>
              </div>
              <div className="flex items-center justify-between py-1">
                <span className="text-slate-500">Канал данных sendData:</span>
                <span className="font-bold text-emerald-700">Готов к передаче</span>
              </div>
            </div>

            {/* Test Send Button */}
            <div className="pt-1">
              <button
                onClick={handleTestDataSend}
                className="w-full py-2.5 px-3 rounded-xl bg-white hover:bg-slate-100 border border-slate-300 text-slate-800 font-bold text-xs flex items-center justify-center gap-2 cursor-pointer transition-all active:scale-98 shadow-3xs"
              >
                <Send size={13} className="text-emerald-600" />
                Тест отправки отчета в @{BOT_USERNAME}
              </button>
            </div>

            {testResult && (
              <div className="p-2 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-[11px] font-medium flex items-center gap-2 animate-fade-in">
                <Check size={14} className="text-emerald-600 shrink-0" />
                <span>{testResult}</span>
              </div>
            )}
          </div>

          {/* Setup Guide in BotFather */}
          <div className="bg-emerald-50/50 rounded-2xl p-3.5 border border-emerald-150 space-y-2">
            <div className="font-bold text-slate-800 text-xs flex items-center gap-1.5">
              <Info size={13} className="text-emerald-700" />
              Как привязать Web App к @{BOT_USERNAME}:
            </div>

            <ol className="list-decimal list-inside space-y-1 text-[11px] text-slate-600 leading-snug">
              <li>Откройте бота <a href="https://t.me/BotFather" target="_blank" rel="noreferrer" className="text-emerald-700 font-bold underline">@BotFather</a> в Telegram</li>
              <li>Отправьте команду <code className="bg-white px-1 py-0.5 rounded border border-slate-200 text-emerald-800 font-mono">/mybots</code> и выберите <strong>@{BOT_USERNAME}</strong></li>
              <li>Перейдите в <strong>Bot Settings</strong> &rarr; <strong>Menu Button</strong> &rarr; <strong>Configure menu button</strong></li>
              <li>Вставьте URL приложения и укажите название кнопки (напр. <em>Оценка качества среды</em>)</li>
            </ol>

            {/* Current URL Copy Box */}
            <div className="pt-1">
              <div className="text-[10px] text-slate-500 font-medium mb-1">URL для @BotFather:</div>
              <div className="flex items-center gap-1.5 bg-white p-1.5 rounded-xl border border-slate-200">
                <input
                  type="text"
                  readOnly
                  value={currentAppUrl}
                  className="flex-1 bg-transparent border-none text-[10px] text-slate-600 font-mono focus:outline-none px-1 select-all"
                />
                <button
                  onClick={handleCopyAppUrl}
                  className="p-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 cursor-pointer transition-colors"
                  title="Скопировать ссылку"
                >
                  {copiedUrl ? <Check size={12} className="text-emerald-600" /> : <Copy size={12} />}
                </button>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="p-3 bg-slate-50 border-t border-slate-200 flex items-center justify-between text-xs">
          <a
            href={BOT_URL}
            target="_blank"
            rel="noopener noreferrer"
            className="text-emerald-700 hover:underline font-bold inline-flex items-center gap-1"
          >
            t.me/{BOT_USERNAME}
            <ExternalLink size={11} />
          </a>
          <button
            onClick={() => {
              telegramHaptic("light");
              onClose();
            }}
            className="px-4 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-900 text-white font-bold text-xs cursor-pointer transition-colors"
          >
            Понятно
          </button>
        </div>
      </div>
    </div>
  );
}
