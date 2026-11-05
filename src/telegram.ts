/**
 * Telegram WebApp SDK Integration Helper
 * Provides typed access to Telegram Mini App APIs, haptics, theme, and full-screen expansion.
 */

export const BOT_USERNAME = "bpaqi_bot";
export const BOT_URL = "https://t.me/bpaqi_bot";
export const BOT_APP_URL = "https://t.me/bpaqi_bot/app";

export interface TelegramUser {
  id: number;
  is_bot?: boolean;
  first_name: string;
  last_name?: string;
  username?: string;
  language_code?: string;
  photo_url?: string;
}

export interface TelegramThemeParams {
  bg_color?: string;
  text_color?: string;
  hint_color?: string;
  link_color?: string;
  button_color?: string;
  button_text_color?: string;
  secondary_bg_color?: string;
}

export interface TelegramWebAppAPI {
  initData: string;
  initDataUnsafe: {
    query_id?: string;
    user?: TelegramUser;
    receiver?: TelegramUser;
    start_param?: string;
    auth_date?: number;
    hash?: string;
  };
  version: string;
  platform: string;
  colorScheme: "light" | "dark";
  themeParams: TelegramThemeParams;
  isExpanded: boolean;
  viewportHeight: number;
  viewportStableHeight: number;
  headerColor: string;
  backgroundColor: string;
  isClosingConfirmationEnabled: boolean;
  BackButton: {
    isVisible: boolean;
    onClick(callback: () => void): void;
    offClick(callback: () => void): void;
    show(): void;
    hide(): void;
  };
  MainButton: {
    text: string;
    color: string;
    textColor: string;
    isVisible: boolean;
    isActive: boolean;
    isProgressVisible: boolean;
    setText(text: string): void;
    onClick(callback: () => void): void;
    offClick(callback: () => void): void;
    show(): void;
    hide(): void;
    enable(): void;
    disable(): void;
    showProgress(leaveActive?: boolean): void;
    hideProgress(): void;
  };
  HapticFeedback: {
    impactOccurred(style: "light" | "medium" | "heavy" | "rigid" | "soft"): void;
    notificationOccurred(type: "error" | "success" | "warning"): void;
    selectionChanged(): void;
  };
  ready(): void;
  expand(): void;
  close(): void;
  sendData(data: string): void;
  showAlert(message: string, callback?: () => void): void;
  showConfirm(message: string, callback?: (confirmed: boolean) => void): void;
  showPopup?(params: { title?: string; message: string; buttons?: { id?: string; type?: string; text?: string }[] }, callback?: (buttonId: string) => void): void;
  setHeaderColor(color: string): void;
  setBackgroundColor(color: string): void;
  enableClosingConfirmation(): void;
  onEvent(eventType: string, eventHandler: () => void): void;
  offEvent(eventType: string, eventHandler: () => void): void;
  openLink(url: string, options?: { try_instant_view?: boolean }): void;
  openTelegramLink(url: string): void;
  switchInlineQuery?(query: string, choose_chat_types?: string[]): void;
}

declare global {
  interface Window {
    Telegram?: {
      WebApp?: TelegramWebAppAPI;
    };
  }
}

/**
 * Returns true if running inside Telegram Mini App
 */
export function isTelegramWebApp(): boolean {
  if (typeof window === "undefined") return false;
  const tg = window.Telegram?.WebApp;
  return Boolean(
    tg && (tg.initData || tg.version || window.location.search.includes("tgWebAppData"))
  );
}

/**
 * Get active Telegram WebApp instance if available
 */
export function getTelegramWebApp(): TelegramWebAppAPI | null {
  if (typeof window === "undefined") return null;
  return window.Telegram?.WebApp || null;
}

/**
 * Initialize Telegram Mini App: ready, expand viewport, enable closing confirmation
 */
export function initTelegramWebApp(): void {
  const tg = getTelegramWebApp();
  if (!tg) return;

  try {
    tg.ready();
    tg.expand();
    if (tg.enableClosingConfirmation) {
      tg.enableClosingConfirmation();
    }
    // Set matching emerald/slate header color for botanical identity
    if (tg.setHeaderColor) {
      tg.setHeaderColor("#065f46"); // Emerald-800
    }
  } catch (err) {
    console.warn("Telegram WebApp initialization error:", err);
  }
}

/**
 * Trigger subtle haptic feedback for user interactions
 */
export function telegramHaptic(
  type: "light" | "medium" | "heavy" | "selection" | "success" | "warning" | "error"
): void {
  const tg = getTelegramWebApp();
  if (!tg?.HapticFeedback) return;

  try {
    switch (type) {
      case "light":
      case "medium":
      case "heavy":
        tg.HapticFeedback.impactOccurred(type);
        break;
      case "selection":
        tg.HapticFeedback.selectionChanged();
        break;
      case "success":
      case "warning":
      case "error":
        tg.HapticFeedback.notificationOccurred(type);
        break;
    }
  } catch {
    // Ignore fallback
  }
}

/**
 * Get Telegram user information if available
 */
export function getTelegramUser(): TelegramUser | null {
  const tg = getTelegramWebApp();
  return tg?.initDataUnsafe?.user || null;
}

/**
 * Get unified Telegram bot status and context
 */
export function getTelegramBotInfo() {
  const tg = getTelegramWebApp();
  const inTma = isTelegramWebApp();
  const user = tg?.initDataUnsafe?.user || null;

  return {
    botUsername: BOT_USERNAME,
    botUrl: BOT_URL,
    botAppUrl: BOT_APP_URL,
    isTMA: inTma,
    user,
    version: tg?.version || "не подключен (в браузере)",
    platform: tg?.platform || "web",
    hasSendData: Boolean(tg?.sendData),
  };
}

/**
 * Open chat with @bpaqi_bot
 */
export function openBpaqiBotChat(startParam?: string): void {
  telegramHaptic("light");
  const tg = getTelegramWebApp();
  const targetUrl = startParam ? `${BOT_URL}?start=${encodeURIComponent(startParam)}` : BOT_URL;

  if (tg?.openTelegramLink) {
    try {
      tg.openTelegramLink(targetUrl);
      return;
    } catch (e) {
      console.warn("Failed to open via openTelegramLink:", e);
    }
  }

  // Browser fallback
  window.open(targetUrl, "_blank", "noopener,noreferrer");
}

/**
 * Share text report / result via Telegram Share Sheet
 */
export function shareReportToTelegram(text: string, customUrl?: string): void {
  telegramHaptic("medium");
  const tg = getTelegramWebApp();
  const appUrl = customUrl || (typeof window !== "undefined" ? window.location.origin : BOT_APP_URL);
  const shareLink = `https://t.me/share/url?url=${encodeURIComponent(appUrl)}&text=${encodeURIComponent(text)}`;

  if (tg?.openTelegramLink) {
    try {
      tg.openTelegramLink(shareLink);
      return;
    } catch (e) {
      console.warn("tg.openTelegramLink error:", e);
    }
  }

  window.open(shareLink, "_blank", "noopener,noreferrer");
}

/**
 * Send structured data back to @bpaqi_bot
 * 1. Attempts tg.sendData (available when launched via Reply Keyboard button)
 * 2. If not in Reply Keyboard or failed, falls back to copying report and opening @bpaqi_bot
 */
export function sendDataToTelegramBot(data: Record<string, unknown> | string): {
  success: boolean;
  method: "sendData" | "link" | "clipboard";
  message: string;
} {
  const tg = getTelegramWebApp();
  const payloadString = typeof data === "string" ? data : JSON.stringify(data);

  telegramHaptic("success");

  if (tg?.sendData) {
    try {
      tg.sendData(payloadString);
      return {
        success: true,
        method: "sendData",
        message: "Данные успешно отправлены в бот @bpaqi_bot!",
      };
    } catch (err) {
      console.warn("tg.sendData failed, using fallback:", err);
    }
  }

  // Fallback: Copy to clipboard and offer opening bot
  if (typeof navigator !== "undefined" && navigator.clipboard) {
    try {
      navigator.clipboard.writeText(payloadString);
    } catch {
      // Ignore
    }
  }

  return {
    success: true,
    method: "link",
    message: "Отчет сформирован для бота @bpaqi_bot.",
  };
}
