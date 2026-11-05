/**
 * Resilient multi-tier journal storage for EcoBirch FA Analyzer.
 * Supports:
 *  1. LocalStorage (instant synchronous access)
 *  2. IndexedDB (durable persistent offline storage that survives webview resets)
 *  3. Telegram CloudStorage (synced across sessions/devices within @bpaqi_bot)
 *  4. Automated image compression so leaf photos never trigger QuotaExceededError.
 */

import { Territory } from "./types";
import { getTelegramWebApp } from "./telegram";

const STORAGE_KEY = "birch_leaf_journal_v2";
const CLOUD_KEY = "birch_journal_data";
const DB_NAME = "BirchLeafFA_DB";
const STORE_NAME = "journal_store";

/**
 * Downscale large base64 image data URLs to lightweight JPEG thumbnails (~15-30KB)
 * to ensure localStorage quota (5MB) is never exceeded even with hundreds of scans.
 */
export async function compressImageDataUrl(
  dataUrl: string,
  maxWidth = 600,
  quality = 0.65
): Promise<string> {
  // If already template or small enough, return as-is
  if (!dataUrl || dataUrl === "template" || dataUrl.length < 50000) {
    return dataUrl;
  }

  return new Promise((resolve) => {
    try {
      const img = new Image();
      img.onload = () => {
        let width = img.width;
        let height = img.height;

        if (width > maxWidth || height > maxWidth) {
          if (width > height) {
            height = Math.round((height * maxWidth) / width);
            width = maxWidth;
          } else {
            width = Math.round((width * maxWidth) / height);
            height = maxWidth;
          }
        }

        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext("2d");
        if (!ctx) {
          resolve(dataUrl);
          return;
        }

        ctx.drawImage(img, 0, 0, width, height);
        const compressed = canvas.toDataURL("image/jpeg", quality);
        resolve(compressed);
      };
      img.onerror = () => resolve(dataUrl);
      img.src = dataUrl;
    } catch {
      resolve(dataUrl);
    }
  });
}

/**
 * Open or init IndexedDB
 */
function openDB(): Promise<IDBDatabase | null> {
  if (typeof window === "undefined" || !window.indexedDB) {
    return Promise.resolve(null);
  }

  return new Promise((resolve) => {
    try {
      const request = indexedDB.open(DB_NAME, 1);
      request.onupgradeneeded = () => {
        const db = request.result;
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME);
        }
      };
      request.onsuccess = () => resolve(request.result);
      request.onerror = () => resolve(null);
    } catch {
      resolve(null);
    }
  });
}

/**
 * Save to IndexedDB
 */
async function saveToIndexedDB(territories: Territory[]): Promise<void> {
  try {
    const db = await openDB();
    if (!db) return;

    const tx = db.transaction(STORE_NAME, "readwrite");
    const store = tx.objectStore(STORE_NAME);
    store.put(territories, "current_journal");
  } catch (err) {
    console.warn("IndexedDB save error:", err);
  }
}

/**
 * Load from IndexedDB
 */
export async function loadFromIndexedDB(): Promise<Territory[] | null> {
  try {
    const db = await openDB();
    if (!db) return null;

    return new Promise((resolve) => {
      const tx = db.transaction(STORE_NAME, "readonly");
      const store = tx.objectStore(STORE_NAME);
      const req = store.get("current_journal");
      req.onsuccess = () => {
        if (Array.isArray(req.result) && req.result.length > 0) {
          resolve(req.result);
        } else {
          resolve(null);
        }
      };
      req.onerror = () => resolve(null);
    });
  } catch {
    return null;
  }
}

/**
 * Synchronous initial load from localStorage
 */
export function loadJournalSync(): Territory[] {
  if (typeof window === "undefined") return [];

  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      const parsed = JSON.parse(saved);
      if (Array.isArray(parsed)) {
        return parsed;
      }
    }
  } catch (err) {
    console.warn("localStorage read failed:", err);
  }

  return [];
}

/**
 * Check if current Telegram WebApp supports CloudStorage (available since Bot API 6.9)
 */
function isTelegramCloudStorageSupported(tg: any): boolean {
  if (!tg) return false;
  // If Telegram provided isVersionAtLeast method, verify minimum version 6.9
  if (typeof tg.isVersionAtLeast === "function") {
    try {
      if (!tg.isVersionAtLeast("6.9")) {
        return false;
      }
    } catch {
      return false;
    }
  } else if (typeof tg.version === "string") {
    // Basic fallback comparison
    const [major, minor] = tg.version.split(".").map((n: string) => parseInt(n, 10));
    if (isNaN(major) || major < 6 || (major === 6 && (isNaN(minor) || minor < 9))) {
      return false;
    }
  }
  return Boolean(tg.CloudStorage && typeof tg.CloudStorage.setItem === "function");
}

/**
 * Save to Telegram Mini App CloudStorage if available and supported by version
 */
function saveToTelegramCloud(territories: Territory[]): void {
  const tg = getTelegramWebApp() as any;
  if (!isTelegramCloudStorageSupported(tg)) return;

  try {
    // Compress lightweight JSON for Telegram Cloud Storage
    const compact = territories.map((t) => ({
      ...t,
      leaves: t.leaves.map((l) => ({
        ...l,
        // Strip heavy base64 for cloud payload to stay within Telegram cloud storage item limits
        photoUrl: l.photoUrl.startsWith("data:") ? "stored_locally" : l.photoUrl,
      })),
    }));

    const json = JSON.stringify(compact);
    if (json.length < 4000) {
      tg.CloudStorage.setItem(CLOUD_KEY, json, () => {});
    }
  } catch (err) {
    // Silent catch without console warnings to prevent cluttering TMA log
  }
}

/**
 * Load from Telegram Mini App CloudStorage if available and supported by version
 */
export function loadFromTelegramCloud(): Promise<Territory[] | null> {
  const tg = getTelegramWebApp() as any;
  if (!isTelegramCloudStorageSupported(tg)) return Promise.resolve(null);

  return new Promise((resolve) => {
    try {
      tg.CloudStorage.getItem(CLOUD_KEY, (err: any, result?: string) => {
        if (!err && result) {
          try {
            const parsed = JSON.parse(result);
            if (Array.isArray(parsed) && parsed.length > 0) {
              resolve(parsed);
              return;
            }
          } catch {
            // Ignore parse error
          }
        }
        resolve(null);
      });
    } catch {
      resolve(null);
    }
  });
}

/**
 * Primary save routine:
 * 1. Sanitizes & compresses images
 * 2. Writes to LocalStorage (with fallback in case of quota)
 * 3. Saves in background to IndexedDB
 * 4. Syncs to Telegram CloudStorage
 */
export async function persistJournal(territories: Territory[]): Promise<void> {
  if (typeof window === "undefined") return;

  // Process & compress any oversized leaf photos
  const sanitizedTerritories: Territory[] = await Promise.all(
    territories.map(async (t) => ({
      ...t,
      leaves: await Promise.all(
        t.leaves.map(async (l) => {
          if (l.photoUrl && l.photoUrl.length > 60000) {
            const compressed = await compressImageDataUrl(l.photoUrl, 500, 0.6);
            return { ...l, photoUrl: compressed };
          }
          return l;
        })
      ),
    }))
  );

  // 1. LocalStorage write
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(sanitizedTerritories));
  } catch (quotaError) {
    console.warn("localStorage quota hit, saving stripped fallback:", quotaError);
    // If full data exceeded quota, strip photoUrls in localStorage to preserve all metrics & coordinates
    try {
      const minimalTerritories = sanitizedTerritories.map((t) => ({
        ...t,
        leaves: t.leaves.map((l) => ({
          ...l,
          photoUrl: l.photoUrl.startsWith("data:") ? "stored_offline" : l.photoUrl,
        })),
      }));
      localStorage.setItem(STORAGE_KEY, JSON.stringify(minimalTerritories));
    } catch (e) {
      console.error("Critical: failed to write minimal journal to localStorage:", e);
    }
  }

  // 2. IndexedDB write (unlimited capacity)
  saveToIndexedDB(sanitizedTerritories).catch(() => {});

  // 3. Telegram Cloud Storage sync
  saveToTelegramCloud(sanitizedTerritories);
}

/**
 * Immediate synchronous flush on window exit / pagehide / beforeunload
 */
export function flushJournalImmediate(territories: Territory[]): void {
  if (typeof window === "undefined") return;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(territories));
  } catch (err) {
    try {
      const minimal = territories.map((t) => ({
        ...t,
        leaves: t.leaves.map((l) => ({
          ...l,
          photoUrl: l.photoUrl.startsWith("data:") ? "stored_offline" : l.photoUrl,
        })),
      }));
      localStorage.setItem(STORAGE_KEY, JSON.stringify(minimal));
    } catch {
      // Ignore
    }
  }
}
