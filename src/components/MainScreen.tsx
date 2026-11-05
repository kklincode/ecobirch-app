import React, { useState, useEffect, useRef } from "react";
import { Territory, LeafAnalysis } from "../types";
import { mockTerritories, createSampleTerritories } from "../mockData";
import { telegramHaptic } from "../telegram";
import { 
  loadJournalSync, 
  persistJournal, 
  loadFromIndexedDB, 
  loadFromTelegramCloud, 
  flushJournalImmediate 
} from "../storage";
import ScanScreen from "./ScanScreen";
import MapScreen from "./MapScreen";
import TerritoryScreen from "./TerritoryScreen";
import MethodologyScreen from "./MethodologyScreen";
import { Camera, Map, ClipboardList, BookOpen } from "lucide-react";

const STORAGE_KEY = "birch_leaf_journal_v2";

export default function MainScreen() {
  const [activeTab, setActiveTab] = useState<"scan" | "map" | "history" | "info">("scan");
  
  // Real reactive state holding all territories and measurements with multi-tier persistence
  const [territories, setTerritories] = useState<Territory[]>(() => {
    const loaded = loadJournalSync();
    if (loaded && loaded.length > 0) {
      return loaded;
    }
    return mockTerritories;
  });

  // Cross-screen preset navigation variables
  const [selectedTerritoryIdForScan, setSelectedTerritoryIdForScan] = useState<string | null>(null);
  const [activeTerritoryIdFromMap, setActiveTerritoryIdFromMap] = useState<string | null>(null);
  const [mapInitialAddMode, setMapInitialAddMode] = useState(false);
  const [pendingMeasurement, setPendingMeasurement] = useState<LeafAnalysis | null>(null);

  const territoriesRef = useRef<Territory[]>(territories);
  useEffect(() => {
    territoriesRef.current = territories;
  }, [territories]);

  // Synchronize state with persistent storage on every change
  useEffect(() => {
    persistJournal(territories);
  }, [territories]);

  // Rehydrate from durable IndexedDB or Telegram CloudStorage if localStorage was cleared on app exit
  useEffect(() => {
    async function rehydrateDurableStorage() {
      if (territories.length === 0) {
        const idbData = await loadFromIndexedDB();
        if (idbData && idbData.length > 0) {
          setTerritories(idbData);
          return;
        }
        const cloudData = await loadFromTelegramCloud();
        if (cloudData && cloudData.length > 0) {
          setTerritories(cloudData);
        }
      }
    }
    rehydrateDurableStorage();
  }, []);

  // Flush state immediately when exiting the app, closing the tab, or switching apps
  useEffect(() => {
    const handleExit = () => {
      flushJournalImmediate(territoriesRef.current);
    };

    window.addEventListener("pagehide", handleExit);
    window.addEventListener("beforeunload", handleExit);
    document.addEventListener("visibilitychange", () => {
      if (document.visibilityState === "hidden") {
        handleExit();
      }
    });

    return () => {
      window.removeEventListener("pagehide", handleExit);
      window.removeEventListener("beforeunload", handleExit);
    };
  }, []);

  // Add new territory from map
  const handleAddTerritory = (name: string, desc: string, lat: number, lng: number) => {
    const newT: Territory = {
      id: "t_" + Date.now(),
      name: name,
      description: desc,
      latitude: lat,
      longitude: lng,
      timestamp: new Date().toISOString(),
      leaves: pendingMeasurement ? [pendingMeasurement] : [],
    };
    setTerritories(prev => [newT, ...prev]);
    telegramHaptic("success");
    if (pendingMeasurement) {
      setPendingMeasurement(null);
    }
  };

  // Add measurement back to territory
  const handleAddNewMeasurement = (
    territoryId: string, 
    measurement: LeafAnalysis, 
    customCoords?: { latitude: number; longitude: number }
  ) => {
    setTerritories(prevTerritories => {
      // Check if this is a temp or new territory that doesn't exist in our state yet
      const exists = prevTerritories.some(t => t.id === territoryId);
      
      if (!exists && (territoryId.startsWith("temp_") || territoryId === "" || territoryId === "new")) {
        const lat = customCoords?.latitude ?? 43.125;
        const lng = customCoords?.longitude ?? 131.895;
        const newT: Territory = {
          id: "t_" + Date.now(),
          name: "Точка замера #" + (prevTerritories.length + 1),
          description: "Создана автоматически в процессе сканирования",
          latitude: lat,
          longitude: lng,
          timestamp: new Date().toISOString(),
          leaves: [measurement]
        };
        return [newT, ...prevTerritories];
      }

      return prevTerritories.map(t => {
        if (t.id === territoryId) {
          return {
            ...t,
            leaves: [measurement, ...t.leaves]
          };
        }
        return t;
      });
    });

    telegramHaptic("success");
    // Reset preset selector
    setSelectedTerritoryIdForScan(null);
  };

  // Remove individual leaf record
  const handleDeleteLeaf = (territoryId: string, leafId: string) => {
    setTerritories(prev =>
      prev.map(t => {
        if (t.id === territoryId) {
          return {
            ...t,
            leaves: t.leaves.filter(l => l.id !== leafId),
          };
        }
        return t;
      })
    );
    telegramHaptic("light");
  };

  // Remove entire territory
  const handleDeleteTerritory = (territoryId: string) => {
    setTerritories(prev => prev.filter(t => t.id !== territoryId));
    telegramHaptic("warning");
  };

  // Clean entire journal (Clear all territories and storage)
  const handleClearJournal = () => {
    setTerritories([]);
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {}
    telegramHaptic("warning");
  };

  // Clear demo templates
  const handleClearTemplates = () => {
    setTerritories(prev => prev.filter(t => !["t1", "t2", "t3"].includes(t.id)));
    telegramHaptic("light");
  };

  // Load 7-day realistic demo data
  const handleLoadDemoData = () => {
    const demo = createSampleTerritories();
    setTerritories(demo);
    telegramHaptic("success");
  };

  // Import journal from JSON
  const handleImportJournal = (imported: Territory[]) => {
    if (Array.isArray(imported)) {
      setTerritories(imported);
      telegramHaptic("success");
    }
  };

  // Navigations presets helper
  const handleSelectTerritoryForScan = (territoryId: string) => {
    setSelectedTerritoryIdForScan(territoryId);
    setActiveTab("scan");
    telegramHaptic("selection");
  };

  const handleSelectTerritoryOnMap = (territoryId: string) => {
    setActiveTerritoryIdFromMap(territoryId);
    setActiveTab("history");
    telegramHaptic("selection");
  };

  const switchTab = (tab: "scan" | "map" | "history" | "info") => {
    setActiveTab(tab);
    telegramHaptic("light");
  };

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-slate-50 relative">
      
      {/* Dynamic Screen Viewport */}
      <div className="flex-grow overflow-hidden flex flex-col relative">
        {activeTab === "scan" && (
          <ScanScreen 
            territories={territories}
            onAddNewMeasurement={handleAddNewMeasurement}
            selectedTerritoryId={selectedTerritoryIdForScan}
            onNavigateToHistory={() => switchTab("history")}
            onNavigateToMap={() => {
              setMapInitialAddMode(true);
              switchTab("map");
            }}
            onSetPendingMeasurement={setPendingMeasurement}
          />
        )}
        
        {activeTab === "map" && (
          <MapScreen 
            territories={territories}
            onSelectTerritory={handleSelectTerritoryOnMap}
            onAddTerritory={handleAddTerritory}
            initialAddMode={mapInitialAddMode}
            clearInitialAddMode={() => setMapInitialAddMode(false)}
            hasPendingMeasurement={!!pendingMeasurement}
            onCancelPendingMeasurement={() => setPendingMeasurement(null)}
          />
        )}

        {activeTab === "history" && (
          <TerritoryScreen 
            territories={territories}
            onSelectTerritoryForScan={handleSelectTerritoryForScan}
            onDeleteLeaf={handleDeleteLeaf}
            onDeleteTerritory={handleDeleteTerritory}
            activeTerritoryIdFromMap={activeTerritoryIdFromMap}
            clearActiveTerritoryIdFromMap={() => setActiveTerritoryIdFromMap(null)}
            onClearTemplates={handleClearTemplates}
            onClearJournal={handleClearJournal}
            onLoadDemoData={handleLoadDemoData}
            onImportJournal={handleImportJournal}
            onNavigateToScan={() => switchTab("scan")}
            onNavigateToMap={() => {
              setMapInitialAddMode(true);
              switchTab("map");
            }}
          />
        )}

        {activeTab === "info" && (
          <MethodologyScreen />
        )}
      </div>

      {/* Modern floating glass bottom navigation bar for Telegram Mini App & Mobile */}
      <nav 
        aria-label="Основная навигация" 
        className="h-16 border-t border-slate-200/70 bg-white/90 backdrop-blur-xl px-4 flex items-center justify-around shrink-0 z-40 select-none pb-[env(safe-area-inset-bottom,0px)] shadow-[0_-4px_20px_rgba(0,0,0,0.03)]"
      >
        {/* TAB 1: SCAN */}
        <button
          onClick={() => switchTab("scan")}
          className={`flex flex-col items-center gap-1 transition-all outline-none cursor-pointer tap-bounce py-1 px-3 rounded-2xl relative ${
            activeTab === "scan" ? "text-emerald-700" : "text-slate-400 hover:text-slate-600"
          }`}
        >
          <div className={`p-1.5 rounded-xl transition-all ${
            activeTab === "scan" 
              ? "bg-emerald-600 text-white shadow-sm shadow-emerald-600/30 ring-2 ring-emerald-600/20" 
              : "bg-transparent text-slate-500"
          }`}>
            <Camera className="w-4 h-4 stroke-[2.2]" />
          </div>
          <span className={`text-[10px] tracking-tight ${activeTab === "scan" ? "font-bold text-emerald-800" : "font-medium"}`}>
            Сканер
          </span>
        </button>

        {/* TAB 2: MAP */}
        <button
          onClick={() => {
            switchTab("map");
            setSelectedTerritoryIdForScan(null);
          }}
          className={`flex flex-col items-center gap-1 transition-all outline-none cursor-pointer tap-bounce py-1 px-3 rounded-2xl relative ${
            activeTab === "map" ? "text-emerald-700" : "text-slate-400 hover:text-slate-600"
          }`}
        >
          <div className={`p-1.5 rounded-xl transition-all ${
            activeTab === "map" 
              ? "bg-emerald-600 text-white shadow-sm shadow-emerald-600/30 ring-2 ring-emerald-600/20" 
              : "bg-transparent text-slate-500"
          }`}>
            <Map className="w-4 h-4 stroke-[2.2]" />
          </div>
          <span className={`text-[10px] tracking-tight ${activeTab === "map" ? "font-bold text-emerald-800" : "font-medium"}`}>
            Карта
          </span>
        </button>

        {/* TAB 3: HISTORY ARCHIVE */}
        <button
          onClick={() => switchTab("history")}
          className={`flex flex-col items-center gap-1 transition-all outline-none cursor-pointer tap-bounce py-1 px-3 rounded-2xl relative ${
            activeTab === "history" ? "text-emerald-700" : "text-slate-400 hover:text-slate-600"
          }`}
        >
          <div className={`p-1.5 rounded-xl transition-all relative ${
            activeTab === "history" 
              ? "bg-emerald-600 text-white shadow-sm shadow-emerald-600/30 ring-2 ring-emerald-600/20" 
              : "bg-transparent text-slate-500"
          }`}>
            <ClipboardList className="w-4 h-4 stroke-[2.2]" />
            {territories.length > 0 && activeTab !== "history" && (
              <span className="absolute -top-0.5 -right-0.5 w-2 h-2 rounded-full bg-emerald-500 ring-2 ring-white" />
            )}
          </div>
          <span className={`text-[10px] tracking-tight ${activeTab === "history" ? "font-bold text-emerald-800" : "font-medium"}`}>
            Журнал
          </span>
        </button>

        {/* TAB 4: METHODOLOGY GUIDE */}
        <button
          onClick={() => switchTab("info")}
          className={`flex flex-col items-center gap-1 transition-all outline-none cursor-pointer tap-bounce py-1 px-3 rounded-2xl relative ${
            activeTab === "info" ? "text-emerald-700" : "text-slate-400 hover:text-slate-600"
          }`}
        >
          <div className={`p-1.5 rounded-xl transition-all ${
            activeTab === "info" 
              ? "bg-emerald-600 text-white shadow-sm shadow-emerald-600/30 ring-2 ring-emerald-600/20" 
              : "bg-transparent text-slate-500"
          }`}>
            <BookOpen className="w-4 h-4 stroke-[2.2]" />
          </div>
          <span className={`text-[10px] tracking-tight ${activeTab === "info" ? "font-bold text-emerald-800" : "font-medium"}`}>
            Методика
          </span>
        </button>

      </nav>

    </div>
  );
}
