import React, { useState, useRef } from "react";
import { Territory, LeafAnalysis } from "../types";
import { getEnvironmentalRating } from "../utils";
import { 
  telegramHaptic, 
  BOT_USERNAME, 
  sendDataToTelegramBot, 
  shareReportToTelegram 
} from "../telegram";
import { 
  Trash2, PlusCircle, ArrowLeft, ChevronDown, ChevronUp, MapPin, 
  AlignJustify, ClipboardList, Info, Share2, Copy, Check, ExternalLink, 
  QrCode, Leaf, Globe, Sparkles, X, Heart, ShieldCheck, Activity, 
  ChevronRight, BarChart2, Download, Upload, Camera, AlertCircle,
  Bot, Send
} from "lucide-react";
import FAWeeklyChart from "./FAWeeklyChart";

interface TerritoryScreenProps {
  territories: Territory[];
  onSelectTerritoryForScan: (territoryId: string) => void;
  onDeleteLeaf: (territoryId: string, leafId: string) => void;
  onDeleteTerritory: (territoryId: string) => void;
  activeTerritoryIdFromMap: string | null;
  clearActiveTerritoryIdFromMap: () => void;
  onClearTemplates?: () => void;
  onClearJournal?: () => void;
  onLoadDemoData?: () => void;
  onImportJournal?: (imported: Territory[]) => void;
  onNavigateToScan?: () => void;
  onNavigateToMap?: () => void;
}

export default function TerritoryScreen({
  territories,
  onSelectTerritoryForScan,
  onDeleteLeaf,
  onDeleteTerritory,
  activeTerritoryIdFromMap,
  clearActiveTerritoryIdFromMap,
  onClearTemplates,
  onClearJournal,
  onLoadDemoData,
  onImportJournal,
  onNavigateToScan,
  onNavigateToMap
}: TerritoryScreenProps) {
  
  // Set default initial active territory ID to either the one selected from the map, or the first one in the list.
  const [expandedTerritoryId, setExpandedTerritoryId] = useState<string | null>(activeTerritoryIdFromMap);
  const [expandedLeafId, setExpandedLeafId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [sharingData, setSharingData] = useState<{ territory: Territory; leaf: LeafAnalysis } | null>(null);
  const [copied, setCopied] = useState(false);
  const [viewMode, setViewMode] = useState<"chart" | "territories">("chart");
  const [showClearConfirmModal, setShowClearConfirmModal] = useState(false);
  const [botSentSuccess, setBotSentSuccess] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleExportJSON = () => {
    telegramHaptic("light");
    const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(territories, null, 2));
    const dlAnchorElem = document.createElement("a");
    dlAnchorElem.setAttribute("href", dataStr);
    dlAnchorElem.setAttribute("download", `birch_fa_journal_${new Date().toISOString().slice(0, 10)}.json`);
    dlAnchorElem.click();
  };

  const handleFileImport = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const json = JSON.parse(event.target?.result as string);
        if (Array.isArray(json)) {
          onImportJournal?.(json);
        } else {
          alert("Неверный формат файла");
        }
      } catch (err) {
        alert("Ошибка чтения JSON-файла");
      }
    };
    reader.readAsText(file);
    if (e.target) e.target.value = "";
  };

  const handleCopyLink = (leafId: string) => {
    const shareUrl = `${window.location.origin}/#leaf-${leafId}`;
    navigator.clipboard.writeText(shareUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleNativeShare = async (leafId: string, leafName: string, territoryName: string, zScore: number) => {
    const shareUrl = `${window.location.origin}/#leaf-${leafId}`;
    const rating = getEnvironmentalRating(zScore);
    try {
      if (navigator.share) {
        await navigator.share({
          title: `Эко-паспорт листа: ${leafName || "Лист берёзы"}`,
          text: `Экологический замер качества воздуха на территории: ${territoryName}. Класс качества среды: ${rating.score} (${rating.status}).`,
          url: shareUrl,
        });
      } else {
        handleCopyLink(leafId);
      }
    } catch (err) {
      console.warn("Share failed or canceled:", err);
      handleCopyLink(leafId);
    }
  };

  // If we have an active territory from the map, scroll to/highlight it!
  React.useEffect(() => {
    if (activeTerritoryIdFromMap) {
      setViewMode("territories");
      setExpandedTerritoryId(activeTerritoryIdFromMap);
      clearActiveTerritoryIdFromMap();
    }
  }, [activeTerritoryIdFromMap]);

  const toggleTerritory = (id: string) => {
    setExpandedTerritoryId(expandedTerritoryId === id ? null : id);
    setExpandedLeafId(null);
  };

  const toggleLeaf = (id: string, e: React.MouseEvent) => {
    e.stopPropagation();
    setExpandedLeafId(expandedLeafId === id ? null : id);
  };

  const getFAForTerritory = (territory: Territory) => {
    if (territory.leaves.length === 0) return 0;
    const sumZ = territory.leaves.reduce((sum, leaf) => sum + leaf.zScore, 0);
    return sumZ / territory.leaves.length;
  };

  const hasTemplates = territories.some(t => ["t1", "t2", "t3"].includes(t.id));

  return (
    <div className="flex-1 overflow-y-auto px-4 py-5 flex flex-col bg-slate-50 text-slate-800">
      
      {/* Hidden file input for JSON import */}
      <input
        ref={fileInputRef}
        type="file"
        accept=".json,application/json"
        onChange={handleFileImport}
        className="hidden"
      />

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100 shrink-0">
            <ClipboardList className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-xl font-bold text-slate-900 tracking-tight">Журнал наблюдений</h1>
            <p className="text-xs text-slate-500 font-mono">Архив обследований территорий</p>
          </div>
        </div>

        {/* Action controls: Export, Import, and Clear */}
        <div className="flex items-center gap-1.5 self-end sm:self-auto flex-wrap">
          <button
            onClick={handleExportJSON}
            title="Экспорт журнала в JSON"
            className="text-[11px] font-semibold text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 px-2.5 py-1.5 rounded-xl transition-all cursor-pointer active:scale-95 flex items-center gap-1 shadow-2xs"
          >
            <Download size={13} className="text-emerald-600" />
            <span>Экспорт</span>
          </button>

          <button
            onClick={() => fileInputRef.current?.click()}
            title="Импорт журнала из JSON файла"
            className="text-[11px] font-semibold text-slate-700 bg-white hover:bg-slate-100 border border-slate-200 px-2.5 py-1.5 rounded-xl transition-all cursor-pointer active:scale-95 flex items-center gap-1 shadow-2xs"
          >
            <Upload size={13} className="text-blue-600" />
            <span>Импорт</span>
          </button>

          {territories.length > 0 && onClearJournal && (
            <button
              onClick={() => {
                telegramHaptic("warning");
                setShowClearConfirmModal(true);
              }}
              title="Очистить весь журнал"
              className="text-[11px] font-semibold text-rose-600 bg-rose-50 hover:bg-rose-100 border border-rose-200 px-2.5 py-1.5 rounded-xl transition-all cursor-pointer active:scale-95 flex items-center gap-1"
            >
              <Trash2 size={13} />
              <span>Очистить</span>
            </button>
          )}

          {hasTemplates && onClearTemplates && territories.length > 0 && (
            <button
              onClick={onClearTemplates}
              className="text-[10px] text-slate-500 hover:text-slate-700 bg-slate-100 hover:bg-slate-200 px-2 py-1.5 rounded-xl font-medium transition-all cursor-pointer active:scale-95"
              title="Удалить только демо-примеры"
            >
              Сброс демо
            </button>
          )}
        </div>
      </div>

      {/* Top Segmented Tabs: 7-Day Chart vs Territory List */}
      <div className="grid grid-cols-2 gap-1.5 p-1 bg-slate-200/80 rounded-2xl mb-4 font-sans select-none shrink-0">
        <button
          onClick={() => setViewMode("chart")}
          className={`py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer ${
            viewMode === "chart"
              ? "bg-white text-emerald-700 shadow-xs"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          <Activity size={15} className={viewMode === "chart" ? "text-emerald-600" : "text-slate-500"} />
          <span>График ФА (7 дней)</span>
        </button>
        <button
          onClick={() => setViewMode("territories")}
          className={`py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer ${
            viewMode === "territories"
              ? "bg-white text-emerald-700 shadow-xs"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          <ClipboardList size={15} className={viewMode === "territories" ? "text-emerald-600" : "text-slate-500"} />
          <span>Территории ({territories.length})</span>
        </button>
      </div>

      {viewMode === "chart" ? (
        <div className="space-y-4">
          <FAWeeklyChart
            territories={territories}
            onSelectTerritoryForScan={onSelectTerritoryForScan}
            onLoadDemoData={onLoadDemoData}
          />
          {territories.length > 0 && (
            <div className="pt-1 pb-4 text-center">
              <button
                onClick={() => setViewMode("territories")}
                className="text-xs font-bold text-slate-500 hover:text-emerald-700 inline-flex items-center gap-1 transition-colors py-1.5 px-3 rounded-xl hover:bg-slate-100 cursor-pointer"
              >
                <span>Перейти к подробному списку территорий ({territories.length})</span>
                <ChevronRight size={14} />
              </button>
            </div>
          )}
        </div>
      ) : territories.length === 0 ? (
        <div className="flex-1 flex flex-col items-center justify-center p-6 text-center bg-white rounded-3xl border border-slate-200/80 shadow-sm my-4">
          <div className="w-16 h-16 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3 shadow-inner">
            <Leaf size={32} className="text-emerald-600 animate-pulse" />
          </div>
          <h3 className="font-bold text-slate-800 text-base mb-1">Журнал наблюдений пуст</h3>
          <p className="text-xs text-slate-500 max-w-[280px] leading-relaxed mb-5">
            Журнал очищен и готов к сохранению новых замеров. Выберите действие для начала работы:
          </p>

          <div className="w-full max-w-xs space-y-2.5">
            {onNavigateToScan && (
              <button
                onClick={() => {
                  telegramHaptic("medium");
                  onNavigateToScan();
                }}
                className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs py-3 px-4 rounded-xl shadow-xs transition-all active:scale-98 cursor-pointer flex items-center justify-center gap-2"
              >
                <Camera size={16} />
                <span>Начать замер листа</span>
              </button>
            )}

            {onNavigateToMap && (
              <button
                onClick={() => {
                  telegramHaptic("selection");
                  onNavigateToMap();
                }}
                className="w-full bg-slate-100 hover:bg-slate-200 text-slate-700 font-bold text-xs py-2.5 px-4 rounded-xl transition-all active:scale-98 cursor-pointer flex items-center justify-center gap-2 border border-slate-200/80"
              >
                <MapPin size={15} className="text-emerald-600" />
                <span>Отметить точку на карте</span>
              </button>
            )}

            {onLoadDemoData && (
              <button
                onClick={() => {
                  telegramHaptic("medium");
                  onLoadDemoData();
                }}
                className="w-full text-emerald-700 hover:text-emerald-800 hover:bg-emerald-50 text-[11px] font-semibold py-2 px-3 rounded-xl transition-all cursor-pointer border border-dashed border-emerald-300"
              >
                Загрузить замеры за последние 7 дней
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className="space-y-4">
          {/* Quick link banner to chart */}
          <div
            onClick={() => setViewMode("chart")}
            className="bg-emerald-50 hover:bg-emerald-100/70 border border-emerald-150 p-3 rounded-2xl flex items-center justify-between cursor-pointer transition-all active:scale-98 shadow-3xs"
          >
            <div className="flex items-center gap-2.5">
              <div className="p-2 bg-emerald-600 text-white rounded-xl shadow-2xs">
                <Activity size={16} />
              </div>
              <div>
                <p className="text-xs font-bold text-emerald-950">
                  Динамика асимметрии за последние 7 дней
                </p>
                <p className="text-[10px] text-emerald-700">
                  График Recharts, тренды и морфометрия признаков
                </p>
              </div>
            </div>
            <ChevronRight size={16} className="text-emerald-600" />
          </div>
          {territories.map((territory) => {
            const isExpanded = expandedTerritoryId === territory.id;
            const avgFA = getFAForTerritory(territory);
            const rating = getEnvironmentalRating(avgFA);
            const keyColor = territory.leaves.length === 0 ? "slate" : rating.score === 1 ? "emerald" : rating.score === 2 ? "yellow" : rating.score === 3 ? "amber" : rating.score === 4 ? "rose" : "red";
            
            const isMeaningfulDesc = !!(
              territory.description &&
              !territory.description.startsWith("Выбрано на карте") &&
              !territory.description.startsWith("Координаты") &&
              territory.description.trim() !== ""
            );

            return (
              <div
                key={territory.id}
                className={`bg-white rounded-2xl border transition-all overflow-hidden ${
                  isExpanded
                    ? "border-slate-800 shadow-md ring-1 ring-slate-800/10"
                    : "border-slate-100 hover:border-slate-200 shadow-xs"
                }`}
              >
                
                {/* Collapsed Header */}
                <div
                  onClick={() => toggleTerritory(territory.id)}
                  className="p-4 cursor-pointer flex items-start gap-3.5 select-none hover:bg-slate-50/50"
                >
                  {/* Score badge / color dot */}
                  <div className={`w-10 h-10 rounded-xl flex flex-col items-center justify-center shrink-0 border shadow-xs ${
                    territory.leaves.length === 0
                      ? "bg-slate-100 text-slate-400 border-slate-200"
                      : rating.score === 1
                      ? "bg-emerald-50 text-emerald-600 border-emerald-100"
                      : rating.score === 2
                      ? "bg-yellow-50 text-yellow-600 border-yellow-200"
                      : rating.score === 3
                      ? "bg-amber-50 text-amber-600 border-amber-200"
                      : rating.score === 4
                      ? "bg-rose-50 text-rose-600 border-rose-200"
                      : "bg-red-50 text-red-800 border-red-200"
                  }`}>
                    <span className="text-[9px] font-bold tracking-tight uppercase leading-none text-slate-400">БАЛЛ</span>
                    <span className="text-lg font-extrabold leading-none mt-0.5">
                      {territory.leaves.length > 0 ? rating.score : "—"}
                    </span>
                  </div>

                  {/* Title & Stats summary */}
                  <div className="flex-1 min-w-0">
                    <h3 className="font-bold text-slate-900 text-sm leading-tight truncate">{territory.name}</h3>
                    <div className="flex items-center gap-2 mt-1 text-[10px] text-slate-400 font-mono tracking-tight font-semibold">
                      <span className="flex items-center gap-0.5 max-w-[120px] truncate">
                        <MapPin size={10} className="text-slate-400" />
                        {territory.latitude.toFixed(3)}°, {territory.longitude.toFixed(3)}°
                      </span>
                      <span>•</span>
                      <span>Замеров: {territory.leaves.length}</span>
                      {territory.leaves.length > 0 && (
                        <>
                          <span>•</span>
                          <span className="font-bold text-slate-600">FA: {avgFA.toFixed(4)}</span>
                        </>
                      )}
                    </div>
                  </div>

                  {/* Expand status arrow */}
                  <div className="flex items-center gap-1.5 self-center shrink-0" onClick={(e) => e.stopPropagation()}>
                    <div className="text-slate-400 cursor-pointer p-0.5" onClick={() => toggleTerritory(territory.id)}>
                      {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                    </div>
                  </div>

                </div>

                {/* Expanded Territory content details */}
                {isExpanded && (
                  <div className="border-t border-slate-100 bg-slate-50/40 p-4 space-y-4 animate-slide-down">
                    
                    {/* Description card */}
                    {(isMeaningfulDesc || territory.leaves.length > 0) && (
                      <div className="bg-white p-3.5 rounded-xl border border-slate-100 text-xs">
                        {isMeaningfulDesc && (
                          <div className="mb-3">
                            <p className="font-semibold text-slate-900 mb-1">Ориентир / Описание:</p>
                            <p className="text-slate-500 leading-relaxed">{territory.description}</p>
                          </div>
                        )}
                        
                        {territory.leaves.length > 0 && (
                          <div className={`${isMeaningfulDesc ? "pt-3 border-t border-slate-100" : ""} flex items-center justify-between text-[11px]`}>
                            <span className="text-slate-400 font-medium">Заключение качества среды:</span>
                            <span className={`font-extrabold px-2 py-0.5 rounded-md ${
                              rating.score === 1
                                ? "bg-emerald-500 text-white"
                                : rating.score === 2
                                ? "bg-yellow-400 text-slate-900"
                                : rating.score === 3
                                ? "bg-amber-500 text-white"
                                : rating.score === 4
                                ? "bg-rose-500 text-white"
                                : "bg-red-800 text-white"
                            }`}>
                              {rating.status}
                            </span>
                          </div>
                        )}
                      </div>
                    )}

                    {/* Button for scanning directly to this territory or viewing 7-day chart */}
                    <div className="flex gap-2">
                      <button
                        onClick={() => onSelectTerritoryForScan(territory.id)}
                        className="flex-1 bg-emerald-600 hover:bg-emerald-700 text-white font-bold py-2.5 px-3 rounded-xl text-xs flex items-center justify-center gap-1.5 shadow-sm active:scale-95 transition-all cursor-pointer"
                      >
                        <PlusCircle size={15} />
                        Добавить замер
                      </button>
                      <button
                        onClick={() => setViewMode("chart")}
                        className="bg-slate-100 hover:bg-emerald-50 text-slate-700 hover:text-emerald-700 font-bold py-2.5 px-3 rounded-xl text-xs flex items-center justify-center gap-1.5 border border-slate-200 hover:border-emerald-200 transition-all cursor-pointer active:scale-95 shrink-0"
                        title="Посмотреть динамику ФА на графике за 7 дней"
                      >
                        <Activity size={15} className="text-emerald-600" />
                        График за 7 дней
                      </button>
                    </div>

                    {/* Leaf logs list inside territory */}
                    <div className="space-y-2">
                      <h4 className="font-bold text-slate-800 text-xs tracking-tight uppercase px-1">Зарегистрированные листья</h4>
                      
                      {territory.leaves.length === 0 ? (
                        <div className="p-4 text-center rounded-xl border border-dashed border-slate-200 text-xs text-slate-400 font-medium">
                          Листья берез еще не отсканированы для этой геопозиции.
                        </div>
                      ) : (
                        territory.leaves.map((leaf) => {
                          const isLeafExpanded = expandedLeafId === leaf.id;

                          return (
                            <div
                              key={leaf.id}
                              className="bg-white rounded-xl border border-slate-100 overflow-hidden shadow-2xs"
                            >
                              
                              {/* Leaf row collapsed header */}
                              <div
                                onClick={(e) => toggleLeaf(leaf.id, e)}
                                className="p-3 cursor-pointer flex justify-between items-center select-none hover:bg-slate-50"
                              >
                                <div className="flex items-center gap-2.5 min-w-0">
                                  {/* Rounded thumbnail */}
                                  <div className="shrink-0 w-8 h-8 rounded-lg overflow-hidden border border-slate-100 flex items-center justify-center bg-slate-50">
                                    {leaf.photoUrl === "template" ? (
                                      <span className="text-[8px] font-extrabold text-emerald-600">БС-1</span>
                                    ) : (
                                      <img
                                        src={leaf.photoUrl}
                                        alt="leaf thumbnail"
                                        className="w-full h-full object-cover"
                                        referrerPolicy="no-referrer"
                                      />
                                    )}
                                  </div>
                                  <div className="min-w-0">
                                    <div className="font-extrabold text-slate-800 text-xs font-mono truncate max-w-[170px]">
                                      {territory.latitude.toFixed(4)}°N, {territory.longitude.toFixed(4)}°E
                                    </div>
                                    <div className="text-[10px] text-slate-400 font-mono mt-0.5 font-semibold">
                                      {new Date(leaf.timestamp).toLocaleDateString("ru-RU")} в {new Date(leaf.timestamp).toLocaleTimeString("ru-RU", {hour: "2-digit", minute:"2-digit"})}
                                    </div>
                                  </div>
                                </div>

                                <div className="flex items-center gap-3 shrink-0">
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      setSharingData({ territory, leaf });
                                    }}
                                    className="p-1.5 rounded-lg bg-emerald-50 border border-emerald-100 text-emerald-600 hover:bg-emerald-100/70 hover:text-emerald-700 transition-all cursor-pointer active:scale-90"
                                    title="Поделиться"
                                  >
                                    <Share2 size={13} />
                                  </button>
                                  <div className="text-right">
                                    <div className="text-xs font-extrabold text-slate-800 font-mono">
                                      Z = {leaf.zScore.toFixed(4)}
                                    </div>
                                    <div className="text-[8px] font-medium text-slate-400">FA замера</div>
                                  </div>
                                  <div className="text-slate-400">
                                    {isLeafExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                                  </div>
                                </div>
                              </div>

                              {/* Expanded Leaf Details Matrix (Detailed morph traits Left vs Right) */}
                              {isLeafExpanded && (
                                <div className="border-t border-slate-100 p-3 bg-slate-50 text-[11px] animate-slide-down space-y-2.5">
                                  
                                  {/* Leaf Photo Preview */}
                                  <div className="bg-white p-2.5 rounded-lg border border-slate-150 flex flex-col items-center gap-1.5">
                                    <div className="text-[9px] font-bold text-slate-400 uppercase tracking-wider self-start px-1">Фотография листа:</div>
                                    <div className="relative w-full max-w-[180px] aspect-square rounded-lg overflow-hidden border border-slate-100 bg-slate-50 flex items-center justify-center">
                                      {leaf.photoUrl === "template" ? (
                                        <div className="flex flex-col items-center justify-center p-3 text-center text-slate-400 gap-1">
                                          <span className="text-xs font-bold text-emerald-600 uppercase">Симметричный шаблон</span>
                                          <span className="text-[9px]">Стандартная калибровка березы</span>
                                        </div>
                                      ) : (
                                        <img
                                          src={leaf.photoUrl}
                                          alt={leaf.photoName}
                                          className="w-full h-full object-contain"
                                          referrerPolicy="no-referrer"
                                        />
                                      )}
                                    </div>
                                    <div className="text-[9px] text-slate-400 font-mono italic max-w-full truncate">Координаты: {territory.latitude.toFixed(4)}°N, {territory.longitude.toFixed(4)}°E</div>
                                  </div>

                                  {/* Table dimensions of 5 traits calculated values */}
                                  <div className="space-y-1.5 bg-white p-2.5 rounded-lg border border-slate-150">
                                    <div className="grid grid-cols-4 font-bold text-slate-400 text-[10px] pb-1 border-b border-slate-100 font-mono">
                                      <span>ПРИЗНАК</span>
                                      <span className="text-center">ЛЕВО</span>
                                      <span className="text-center">ПРАВО</span>
                                      <span className="text-right text-slate-800">Y_i (РАЗЛ)</span>
                                    </div>

                                    {/* Trait 1 width */}
                                    <div className="grid grid-cols-4 font-medium text-slate-600">
                                      <span className="font-semibold text-slate-800">1. Ширина</span>
                                      <span className="text-center">{leaf.traits.t1Width.xLeft.toFixed(1)} мм</span>
                                      <span className="text-center">{leaf.traits.t1Width.xRight.toFixed(1)} мм</span>
                                      <span className="text-right font-bold text-slate-800 font-mono">{leaf.traits.t1Width.yDiff.toFixed(4)}</span>
                                    </div>

                                    {/* Trait 2 vein len */}
                                    <div className="grid grid-cols-4 font-medium text-slate-600">
                                      <span className="font-semibold text-slate-800">2. Дл. жилки</span>
                                      <span className="text-center">{leaf.traits.t2VeinLen.xLeft.toFixed(1)} мм</span>
                                      <span className="text-center">{leaf.traits.t2VeinLen.xRight.toFixed(1)} мм</span>
                                      <span className="text-right font-bold text-slate-800 font-mono">{leaf.traits.t2VeinLen.yDiff.toFixed(4)}</span>
                                    </div>

                                    {/* Trait 3 bases dist */}
                                    <div className="grid grid-cols-4 font-medium text-slate-600">
                                      <span className="font-semibold text-slate-800">3. Расст. осн</span>
                                      <span className="text-center">{leaf.traits.t3VeinDist.xLeft.toFixed(1)} мм</span>
                                      <span className="text-center">{leaf.traits.t3VeinDist.xRight.toFixed(1)} мм</span>
                                      <span className="text-right font-bold text-slate-800 font-mono">{leaf.traits.t3VeinDist.yDiff.toFixed(4)}</span>
                                    </div>

                                    {/* Trait 4 ends dist */}
                                    <div className="grid grid-cols-4 font-medium text-slate-600">
                                      <span className="font-semibold text-slate-800">4. Расст. кон</span>
                                      <span className="text-center">{leaf.traits.t4EndDist.xLeft.toFixed(1)} мм</span>
                                      <span className="text-center">{leaf.traits.t4EndDist.xRight.toFixed(1)} мм</span>
                                      <span className="text-right font-bold text-slate-800 font-mono">{leaf.traits.t4EndDist.yDiff.toFixed(4)}</span>
                                    </div>

                                    {/* Trait 5 angle */}
                                    <div className="grid grid-cols-4 font-medium text-slate-600">
                                      <span className="font-semibold text-slate-800">5. Угол бок.</span>
                                      <span className="text-center">{leaf.traits.t5Angle.xLeft.toFixed(0)}°</span>
                                      <span className="text-center">{leaf.traits.t5Angle.xRight.toFixed(0)}°</span>
                                      <span className="text-right font-bold text-slate-800 font-mono">{leaf.traits.t5Angle.yDiff.toFixed(4)}</span>
                                    </div>
                                  </div>

                                  <div className="flex justify-between items-center text-[10px] px-1 text-slate-400 font-medium">
                                    <span>Векторные измерения пересчитаны в мм/градусы</span>
                                    <div className="flex items-center gap-2.5">
                                      <button
                                        onClick={(e) => {
                                          e.stopPropagation();
                                          setSharingData({ territory, leaf });
                                        }}
                                        className="flex items-center gap-1 text-emerald-600 hover:text-emerald-700 font-bold transition-all cursor-pointer bg-emerald-50 hover:bg-emerald-100/75 px-2 py-1 rounded-md border border-emerald-150/60 active:scale-95 text-[10px]"
                                      >
                                        <Share2 size={11} />
                                        Поделиться
                                      </button>
                                      <button
                                        onClick={() => onDeleteLeaf(territory.id, leaf.id)}
                                        className="flex items-center gap-0.5 text-rose-500 hover:text-rose-700 font-bold transition-all cursor-pointer"
                                        title="Удалить замер"
                                      >
                                        <Trash2 size={12} />
                                        Удалить замер
                                      </button>
                                    </div>
                                  </div>

                                </div>
                              )}

                            </div>
                          );
                        })
                      )}
                    </div>

                  </div>
                )}

              </div>
            );
          })}
        </div>
      )}

      {/* Share Modal Dialog */}
      {sharingData && (() => {
        const leaf = sharingData.leaf;
        const territory = sharingData.territory;
        const rating = getEnvironmentalRating(leaf.zScore);
        const shareUrl = `${window.location.origin}/#leaf-${leaf.id}`;
        
        return (
          <div className="fixed inset-0 bg-slate-900/70 backdrop-blur-xs flex items-center justify-center z-50 p-4 animate-fade-in overflow-y-auto">
            <div className="bg-slate-50 rounded-3xl w-full max-w-sm md:max-w-md overflow-hidden shadow-2xl flex flex-col relative border border-slate-200/80 animate-scale-up my-4">
              
              {/* Close Button */}
              <button
                onClick={() => {
                  setSharingData(null);
                  setCopied(false);
                }}
                className="absolute top-4 right-4 text-slate-400 hover:text-slate-600 bg-white hover:bg-slate-100 p-1.5 rounded-full z-20 shadow-xs border border-slate-100 transition-all active:scale-90 cursor-pointer"
                title="Закрыть"
              >
                <X size={16} />
              </button>

              {/* Modal Body / Product Card Container */}
              <div className="p-5 flex-1 flex flex-col space-y-4">
                
                {/* Title Badge */}
                <div className="flex items-center gap-1.5 bg-emerald-50 border border-emerald-100 text-emerald-700 px-3 py-1.5 rounded-full w-fit self-center">
                  <Leaf size={14} className="fill-emerald-500 animate-pulse" />
                  <span className="text-[10px] font-extrabold tracking-wider uppercase">Экологический паспорт листа</span>
                </div>

                {/* THE PRODUCT CARD ("Карточка товара") */}
                <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-sm flex flex-col space-y-3 relative overflow-hidden">
                  
                  {/* Decorative background circle */}
                  <div className="absolute -right-16 -top-16 w-36 h-36 rounded-full bg-slate-50 border border-slate-100 -z-0 opacity-50"></div>

                  {/* Product Card Header */}
                  <div className="relative z-10 flex justify-between items-start border-b border-slate-100 pb-2.5">
                    <div>
                      <h4 className="font-extrabold text-slate-900 text-[13px] tracking-tight truncate max-w-[180px]">
                        Лист берёзы повислой
                      </h4>
                      <p className="text-[9px] text-slate-400 font-mono mt-0.5 uppercase tracking-wide">
                        Серия VLA-RU-2026 • №{leaf.id.split('_')[1] || "4201"}
                      </p>
                    </div>
                    
                    {/* Air quality badge */}
                    <div className="text-right">
                      <span className={`text-[9px] font-extrabold px-2 py-1 rounded-md shadow-2xs border ${
                        rating.score === 1
                          ? "bg-emerald-50 text-emerald-700 border-emerald-200"
                          : rating.score === 2
                          ? "bg-yellow-50 text-yellow-700 border-yellow-200"
                          : rating.score === 3
                          ? "bg-amber-50 text-amber-700 border-amber-200"
                          : rating.score === 4
                          ? "bg-rose-50 text-rose-700 border-rose-200"
                          : "bg-red-50 text-red-800 border-red-200"
                      }`}>
                        Класс {rating.score}
                      </span>
                    </div>
                  </div>

                  {/* Two Main Images Block: [Map with Quality Pin] & [Leaf Scan Photo] */}
                  <div className="grid grid-cols-2 gap-3 relative z-10">
                    
                    {/* Left: Карта с отметкой качества */}
                    <div className="flex flex-col space-y-1">
                      <span className="text-[8px] font-extrabold text-slate-400 uppercase tracking-wider px-0.5">1. Карта замера:</span>
                      <div className="relative w-full aspect-square bg-slate-100 rounded-xl overflow-hidden border border-slate-200 group flex items-center justify-center">
                        {/* Real dynamic static map from Yandex.Maps API */}
                        <img
                          src={`https://static-maps.yandex.ru/1.x/?ll=${territory.longitude},${territory.latitude}&z=14&l=map&size=250,250&pt=${territory.longitude},${territory.latitude},pm2${
                            rating.score === 1
                              ? "gn"
                              : rating.score === 2
                              ? "yl"
                              : rating.score === 3
                              ? "og"
                              : "rd"
                          }m`}
                          alt="Карта замера"
                          className="absolute inset-0 w-full h-full object-cover z-0"
                          referrerPolicy="no-referrer"
                          onError={(e) => {
                            e.currentTarget.style.display = "none";
                          }}
                        />

                        {/* Tech design grid overlay */}
                        <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.08)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.08)_1px,transparent_1px)] bg-[size:15px_15px] z-10 pointer-events-none opacity-30"></div>
                        <div className="absolute inset-0 bg-gradient-to-t from-slate-950/30 via-transparent to-transparent z-10 pointer-events-none"></div>

                        {/* GPS Coordinates overlay watermark */}
                        <div className="absolute bottom-1 left-1.5 right-1.5 flex justify-between items-center text-[7px] text-white font-mono bg-slate-900/80 px-1 py-0.5 rounded-md z-20">
                          <span>{territory.latitude.toFixed(4)}°N</span>
                          <span>{territory.longitude.toFixed(4)}°E</span>
                        </div>

                        {/* Compass indicator */}
                        <div className="absolute top-1.5 right-1.5 w-4 h-4 rounded-full border border-slate-700 bg-slate-900/80 flex items-center justify-center text-[7px] text-slate-300 font-bold z-20 shadow-xs">
                          N
                        </div>
                      </div>
                    </div>

                    {/* Right: Фото скана листа */}
                    <div className="flex flex-col space-y-1">
                      <span className="text-[8px] font-extrabold text-slate-400 uppercase tracking-wider px-0.5">2. Скан листа:</span>
                      <div className="relative w-full aspect-square bg-emerald-950/5 rounded-xl overflow-hidden border border-emerald-100 flex items-center justify-center">
                        
                        {/* Measuring crosshairs graphic */}
                        <div className="absolute top-0 bottom-0 left-1/2 w-[0.5px] bg-emerald-600/10"></div>
                        <div className="absolute left-0 right-0 top-1/2 h-[0.5px] bg-emerald-600/10"></div>
                        <div className="absolute w-8 h-8 rounded-full border border-emerald-600/10"></div>

                        {/* Leaf image representation */}
                        {leaf.photoUrl === "template" ? (
                          <div className="flex flex-col items-center justify-center p-2 text-center text-slate-400 gap-0.5">
                            <span className="text-[9px] font-extrabold text-emerald-600 uppercase">БС-1</span>
                            <span className="text-[6px] leading-tight">Симметричная калибровка</span>
                          </div>
                        ) : (
                          <img
                            src={leaf.photoUrl}
                            alt="leaf scan"
                            className="w-full h-full object-contain relative z-10"
                            referrerPolicy="no-referrer"
                          />
                        )}

                        {/* Digital calibration tag overlay */}
                        <div className="absolute top-1 left-1 bg-emerald-600/80 text-[5px] font-extrabold text-white px-0.5 py-0.2 rounded-xs tracking-wider leading-none uppercase">
                          CALIB
                        </div>
                        <div className="absolute bottom-1 right-1 text-[5px] font-mono font-bold text-emerald-700/60 leading-none">
                          BIOMETRY v3
                        </div>
                      </div>
                    </div>

                  </div>

                  {/* Specifications and stats (Product metrics) */}
                  <div className="relative z-10 bg-slate-50 rounded-xl p-2.5 border border-slate-100 text-[10px] space-y-2">
                    
                    {/* Location and Info */}
                    <div className="grid grid-cols-2 gap-2 text-slate-500 font-medium">
                      <div>
                        <span className="text-[8px] text-slate-400 block font-bold uppercase">Геопозиция:</span>
                        <span className="text-slate-800 font-bold truncate block">{territory.name}</span>
                      </div>
                      <div>
                        <span className="text-[8px] text-slate-400 block font-bold uppercase">Дата замера:</span>
                        <span className="text-slate-800 font-bold block">
                          {new Date(leaf.timestamp).toLocaleDateString("ru-RU")}
                        </span>
                      </div>
                    </div>

                    {/* Scientific Z-Score indicator */}
                    <div className="pt-2 border-t border-slate-200/60 flex items-center justify-between">
                      <div>
                        <span className="text-[8px] text-slate-400 block font-bold uppercase">Показатель асимметрии (FA):</span>
                        <span className="text-xs font-mono font-extrabold text-slate-900 leading-none">
                          {leaf.zScore.toFixed(4)}
                        </span>
                      </div>
                      
                      {/* Quality description stamp */}
                      <div className="text-right">
                        <span className="text-[8px] text-slate-400 block font-bold uppercase">Качество среды:</span>
                        <span className="text-slate-700 font-extrabold block text-[9px] tracking-tight text-right">
                          {rating.status}
                        </span>
                      </div>
                    </div>
                  </div>

                  {/* Authentic Barcode widget bottom */}
                  <div className="flex flex-col items-center pt-2 border-t border-dashed border-slate-200">
                    <div className="flex items-center gap-[1px] h-6 justify-center text-slate-800 opacity-80 select-none">
                      <span className="w-[1.5px] h-6 bg-slate-800"></span>
                      <span className="w-[3px] h-6 bg-slate-800 ml-[1px]"></span>
                      <span className="w-[1px] h-6 bg-slate-800 ml-[1px]"></span>
                      <span className="w-[2px] h-6 bg-slate-800 ml-[2px]"></span>
                      <span className="w-[4px] h-6 bg-slate-800 ml-[1px]"></span>
                      <span className="w-[1px] h-6 bg-slate-800"></span>
                      <span className="w-[1px] h-6 bg-slate-800 ml-[1px]"></span>
                      <span className="w-[3px] h-6 bg-slate-800 ml-[2px]"></span>
                      <span className="w-[1.5px] h-6 bg-slate-800 ml-[1px]"></span>
                      <span className="w-[2px] h-6 bg-slate-800 ml-[2px]"></span>
                      <span className="w-[1px] h-6 bg-slate-800 ml-[1px]"></span>
                      <span className="w-[4px] h-6 bg-slate-800"></span>
                    </div>
                    <span className="text-[7px] font-mono font-bold tracking-widest text-slate-400 mt-1 uppercase">
                      * vla-eco-{leaf.id.split('_')[1] || "3910"} *
                    </span>
                  </div>

                </div>

                {/* Copied alert toast inside the card modal */}
                {copied && (
                  <div className="bg-emerald-50 border border-emerald-150 rounded-xl p-2.5 text-xs text-emerald-800 font-semibold text-center animate-fade-in flex items-center justify-center gap-1.5">
                    <Check size={14} className="text-emerald-600" />
                    Ссылка скопирована в буфер обмена!
                  </div>
                )}

                {/* Action buttons (Copy link / share link) */}
                <div className="flex flex-col gap-2 pt-1">
                  
                  {/* Shareable Link Input bar */}
                  <div className="flex items-center gap-2 bg-slate-100 border border-slate-200 rounded-xl p-1.5">
                    <input
                      type="text"
                      readOnly
                      value={shareUrl}
                      className="flex-1 bg-transparent border-none text-[10px] text-slate-500 font-mono focus:outline-none px-2 select-all"
                    />
                    <button
                      onClick={() => handleCopyLink(leaf.id)}
                      className="p-2 rounded-lg bg-white border border-slate-200 text-slate-700 hover:text-slate-900 shadow-3xs cursor-pointer active:scale-95 transition-all"
                      title="Копировать"
                    >
                      <Copy size={12} />
                    </button>
                  </div>

                  {/* Feedback toast for Bot send */}
                  {botSentSuccess && (
                    <div className="bg-teal-50 border border-teal-200 rounded-xl p-2.5 text-xs text-teal-800 font-semibold text-center animate-fade-in flex items-center justify-center gap-1.5">
                      <Check size={14} className="text-teal-600 shrink-0" />
                      {botSentSuccess}
                    </div>
                  )}

                  {/* Send to @bpaqi_bot */}
                  <button
                    onClick={() => {
                      const result = sendDataToTelegramBot({
                        type: "leaf_passport",
                        bot: BOT_USERNAME,
                        territoryName: territory.name,
                        leafName: leaf.photoName || "Лист берёзы",
                        zScore: leaf.zScore,
                        qualityScore: rating.score,
                        qualityStatus: rating.status,
                        timestamp: leaf.timestamp,
                        leafId: leaf.id,
                        measurements: leaf.measurements,
                      });
                      setBotSentSuccess(result.message);
                      setTimeout(() => setBotSentSuccess(null), 3500);
                    }}
                    className="w-full py-2.5 rounded-xl text-xs font-bold text-white bg-teal-700 hover:bg-teal-800 active:scale-98 transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow-md"
                  >
                    <Send size={13} />
                    Отправить замер в @{BOT_USERNAME}
                  </button>

                  {/* Primary action share / copy button */}
                  <div className="grid grid-cols-2 gap-2">
                    <button
                      onClick={() => handleNativeShare(leaf.id, leaf.photoName, territory.name, leaf.zScore)}
                      className="py-2.5 px-2 rounded-xl text-xs font-bold text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 active:scale-98 transition-all cursor-pointer flex items-center justify-center gap-1.5"
                    >
                      <Share2 size={13} className="text-emerald-700" />
                      <span>Поделиться</span>
                    </button>

                    <button
                      onClick={() => {
                        const shareText = `🍃 Эко-замер флуктуирующей асимметрии листа берёзы\nТерритория: ${territory.name}\nИнтегральный показатель Z: ${leaf.zScore.toFixed(4)}\nБалл качества: ${rating.score} (${rating.status})\nБот: @${BOT_USERNAME}`;
                        shareReportToTelegram(shareText, shareUrl);
                      }}
                      className="py-2.5 px-2 rounded-xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 active:scale-98 transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow-sm"
                    >
                      <Bot size={13} />
                      <span>В Telegram</span>
                    </button>
                  </div>

                  <button
                    onClick={() => {
                      setSharingData(null);
                      setCopied(false);
                    }}
                    className="w-full py-2 text-slate-400 hover:text-slate-600 text-[10px] font-bold text-center hover:bg-slate-100/50 rounded-lg cursor-pointer transition-all"
                  >
                    Закрыть окно
                  </button>
                </div>

              </div>
            </div>
          </div>
        );
      })()}

      {/* Clear Entire Journal Confirmation Modal */}
      {showClearConfirmModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs animate-fade-in">
          <div className="bg-white rounded-3xl p-5 w-full max-w-sm shadow-2xl border border-slate-150 animate-scale-up space-y-4">
            <div className="flex items-center gap-3">
              <div className="p-3 rounded-2xl bg-rose-50 text-rose-600 border border-rose-100">
                <AlertCircle className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-bold text-slate-900 text-base">Очистить весь журнал?</h3>
                <p className="text-xs text-slate-500">Действие нельзя отменить</p>
              </div>
            </div>

            <p className="text-xs text-slate-600 leading-relaxed">
              Все сохраненные территории и замеры листьев ({territories.length} шт.) будут безвозвратно удалены из памяти устройства.
            </p>

            <div className="bg-slate-50 rounded-xl p-3 border border-slate-200 text-[11px] text-slate-600 flex items-center justify-between">
              <span>Сохранить копию перед удалением?</span>
              <button
                onClick={handleExportJSON}
                className="text-emerald-700 font-bold hover:underline inline-flex items-center gap-1 cursor-pointer"
              >
                <Download size={12} />
                Экспорт JSON
              </button>
            </div>

            <div className="flex items-center gap-2 pt-2">
              <button
                onClick={() => setShowClearConfirmModal(false)}
                className="flex-1 py-2.5 rounded-xl border border-slate-200 text-slate-700 font-bold text-xs hover:bg-slate-50 active:scale-95 transition-all cursor-pointer text-center"
              >
                Отмена
              </button>
              <button
                onClick={() => {
                  telegramHaptic("warning");
                  onClearJournal?.();
                  setShowClearConfirmModal(false);
                }}
                className="flex-1 py-2.5 rounded-xl bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs shadow-xs active:scale-95 transition-all cursor-pointer text-center"
              >
                Очистить
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
