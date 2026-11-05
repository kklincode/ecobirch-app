import React, { useState, useRef, useEffect } from "react";
import { LeafPoints, LeafAnalysis, Territory, TraitValues } from "../types";
import { 
  getDefaultLeafPoints, 
  calculateLeafTraits, 
  calculateZScore, 
  getEnvironmentalRating 
} from "../utils";
import { 
  healthyLeafPoints, 
  damagedLeafPoints, 
  mediumLeafPoints 
} from "../mockData";
import { 
  Camera, Upload, Eye, RefreshCw, Save, CheckCircle, AlertTriangle, 
  MapPin, HelpCircle, ArrowRight, ShieldAlert, FileImage,
  ZoomIn, ZoomOut, RotateCw, RotateCcw, SlidersHorizontal,
  Bot, Send, Share2, Check
} from "lucide-react";
import { 
  BOT_USERNAME, 
  sendDataToTelegramBot, 
  shareReportToTelegram, 
  telegramHaptic 
} from "../telegram";
import { compressImageDataUrl } from "../storage";

interface ScanScreenProps {
  territories: Territory[];
  onAddNewMeasurement: (territoryId: string, measurement: LeafAnalysis, customCoords?: { latitude: number; longitude: number }) => void;
  selectedTerritoryId: string | null;
  onNavigateToHistory: () => void;
  onNavigateToMap?: () => void;
  onSetPendingMeasurement?: (measurement: LeafAnalysis | null) => void;
}

export default function ScanScreen({
  territories,
  onAddNewMeasurement,
  selectedTerritoryId,
  onNavigateToHistory,
  onNavigateToMap,
  onSetPendingMeasurement
}: ScanScreenProps) {
  
  // SCAN STAGES: "select" -> "processing" -> "adjust" -> "saved"
  const [stage, setStage] = useState<"select" | "processing" | "adjust" | "saved">("select");
  const [photoUrl, setPhotoUrl] = useState<string>("template");
  const [photoName, setPhotoName] = useState<string>("Симметричный шаблон");
  
  // Draggable leaf coordinates
  const [leafPoints, setLeafPoints] = useState<LeafPoints>(getDefaultLeafPoints());
  
  const [isAiFallbackMode, setIsAiFallbackMode] = useState(false);
  const [aiMessage, setAiMessage] = useState("");
  const [selectedDestId, setSelectedDestId] = useState<string>(selectedTerritoryId || (territories[0]?.id || ""));
  const [customTerritoryName, setCustomTerritoryName] = useState("");
  const [showHelpModal, setShowHelpModal] = useState(false);
  const [showManualMapPrompt, setShowManualMapPrompt] = useState(false);
  const [tempPendingAnalysis, setTempPendingAnalysis] = useState<LeafAnalysis | null>(null);
  
  // Photo alignment transformation states (Zoom & Rotation & Offset)
  const [imgZoom, setImgZoom] = useState<number>(1.0);
  const [imgRotate, setImgRotate] = useState<number>(0);
  const [imgOffsetX, setImgOffsetX] = useState<number>(0);
  const [imgOffsetY, setImgOffsetY] = useState<number>(0);
  const [isAlignPanelOpen, setIsAlignPanelOpen] = useState<boolean>(true);
  const [isRealFileProcessing, setIsRealFileProcessing] = useState<boolean>(false);
  const [isApiLoading, setIsApiLoading] = useState<boolean>(false);
  const [botSentMsg, setBotSentMsg] = useState<string | null>(null);

  const resetImageTransforms = () => {
    setImgZoom(1.0);
    setImgRotate(0);
    setImgOffsetX(0);
    setImgOffsetY(0);
  };
  
  // Neural network simulation texts
  const [scanStatusIndex, setScanStatusIndex] = useState(0);
  const scanStatuses = [
    "Сегментация и бинаризация контура листа",
    "Определение основания и острой вершины (Apex)",
    "Трассировка центральной жилки симметрии",
    "Локализация первой пары боковых жилок",
    "Локализация второй пары боковых жилок",
    "Вычисление коэффициентов асимметрии Y1-Y5",
    "Синхронизация калибровочной сетки..."
  ];

  const fileInputRef = useRef<HTMLInputElement>(null);
  
  // Interactive Canvas variables
  const containerRef = useRef<HTMLDivElement>(null);
  const [draggedPointKey, setDraggedPointKey] = useState<keyof LeafPoints | null>(null);

  // Sync selected destination log if it updates externally
  useEffect(() => {
    if (selectedTerritoryId) {
      setSelectedDestId(selectedTerritoryId);
    } else if (territories.length > 0) {
      if (!territories.some(t => t.id === selectedDestId)) {
        setSelectedDestId(territories[0].id);
      }
    } else {
      setSelectedDestId("");
    }
  }, [selectedTerritoryId, territories]);

  // AI loading sequence timer with progressive step holding
  useEffect(() => {
    if (stage === "processing") {
      setScanStatusIndex(0);
      
      // Progressively advance through the 7 steps (takes ~2.5 seconds total)
      const tickSpeed = 350;
      
      const interval = setInterval(() => {
        setScanStatusIndex(prev => {
          const isAtLastStep = prev >= scanStatuses.length - 1;
          
          if (isAtLastStep) {
            // If the backend API call is still running, hold at the last step
            if (isRealFileProcessing && isApiLoading) {
              return prev;
            }
            // Transition only when we reach the end and API load has resolved
            clearInterval(interval);
            return prev;
          }
          
          return prev + 1;
        });
      }, tickSpeed);

      return () => clearInterval(interval);
    }
  }, [stage, isRealFileProcessing, isApiLoading]);

  // Watch for progression to complete transitioning to adjustment mode
  useEffect(() => {
    if (stage === "processing" && !isApiLoading && scanStatusIndex === scanStatuses.length - 1) {
      const timer = setTimeout(() => {
        setStage("adjust");
      }, 350);
      return () => clearTimeout(timer);
    }
  }, [stage, isApiLoading, scanStatusIndex]);

  // Handle template selection
  const handleSelectTemplate = (type: "healthy" | "medium" | "damaged") => {
    resetImageTransforms();
    setIsRealFileProcessing(false);
    if (type === "healthy") {
      setLeafPoints(healthyLeafPoints);
      setPhotoUrl("template");
      setPhotoName("Шаблон: Идеально симметричный (БС-1)");
    } else if (type === "medium") {
      setLeafPoints(mediumLeafPoints);
      setPhotoUrl("template");
      setPhotoName("Шаблон: Слабая асимметрия (ПК-1)");
    } else {
      setLeafPoints(damagedLeafPoints);
      setPhotoUrl("template");
      setPhotoName("Шаблон: Выраженная асимметрия (ИЗ-1)");
    }
    setStage("processing");
  };

  // Convert uploaded image file of custom leaf
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    resetImageTransforms();
    setIsRealFileProcessing(true);
    setIsApiLoading(true);
    setPhotoName(file.name);
    
    const reader = new FileReader();
    reader.onload = async () => {
      const base64 = reader.result as string;
      setPhotoUrl(base64);
      setStage("processing");

      // Attempt to invoke the raw Gemini 3.5 AI segmentation endpoint
      try {
        const response = await fetch("/api/analyze-leaf", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ imageBase64: base64, mimeType: file.type })
        });
        
        const data = await response.json();
        if (data && data.success && data.points) {
          setLeafPoints(data.points);
          setIsAiFallbackMode(!!data.fallbackMode);
          setAiMessage(data.message || "");
        } else {
          throw new Error("Invalid output layout");
        }
      } catch (err: any) {
        console.warn("AI segment error, running local fallback logic:", err);
        setLeafPoints(getDefaultLeafPoints());
        setIsAiFallbackMode(true);
        setAiMessage("Локальная калибровка: точки установлены по умолчанию.");
      } finally {
        setIsApiLoading(false);
      }
    };
    reader.readAsDataURL(file);
  };

  // Trigger web cam camera snapshot mock / file selector
  const handleCapturePhoto = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  // Draggable handlers
  const handleMouseDown = (key: keyof LeafPoints, e: React.MouseEvent) => {
    e.preventDefault();
    setDraggedPointKey(key);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!draggedPointKey || !containerRef.current) return;
    
    const rect = containerRef.current.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;

    // Constrain points inside the canvas bounds
    const clampedX = Math.max(2, Math.min(98, x));
    const clampedY = Math.max(2, Math.min(98, y));

    setLeafPoints(prev => ({
      ...prev,
      [draggedPointKey]: { x: clampedX, y: clampedY }
    }));
  };

  const handlePointerMove = (e: React.PointerEvent) => {
    if (!draggedPointKey || !containerRef.current) return;
    
    const rect = containerRef.current.getBoundingClientRect();
    const x = ((e.clientX - rect.left) / rect.width) * 100;
    const y = ((e.clientY - rect.top) / rect.height) * 100;

    const clampedX = Math.max(2, Math.min(98, x));
    const clampedY = Math.max(2, Math.min(98, y));

    setLeafPoints(prev => ({
      ...prev,
      [draggedPointKey]: { x: clampedX, y: clampedY }
    }));
  };

  const stopDragging = () => {
    setDraggedPointKey(null);
  };

  // Compute live measurements
  const traits = calculateLeafTraits(leafPoints);
  const zScore = calculateZScore(traits);
  const rating = getEnvironmentalRating(zScore);

  const [isSaving, setIsSaving] = useState(false);
  const [saveStatusText, setSaveStatusText] = useState("");

  // Save measurement immediately to a newly created territory on the map
  const handleSaveResult = async () => {
    setIsSaving(true);
    setSaveStatusText("Подготовка фото...");

    let finalPhotoUrl = photoUrl;
    if (photoUrl && photoUrl !== "template" && photoUrl.length > 50000) {
      try {
        finalPhotoUrl = await compressImageDataUrl(photoUrl, 600, 0.65);
      } catch (err) {
        console.warn("Failed to compress leaf photo, keeping original:", err);
      }
    }

    setSaveStatusText("Определение GPS...");

    const completedAnalysis: LeafAnalysis = {
      id: "leaf_" + Date.now(),
      photoUrl: finalPhotoUrl,
      photoName: photoName,
      points: leafPoints,
      traits: traits,
      zScore: zScore,
      timestamp: new Date().toISOString()
    };

    const destId = "temp_" + Date.now();

    const proceedSaveWithCoords = (lat?: number, lng?: number) => {
      setSaveStatusText("Сохранение в журнал...");
      const customCoords = lat !== undefined && lng !== undefined ? { latitude: lat, longitude: lng } : undefined;
      onAddNewMeasurement(destId, completedAnalysis, customCoords);
      setTimeout(() => {
        setIsSaving(false);
        setStage("saved");
      }, 500);
    };

    if (navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          proceedSaveWithCoords(position.coords.latitude, position.coords.longitude);
        },
        (error) => {
          console.warn("Geolocation failed or denied, prompting for manual map select:", error);
          setIsSaving(false);
          setTempPendingAnalysis(completedAnalysis);
          setShowManualMapPrompt(true);
        },
        { enableHighAccuracy: true, timeout: 3000, maximumAge: 0 }
      );
    } else {
      setIsSaving(false);
      setTempPendingAnalysis(completedAnalysis);
      setShowManualMapPrompt(true);
    }
  };

  return (
    <div className="flex-1 overflow-y-auto flex flex-col bg-slate-50 text-slate-800">
      
      {/* ------------------------------------ */}
      {/* SELECT PHOTO STAGE */}
      {/* ------------------------------------ */}
      {stage === "select" && (
        <div className="flex-1 p-5 flex flex-col gap-5 justify-between">
          
          <div className="space-y-4">
            <div>
              <h1 className="text-xl font-extrabold text-slate-900 tracking-tight flex items-center gap-1.5">
                <Camera className="text-emerald-500 animate-pulse" />
                Сканер листьев березы
              </h1>
              <p className="text-xs text-slate-500">Загрузите снимок для проведения асимметричной биоиндикации</p>
            </div>

            {/* Hidden Input File Trigger */}
            <input 
              type="file" 
              ref={fileInputRef} 
              aria-label="Загрузить фотографию листа"
              onChange={handleFileUpload}
              className="hidden" 
              accept="image/*"
            />

            {/* Camera Capture Box / Immersive Alignment Viewfinder */}
            <div 
              onClick={handleCapturePhoto}
              className="w-full aspect-[4/3] rounded-2xl bg-slate-950 border-2 border-dashed border-emerald-500/40 hover:border-emerald-400 p-4 text-center cursor-pointer active:scale-[0.99] transition-all hover:bg-emerald-950/15 group relative overflow-hidden flex flex-col justify-between shadow-xs"
            >
              {/* Camera grid view lines */}
              <div className="absolute inset-0 grid grid-cols-3 grid-rows-3 pointer-events-none opacity-20">
                <div className="border-r border-b border-slate-700" />
                <div className="border-r border-b border-slate-700" />
                <div className="border-b border-slate-700" />
                <div className="border-r border-b border-slate-700" />
                <div className="border-r border-b border-slate-700" />
                <div className="border-b border-slate-700" />
                <div className="border-r border-slate-700" />
                <div className="border-r border-slate-700" />
                <div className="bg-transparent" />
              </div>

              {/* Focus viewfinder indicator corners */}
              <div className="absolute top-3 left-3 w-4 h-4 border-t-2 border-l-2 border-emerald-500 rounded-tl" />
              <div className="absolute top-3 right-3 w-4 h-4 border-t-2 border-r-2 border-emerald-500 rounded-tr" />
              <div className="absolute bottom-3 left-3 w-4 h-4 border-b-2 border-l-2 border-emerald-500 rounded-bl" />
              <div className="absolute bottom-3 right-3 w-4 h-4 border-b-2 border-r-2 border-emerald-500 rounded-br" />

              {/* Dotted target-alignment leaf silhouette */}
              <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
                <div className="relative w-36 h-44 flex items-center justify-center">
                  <svg viewBox="0 0 100 120" className="w-full h-full text-emerald-500/25 fill-emerald-500/5 stroke-current animate-pulse">
                    <path
                      d="M50,110 C35,110 10,90 5,65 C0,40 25,25 50,10 C75,25 100,40 95,65 C90,90 65,110 50,110 Z"
                      strokeWidth="2"
                      strokeDasharray="3 3"
                    />
                    <line x1="50" y1="110" x2="50" y2="10" strokeWidth="1.5" strokeDasharray="3 3" />
                  </svg>
                  
                  {/* Position Labels annotations - placed further apart */}
                  <span className="absolute -top-7 left-1/2 -translate-x-1/2 text-[8px] font-black tracking-widest text-emerald-400 font-sans uppercase bg-slate-900/90 border border-emerald-500/20 px-1.5 py-0.5 rounded shadow-sm">
                    ↑ Вершина листа
                  </span>
                  <span className="absolute -bottom-7 left-1/2 -translate-x-1/2 text-[8px] font-black tracking-widest text-[#fbbf24] font-sans uppercase bg-slate-900/90 border border-[#fbbf24]/20 px-1.5 py-0.5 rounded shadow-sm">
                    ↓ Черешок листа
                  </span>
                  
                  <span className="absolute -left-12 top-1/2 -translate-y-1/2 text-[8px] font-black tracking-widest text-emerald-400 font-sans uppercase bg-slate-900/90 border border-emerald-500/20 px-1.5 py-0.5 rounded shadow-sm">
                    ← Левая
                  </span>
                  <span className="absolute -right-14 top-1/2 -translate-y-1/2 text-[8px] font-black tracking-widest text-emerald-400 font-sans uppercase bg-slate-900/90 border border-emerald-500/20 px-1.5 py-0.5 rounded shadow-sm">
                    Правая →
                  </span>
                </div>
              </div>

              {/* HUD interface elements */}
              <div className="w-full text-right text-[8px] font-mono font-bold text-emerald-400 tracking-wider flex justify-between items-center z-10 select-none">
                <span className="flex items-center gap-1">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-ping" />
                  ГОТОВ К КАЛИБРОВКЕ
                </span>
                <span>МАСШТАБ 1.0X</span>
              </div>

              <div className="flex flex-col items-center justify-center text-center py-2 z-10 select-none">
                <div className="w-10 h-10 rounded-full bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-1 group-hover:scale-105 transition-transform">
                  <Camera size={18} />
                </div>
              </div>

              <div className="w-full text-left text-[8px] font-mono text-slate-500 tracking-tight flex justify-between items-center z-10 select-none">
                <span>ОРИЕНТАЦИЯ: ВЕРТИКАЛЬНАЯ</span>
                <span>БЕРЁЗА ПОВИСЛАЯ</span>
              </div>
            </div>

            {/* Checklist for perfect photos */}
            <div className="bg-emerald-500/10 border border-emerald-500/20 rounded-xl p-3.5 text-xs text-slate-700 animate-slide-down">
              <span className="font-bold text-emerald-800 flex items-center gap-1.5 mb-2">
                <CheckCircle size={14} className="text-emerald-600 shrink-0" />
                Правила съёмки для точности ИИ:
              </span>
              <ul className="space-y-1.5 font-medium text-[11px] text-slate-600">
                <li className="flex items-start gap-2">
                  <span className="text-emerald-600 font-bold shrink-0">•</span>
                  <span><strong>Белый фон</strong>: разложите зелёный лист на чистом листе бумаги А4.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-emerald-600 font-bold shrink-0">•</span>
                  <span><strong>Вертикальная ориентация</strong>: вершина листа должна смотреть строго вверх, черешок (ножка) — вниз.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-emerald-600 font-bold shrink-0">•</span>
                  <span><strong>Никаких преград</strong>: убирайте пальцы рук и посторонние предметы из кадра.</span>
                </li>
                <li className="flex items-start gap-2">
                  <span className="text-emerald-600 font-bold shrink-0">•</span>
                  <span><strong>Равномерный свет</strong>: избегайте резких косых теней и пересветов на листе.</span>
                </li>
              </ul>
            </div>

            <div className="relative flex py-1.5 items-center">
              <div className="flex-grow border-t border-slate-200"></div>
              <span className="flex-shrink mx-4 text-xs font-bold text-slate-400 uppercase font-mono">или демонстрация</span>
              <div className="flex-grow border-t border-slate-200"></div>
            </div>

            {/* Templates Box Grid */}
            <div className="space-y-2.5">
              <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider font-mono">Виртуальные тренировочные шаблоны</h3>
              
              <div className="grid grid-cols-1 gap-2">
                <button
                  onClick={() => handleSelectTemplate("healthy")}
                  className="w-full text-left p-3.5 bg-white rounded-xl border border-slate-150 hover:border-slate-300 flex items-center gap-3 transition-all cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-emerald-100 flex items-center justify-center text-emerald-600 font-extrabold text-xs">1</div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-900 text-xs">Симметричный лист березы</p>
                    <p className="text-[10px] text-slate-400 truncate">Шаблон чистого лесопарка (FA ~0.040, Балл 1)</p>
                  </div>
                  <ArrowRight size={14} className="text-slate-400" />
                </button>

                <button
                  onClick={() => handleSelectTemplate("medium")}
                  className="w-full text-left p-3.5 bg-white rounded-xl border border-slate-150 hover:border-slate-300 flex items-center gap-3 transition-all cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-amber-100 flex items-center justify-center text-amber-600 font-extrabold text-xs">3</div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-900 text-xs">Средне-асимметричный лист</p>
                    <p className="text-[10px] text-slate-400 truncate">Шаблон городского парка (FA ~0.062, Балл 3)</p>
                  </div>
                  <ArrowRight size={14} className="text-slate-400" />
                </button>

                <button
                  onClick={() => handleSelectTemplate("damaged")}
                  className="w-full text-left p-3.5 bg-white rounded-xl border border-slate-150 hover:border-slate-300 flex items-center gap-3 transition-all cursor-pointer"
                >
                  <div className="w-8 h-8 rounded-lg bg-rose-100 flex items-center justify-center text-rose-600 font-extrabold text-xs">5</div>
                  <div className="flex-1 min-w-0">
                    <p className="font-bold text-slate-900 text-xs">Ассиметричный лист березы</p>
                    <p className="text-[10px] text-slate-400 truncate">Шаблон автострады у промзоны (FA ~0.082, Балл 5)</p>
                  </div>
                  <ArrowRight size={14} className="text-slate-400" />
                </button>
              </div>
            </div>
          </div>

        </div>
      )}

      {/* ------------------------------------ */}
      {/* PROCESSING AI LOADER STAGE */}
      {/* ------------------------------------ */}
      {stage === "processing" && (
        <div className="flex-1 p-5 flex flex-col items-center justify-center bg-slate-950 text-slate-200 overflow-y-auto select-none">
          
          <div className="text-center mb-5 shrink-0">
            {/* Animated Scanning Circle Graphic */}
            <div className="relative w-16 h-16 mx-auto mb-3 flex items-center justify-center">
              {/* outer orbiting ring */}
              <div className="absolute inset-0 border-2 border-emerald-500/20 rounded-full" />
              <div className="absolute inset-0 border-2 border-t-emerald-500 rounded-full animate-spin" />
              
              {/* inner leaf indicator logo */}
              <div className="w-10 h-10 bg-emerald-950/60 rounded-xl flex items-center justify-center border border-emerald-500/30">
                <Camera size={18} className="text-emerald-400 animate-pulse" />
              </div>
            </div>
            <h2 className="text-xs font-extrabold tracking-widest text-emerald-400 font-sans uppercase">ИИ-Сегментация Листа</h2>
            <p className="text-[10px] text-slate-400 font-medium mt-1">Определение геометрических меток Берёзы повислой (Betula pendula)</p>
          </div>

          {/* Unified Checklist of Stages */}
          <div className="w-full max-w-xs space-y-2 my-2 shrink-0">
            {scanStatuses.map((status, index) => {
              const isCompleted = index < scanStatusIndex;
              const isActive = index === scanStatusIndex;
              const isPending = index > scanStatusIndex;
              
              return (
                <div 
                  key={index} 
                  className={`flex items-center gap-3 p-2 rounded-xl border transition-all duration-300 ${
                    isActive 
                      ? "bg-emerald-950/40 border-emerald-500/40 text-white shadow-xs shadow-emerald-950/40 translate-x-1" 
                      : isCompleted 
                      ? "bg-slate-900/40 border-emerald-900/20 text-emerald-400 opacity-90" 
                      : "bg-slate-950/20 border-slate-900/30 text-slate-600 opacity-30"
                  }`}
                >
                  {isCompleted ? (
                    <div className="w-4 h-4 rounded-full bg-emerald-500 flex items-center justify-center text-[9px] text-slate-950 font-black shrink-0">
                      ✓
                    </div>
                  ) : isActive ? (
                    <div className="w-4 h-4 rounded-full border border-emerald-400 border-t-transparent animate-spin shrink-0" />
                  ) : (
                    <div className="w-4 h-4 rounded-full border border-slate-800 shrink-0" />
                  )}
                  
                  <div className="flex-grow min-w-0">
                    <p className={`text-[10px] sm:text-[11px] font-semibold leading-tight truncate ${isActive ? "text-emerald-300" : ""}`}>
                      {status}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>

          <div className="w-full max-w-xs mt-4 pt-3 border-t border-slate-900 flex flex-col items-center shrink-0">
            {/* Smooth mini progress bar */}
            <div className="w-full bg-slate-900 h-1 rounded-full overflow-hidden">
              <div 
                className="bg-emerald-500 h-full transition-all duration-300"
                style={{ width: `${((scanStatusIndex + 1) / scanStatuses.length) * 100}%` }}
              />
            </div>
            <div className="w-full flex justify-between items-center text-[9px] font-mono text-slate-500 mt-2 font-semibold">
              <span>СТАТУС ИССЛЕДОВАНИЯ</span>
              <span>{Math.round(((scanStatusIndex + 1) / scanStatuses.length) * 100)}%</span>
            </div>
          </div>

        </div>
      )}

      {/* ------------------------------------ */}
      {/* ADJUST DRAGGABLE COORDINATES STAGE */}
      {/* ------------------------------------ */}
      {stage === "adjust" && (
        <div className="flex-1 flex flex-col justify-between">
          
          {/* Diagnostic Overlay Info Bar */}
          <div className="px-4 py-2 border-b bg-white border-slate-100 text-xs shrink-0 flex items-center justify-between">
            <div className="flex flex-col gap-0.5 max-w-[70%]">
              {/* Dynamic Snug AI Status Chip replaces absolute banner */}
              {isAiFallbackMode ? (
                <div className="text-[10px] text-amber-600 font-semibold flex items-center gap-1.5">
                  <ShieldAlert size={12} className="shrink-0 animate-pulse text-amber-500" />
                  <span>Калибруйте точки вручную</span>
                </div>
              ) : (
                photoUrl !== "template" && (
                  <div className="text-[10px] text-emerald-600 font-semibold flex items-center gap-1.5">
                    <CheckCircle size={12} className="shrink-0 text-emerald-500" />
                    <span>Точки расставлены ИИ ✨</span>
                  </div>
                )
              )}
            </div>
            
            <button
              onClick={() => {
                setStage("select");
                setIsAiFallbackMode(false);
              }}
              className="text-[10px] font-bold text-emerald-600 hover:text-emerald-700 font-mono flex items-center gap-1 p-1 hover:bg-slate-100 rounded"
            >
              <RefreshCw size={11} className="animate-spin-slow" />
              Перезагрузить
            </button>
          </div>

          {/* Draggable Active Sandbox Canvas Box */}
          <div className="flex-grow flex items-center justify-center p-3 relative bg-slate-900 block overflow-hidden shrink-0">
            
            <div 
              ref={containerRef}
              onMouseMove={handleMouseMove}
              onMouseUp={stopDragging}
              onPointerMove={handlePointerMove}
              onPointerUp={stopDragging}
              className="w-full max-w-[290px] aspect-[4/5] bg-slate-950 border border-slate-800 rounded-2xl relative overflow-hidden shadow-2xl select-none touch-none"
            >
              {/* Photo background or Green backlight canvas simulation with translation/scale/rotation */}
              <div 
                className="absolute inset-0 pointer-events-none select-none"
                style={{
                  transform: `scale(${imgZoom}) rotate(${imgRotate}deg) translate(${imgOffsetX}px, ${imgOffsetY}px)`,
                  transformOrigin: "center center",
                  transition: draggedPointKey ? "none" : "transform 0.15s ease-out"
                }}
              >
                {photoUrl === "template" ? (
                  <div className="absolute inset-0 bg-slate-950/90 flex items-center justify-center">
                    
                    {/* Styled Backlit Green Birch Leaf Vector representation */}
                    <svg viewBox="0 0 100 120" className="w-11/12 h-11/12 opacity-80 pointer-events-none">
                      <defs>
                        {/* Leaf gradient */}
                        <linearGradient id="leafGrad" x1="0%" y1="0%" x2="0%" y2="100%">
                          <stop offset="0%" stopColor="#15803d" />
                          <stop offset="60%" stopColor="#166534" />
                          <stop offset="100%" stopColor="#052e16" />
                        </linearGradient>
                      </defs>
                      
                      {/* Birch leaf silhouette path */}
                      <path
                        d="M50,105 C35,105 10,85 5,60 C0,35 25,20 50,8 C75,20 100,35 95,60 C90,85 65,105 50,105 Z"
                        fill="url(#leafGrad)"
                        stroke="#22c55e"
                        strokeWidth="1"
                      />
                      {/* Secondary vein lines subtle drawing */}
                      <path d="M50,90 Q30,75 22,60" fill="none" stroke="#22c55e" strokeWidth="0.5" opacity="0.4" />
                      <path d="M50,90 Q70,75 78,60" fill="none" stroke="#22c55e" strokeWidth="0.5" opacity="0.4" />
                      <path d="M50,70 Q30,55 25,40" fill="none" stroke="#22c55e" strokeWidth="0.5" opacity="0.4" />
                      <path d="M50,70 Q70,55 75,40" fill="none" stroke="#22c55e" strokeWidth="0.5" opacity="0.4" />
                    </svg>
                  </div>
                ) : (
                  <img 
                    src={photoUrl} 
                    alt="Birch Leaf captured for asymmetry analysis template" 
                    className="w-full h-full object-cover opacity-60 pointer-events-none select-none"
                  />
                )}
              </div>

              {/* Interactive SVG Lines overlay mapping (draw connecting joints between landmarks) */}
              <svg className="absolute inset-0 w-full h-full pointer-events-none">
                
                {/* Central main stem vein line */}
                <line 
                  x1={`${leafPoints.base.x}%`} y1={`${leafPoints.base.y}%`}
                  x2={`${leafPoints.tip.x}%`} y2={`${leafPoints.tip.y}%`}
                  stroke="#ef4444" strokeWidth="1.5" strokeDasharray="3 2"
                />

                {/* Left Vein 1 joint */}
                <line 
                  x1={`${leafPoints.leftVein1Base.x}%`} y1={`${leafPoints.leftVein1Base.y}%`}
                  x2={`${leafPoints.leftVein1End.x}%`} y2={`${leafPoints.leftVein1End.y}%`}
                  stroke="#4ade80" strokeWidth="1.5"
                />

                {/* Left Vein 2 joint */}
                <line 
                  x1={`${leafPoints.leftVein2Base.x}%`} y1={`${leafPoints.leftVein2Base.y}%`}
                  x2={`${leafPoints.leftVein2End.x}%`} y2={`${leafPoints.leftVein2End.y}%`}
                  stroke="#4ade80" strokeWidth="1.5"
                />

                {/* Right Vein 1 joint */}
                <line 
                  x1={`${leafPoints.rightVein1Base.x}%`} y1={`${leafPoints.rightVein1Base.y}%`}
                  x2={`${leafPoints.rightVein1End.x}%`} y2={`${leafPoints.rightVein1End.y}%`}
                  stroke="#4ade80" strokeWidth="1.5"
                />

                {/* Right Vein 2 joint */}
                <line 
                  x1={`${leafPoints.rightVein2Base.x}%`} y1={`${leafPoints.rightVein2Base.y}%`}
                  x2={`${leafPoints.rightVein2End.x}%`} y2={`${leafPoints.rightVein2End.y}%`}
                  stroke="#4ade80" strokeWidth="1.5"
                />

                {/* Left Trait 1 Max width reference line (horizontal project normal to main vein) */}
                <line 
                  x1={`${leafPoints.base.x}%`} y1={`${leafPoints.leftWidthMax.y}%`}
                  x2={`${leafPoints.leftWidthMax.x}%`} y2={`${leafPoints.leftWidthMax.y}%`}
                  stroke="#fbbf24" strokeWidth="1" strokeDasharray="2 2"
                />

                {/* Right Trait 1 Max width reference line */}
                <line 
                  x1={`${leafPoints.base.x}%`} y1={`${leafPoints.rightWidthMax.y}%`}
                  x2={`${leafPoints.rightWidthMax.x}%`} y2={`${leafPoints.rightWidthMax.y}%`}
                  stroke="#fbbf24" strokeWidth="1" strokeDasharray="2 2"
                />

                {/* Trait 4 distance connections (between vein 1 ends and vein 2 ends) */}
                <line 
                  x1={`${leafPoints.leftVein1End.x}%`} y1={`${leafPoints.leftVein1End.y}%`}
                  x2={`${leafPoints.leftVein2End.x}%`} y2={`${leafPoints.leftVein2End.y}%`}
                  stroke="#38bdf8" strokeWidth="1" strokeDasharray="1 1"
                />
                
                <line 
                  x1={`${leafPoints.rightVein1End.x}%`} y1={`${leafPoints.rightVein1End.y}%`}
                  x2={`${leafPoints.rightVein2End.x}%`} y2={`${leafPoints.rightVein2End.y}%`}
                  stroke="#38bdf8" strokeWidth="1" strokeDasharray="1 1"
                />

              </svg>

              {/* Draggable Point Handles (12 landmarks) */}
              {(Object.keys(leafPoints) as Array<keyof LeafPoints>).map((key) => {
                const pt = leafPoints[key];
                
                // Color code the handles for easier identification by the user
                const isApexJoint = key === "base" || key === "tip";
                const isLeftEdge = key.startsWith("left");
                const isVeinBase = key.endsWith("Base");

                let markerColor = "bg-emerald-500 ring-emerald-500/30"; // secondary vein points
                if (isApexJoint) markerColor = "bg-rose-500 ring-rose-500/30"; // central main vein poles
                else if (key.endsWith("Max")) markerColor = "bg-amber-400 ring-amber-400/30"; // widths
                else if (isVeinBase) markerColor = "bg-slate-300 ring-slate-100/10"; // stem attachments (grey/white)

                return (
                  <div
                    key={key}
                    onMouseDown={(e) => handleMouseDown(key, e)}
                    onPointerDown={(e) => handleMouseDown(key, e as any)}
                    className="absolute w-6 h-6 -translate-x-3 -translate-y-3 flex items-center justify-center cursor-pointer select-none active:scale-125 transition-transform group"
                    style={{ left: `${pt.x}%`, top: `${pt.y}%` }}
                  >
                    <div className={`w-3.5 h-3.5 rounded-full border border-white shadow-md ring-4 ${markerColor} transition-transform group-hover:scale-110`} />
                    
                    {/* Floating Label overlay */}
                    <span className="absolute top-4 bg-slate-950/80 px-1 rounded text-[8px] text-white opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none select-none z-10">
                      {key === "base" ? "Основание" : key === "tip" ? "Вершина" : key.includes("Max") ? "Ширина" : key.includes("Vein1") ? "Жилка 1" : "Жилка 2"}
                    </span>
                  </div>
                );
              })}

            </div>

            {/* AI Callout status is now displayed in the top Info Bar to prevent covering critical viewport landmarks */}

            {/* Quick Helper Button - Clickable trigger for popup modal */}
            <button 
              onClick={() => setShowHelpModal(true)}
              className="absolute bottom-2 right-2 p-2 rounded-full bg-slate-800/90 border border-slate-705 text-emerald-400 hover:text-emerald-300 hover:bg-slate-700 active:scale-90 transition-all cursor-pointer shadow-lg flex items-center justify-center"
              title="Инструкция к точкам"
            >
              <HelpCircle size={16} />
            </button>

          </div>

          {/* ------------------------------------ */}
          {/* PHOTO ADJUSTMENT CONTROLS PANEL */}
          {/* ------------------------------------ */}
          <div className="bg-slate-100 border-b border-slate-200 px-4 py-3 space-y-2 shrink-0">
            <div className="flex items-center justify-between">
              <button 
                onClick={() => setIsAlignPanelOpen(!isAlignPanelOpen)}
                className="flex items-center gap-2 text-xs font-bold text-slate-800 hover:text-slate-950 focus:outline-none cursor-pointer"
              >
                <SlidersHorizontal size={14} className="text-emerald-600" />
                <span>Выравнивание и масштаб фотографии</span>
                <span className="text-[10px] bg-slate-200 text-slate-600 px-1.5 py-0.5 rounded-full font-mono font-medium">
                  {isAlignPanelOpen ? "Свернуть" : "Развернуть"}
                </span>
              </button>
              {(imgZoom !== 1.0 || imgRotate !== 0 || imgOffsetX !== 0 || imgOffsetY !== 0) && (
                <button
                  onClick={resetImageTransforms}
                  className="text-[10px] font-bold text-rose-600 hover:text-rose-700 bg-white border border-rose-100 shadow-3xs px-2 py-0.5 rounded-lg active:scale-95 transition-all cursor-pointer font-mono"
                >
                  Сбросить
                </button>
              )}
            </div>

            {isAlignPanelOpen && (
              <div className="space-y-3 pt-2 text-xs text-slate-700 animate-slide-down">
                {/* Scale and Angle side by side */}
                <div className="grid grid-cols-2 gap-4">
                  {/* Zoom Slider */}
                  <div className="space-y-1">
                    <div className="flex justify-between items-center text-[10px] font-bold text-slate-500">
                      <span className="flex items-center gap-1"><ZoomIn size={12} /> Масштаб:</span>
                      <span className="font-mono text-slate-800 bg-slate-200 px-1.5 rounded">{imgZoom.toFixed(2)}x</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button 
                        onClick={() => setImgZoom(z => Math.max(0.5, z - 0.1))}
                        disabled={imgZoom <= 0.5}
                        className="w-6 h-6 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:bg-slate-50 active:scale-90 font-bold disabled:opacity-50"
                      >-</button>
                      <input 
                        type="range"
                        min="0.5"
                        max="3.0"
                        step="0.05"
                        value={imgZoom}
                        onChange={(e) => setImgZoom(parseFloat(e.target.value))}
                        className="flex-1 h-1.5 bg-slate-300 rounded-lg appearance-none cursor-pointer accent-emerald-600"
                        aria-label="Искажение масштаба подложки"
                      />
                      <button 
                        onClick={() => setImgZoom(z => Math.min(3.0, z + 0.1))}
                        disabled={imgZoom >= 3.0}
                        className="w-6 h-6 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:bg-slate-50 active:scale-90 font-bold disabled:opacity-50"
                      >+</button>
                    </div>
                  </div>

                  {/* Rotate Slider */}
                  <div className="space-y-1">
                    <div className="flex justify-between items-center text-[10px] font-bold text-slate-500">
                      <span className="flex items-center gap-1"><RotateCw size={12} /> Поворот:</span>
                      <span className="font-mono text-slate-800 bg-slate-200 px-1.5 rounded">{imgRotate}°</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button 
                        onClick={() => setImgRotate(r => (r - 90 < -180 ? r - 90 + 360 : r - 90))}
                        className="w-6 h-6 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:bg-slate-50 active:scale-90 font-bold"
                        title="-90°"
                      >-90</button>
                      <input 
                        type="range"
                        min="-180"
                        max="180"
                        step="1"
                        value={imgRotate}
                        onChange={(e) => setImgRotate(parseInt(e.target.value))}
                        className="flex-1 h-1.5 bg-slate-300 rounded-lg appearance-none cursor-pointer accent-emerald-600"
                        aria-label="Искажение угла поворота подложки"
                      />
                      <button 
                        onClick={() => setImgRotate(r => (r + 90 > 180 ? r + 90 - 360 : r + 90))}
                        className="w-6 h-6 rounded-lg bg-white border border-slate-200 flex items-center justify-center text-slate-500 hover:bg-slate-50 active:scale-90 font-bold"
                        title="+90°"
                      >+90</button>
                    </div>
                  </div>
                </div>

                {/* Offset Shift Controls */}
                <div className="grid grid-cols-2 gap-4 pt-1.5 border-t border-slate-200/55">
                  {/* Offset X */}
                  <div className="space-y-1">
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-tight">Сдвиг по ширине (X):</p>
                    <div className="flex items-center gap-2">
                      <input 
                        type="range"
                        min="-200"
                        max="200"
                        step="2"
                        value={imgOffsetX}
                        onChange={(e) => setImgOffsetX(parseInt(e.target.value))}
                        className="flex-1 h-1.5 bg-slate-300 rounded-lg appearance-none cursor-pointer accent-slate-600"
                        aria-label="Сдвиг по горизонтали подложки"
                      />
                      <span className="font-mono text-[9px] text-slate-500 w-8 text-right font-medium">{imgOffsetX}px</span>
                    </div>
                  </div>

                  {/* Offset Y */}
                  <div className="space-y-1">
                    <p className="text-[10px] font-bold text-slate-400 uppercase tracking-tight">Сдвиг по высоте (Y):</p>
                    <div className="flex items-center gap-2">
                      <input 
                        type="range"
                        min="-200"
                        max="200"
                        step="2"
                        value={imgOffsetY}
                        onChange={(e) => setImgOffsetY(parseInt(e.target.value))}
                        className="flex-1 h-1.5 bg-slate-300 rounded-lg appearance-none cursor-pointer accent-slate-600"
                        aria-label="Сдвиг по вертикали подложки"
                      />
                      <span className="font-mono text-[9px] text-slate-500 w-8 text-right font-medium">{imgOffsetY}px</span>
                    </div>
                  </div>
                </div>

                {/* Help tip */}
                <p className="text-[10px] text-slate-400 leading-normal bg-white p-1.5 rounded-lg border border-slate-100">
                  💡 Настройте масштаб и поворот фотографии так, чтобы основание листа березы совпало с нижним красным пином, а острая верхушка листа — с верхним. Это исключит систематическую погрешность наклона!
                </p>
              </div>
            )}
          </div>

          {/* --------------------------- */}
          {/* MATH CALCULATIONS SUMMARY CARD */}
          {/* --------------------------- */}
          <div className="px-4 py-3 bg-white border-y border-slate-100 flex flex-col gap-2.5 shrink-0">
            
            <div className="flex justify-between items-center bg-slate-50 p-2.5 rounded-xl border border-slate-100">
              <div className="space-y-0.5">
                <span className="text-[10px] text-slate-400 uppercase tracking-wider font-bold">Интегральный показатель Z:</span>
                <div className="flex items-center gap-2">
                  <span className="text-lg font-black text-slate-800 font-mono">{zScore.toFixed(4)}</span>
                  <span className="text-xs text-slate-400 font-medium">/ балл {rating.score || "—"}</span>
                </div>
              </div>

              <div className={`p-1.5 px-3 rounded-lg text-xs font-bold ${rating.color}`}>
                {rating.status}
              </div>
            </div>

            {/* Interactive sliders/traits preview */}
            <div className="grid grid-cols-5 gap-1.5 text-center">
              {[
                { label: "Y1 Ширина", val: traits.t1Width.yDiff },
                { label: "Y2 Жилка", val: traits.t2VeinLen.yDiff },
                { label: "Y3 Осн.", val: traits.t3VeinDist.yDiff },
                { label: "Y4 Кон.", val: traits.t4EndDist.yDiff },
                { label: "Y5 Угол", val: traits.t5Angle.yDiff },
              ].map((tr, index) => (
                <div key={index} className="bg-slate-50/50 p-1.5 rounded-lg border border-slate-100 text-[10px] flex flex-col justify-between">
                  <span className="text-[9px] text-slate-400 font-semibold truncate leading-none mb-1">{tr.label}</span>
                  <span className="font-mono font-bold text-slate-800">{tr.val.toFixed(4)}</span>
                </div>
              ))}
            </div>

          </div>

          {/* --------------------------- */}
          {/* IMMEDIATE SAVE AND PIN BUTTON */}
          {/* --------------------------- */}
          <div className="p-4 bg-white shrink-0">
            {photoUrl === "template" ? (
              <button
                onClick={() => {
                  setStage("select");
                  setIsAiFallbackMode(false);
                }}
                className="w-full py-3 rounded-2xl font-bold text-sm text-white bg-slate-700 hover:bg-slate-800 cursor-pointer flex items-center justify-center gap-2 shadow-md active:scale-95 transition-all"
              >
                Вернуться назад
              </button>
            ) : (
              <button
                onClick={handleSaveResult}
                disabled={isSaving}
                className={`w-full py-3 rounded-2xl font-bold text-sm text-white flex items-center justify-center gap-2 shadow-md active:scale-95 transition-all ${
                  isSaving
                    ? "bg-emerald-400 cursor-not-allowed"
                    : "bg-emerald-600 hover:bg-emerald-700 cursor-pointer"
                }`}
              >
                {isSaving ? (
                  <>
                    <RefreshCw size={18} className="animate-spin" />
                    {saveStatusText || "Сохранение..."}
                  </>
                ) : (
                  <>
                    <Save size={18} />
                    Сохранить и поставить метку
                  </>
                )}
              </button>
            )}
          </div>

        </div>
      )}

      {/* ------------------------------------ */}
      {/* SAVED SUCCESS STAGE */}
      {/* ------------------------------------ */}
      {stage === "saved" && (
        <div className="flex-1 p-6 flex flex-col items-center justify-center text-center bg-white space-y-6">
          
          <div className="w-16 h-16 rounded-full bg-emerald-100 border border-emerald-200 flex items-center justify-center text-emerald-600 animate-bounce">
            <CheckCircle size={32} />
          </div>

          <div className="space-y-2">
            <h2 className="text-lg font-bold text-slate-900 tracking-tight">Замер сохранен!</h2>
            <p className="text-xs text-slate-500 max-w-[220px] leading-relaxed mx-auto">
              Данный лист берёзы повислой успешно добавлен в журнал территории. Данные и асимметрия обновлены.
            </p>
          </div>

          {/* Quick indicators */}
          <div className="bg-slate-50/50 p-4 border border-slate-100 rounded-2xl w-full max-w-[250px] space-y-1">
            <p className="text-[10px] font-bold font-mono tracking-wider text-slate-400">ПОКАЗАТЕЛЬ ЗАМЕРА:</p>
            <p className="text-2xl font-black font-mono text-slate-800 leading-none">{zScore.toFixed(4)}</p>
            <p className={`text-[10px] font-bold inline-block px-2.5 py-0.5 rounded-full mt-2 ${rating.color}`}>
              Балл {rating.score}: {rating.status}
            </p>
          </div>

          {botSentMsg && (
            <div className="bg-teal-50 border border-teal-200 rounded-xl p-2 text-xs text-teal-800 font-semibold text-center animate-fade-in flex items-center justify-center gap-1.5 w-full max-w-[250px]">
              <Check size={13} className="text-teal-600 shrink-0" />
              <span>{botSentMsg}</span>
            </div>
          )}

          <div className="flex flex-col gap-2 w-full max-w-[240px]">
            {/* Direct Send to @bpaqi_bot */}
            <button
              onClick={() => {
                const res = sendDataToTelegramBot({
                  type: "leaf_measurement_new",
                  bot: BOT_USERNAME,
                  zScore: zScore,
                  score: rating.score,
                  status: rating.status,
                  timestamp: new Date().toISOString(),
                });
                setBotSentMsg(res.message);
                setTimeout(() => setBotSentMsg(null), 3500);
              }}
              className="w-full py-2.5 px-3 rounded-xl text-xs font-bold text-white bg-teal-700 hover:bg-teal-800 active:scale-97 transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow-sm"
            >
              <Send size={13} />
              <span>Отправить замер в @{BOT_USERNAME}</span>
            </button>

            <button
              onClick={() => {
                setStage("select");
                setIsAiFallbackMode(false);
                setBotSentMsg(null);
              }}
              className="w-full py-2 px-4 rounded-xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 transition-all cursor-pointer"
            >
              Сканировать еще один лист
            </button>
            <button
              onClick={onNavigateToHistory}
              className="w-full py-2 px-4 rounded-xl text-xs font-semibold text-slate-600 border border-slate-200 bg-white hover:bg-slate-50 transition-all cursor-pointer"
            >
              Перейти к журналу наблюдений
            </button>
          </div>

        </div>
      )}

      {/* ------------------------------------ */}
      {/* ANATOMICAL GUIDELINES HELP SHEET / MODAL */}
      {/* ------------------------------------ */}
      {showHelpModal && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-end justify-center z-50 animate-fade-in p-0">
          <div className="bg-white rounded-t-3xl w-full max-w-md p-6 space-y-4 max-h-[85vh] overflow-y-auto shadow-2xl relative animate-slide-up border-t border-slate-100">
            <div className="flex justify-between items-center pb-2 border-b border-slate-100">
              <h3 className="font-bold text-slate-950 text-sm flex items-center gap-1.5">
                <HelpCircle className="text-emerald-600" size={18} />
                Анатомия точек и 5 признаков
              </h3>
              <button 
                onClick={() => setShowHelpModal(false)}
                className="text-xs px-2.5 py-1.5 font-bold text-slate-400 hover:text-slate-600 hover:bg-slate-100 rounded-lg cursor-pointer"
              >
                Закрыть
              </button>
            </div>

            <div className="space-y-4 text-xs text-slate-700 leading-relaxed font-sans">
              <p>
                Согласно методике В.М. Захарова (1997 г.), оценка качества среды проводится по 5 морфологическим признакам асимметрии листа берёзы повислой. Сдвиньте точки на фотографии точно по научным ориентирам ниже.
              </p>

              {/* Landmark point guide */}
              <div className="space-y-2">
                <h4 className="font-bold text-[10px] text-slate-400 uppercase tracking-wider font-mono">Как расставить точки:</h4>
                <div className="grid grid-cols-1 gap-2">
                  <div className="p-2.5 rounded-xl bg-rose-50 border border-rose-100/50">
                    <span className="font-bold text-rose-700 text-[11px] block mb-1">🔴 Пинсы основания и верхушки (Ось):</span>
                    <ul className="list-disc pl-4 space-y-1 text-[11px] text-slate-600">
                      <li><strong>Вершина</strong>: Самый кончик листа (апекс), на конце главной вертикальной жилки.</li>
                      <li><strong>Основание</strong>: Место прикрепления черешка (ножки) к листовой пластине.</li>
                    </ul>
                  </div>
                  <div className="p-2.5 rounded-xl bg-emerald-50 border border-emerald-100/50">
                    <span className="font-bold text-emerald-700 text-[11px] block mb-1">🟢 Боковые жилки (Жилка 1 и 2):</span>
                    <ul className="list-disc pl-4 space-y-1 text-[11px] text-slate-600">
                      <li><strong>Жилка 1 & 2 на оси (основания)</strong>: Место разветвления 1-й (нижней) и 2-й (верхней) боковых жилок от центрального ствола. Они должны лежать строго на осевой линии!</li>
                      <li><strong>Жилка 1 & 2 на краю (концы)</strong>: Место, где эти жилки выходят на наружный зубчатый край листа.</li>
                    </ul>
                  </div>
                  <div className="p-2.5 rounded-xl bg-amber-50 border border-amber-100/50">
                    <span className="font-bold text-amber-700 text-[11px] block mb-1">🟡 Максимумы ширины:</span>
                    <ul className="list-disc pl-4 space-y-1 text-[11px] text-slate-600">
                      <li><strong>Левый/Правый макс. ширины</strong>: крайние боковые точки листа во внешней части наибольшего горизонтального уширения.</li>
                    </ul>
                  </div>
                </div>
              </div>

              {/* Parameter formulas guide */}
              <div className="space-y-2 pt-2 border-t border-slate-100">
                <h4 className="font-bold text-[10px] text-slate-400 uppercase tracking-wider font-mono">Пять вычисляемых признаков (Y1-Y5):</h4>
                <div className="space-y-1 text-[11px] font-medium text-slate-600">
                  <p><strong>Y1</strong>: ширина половины листовой пластины от главной жилки.</p>
                  <p><strong>Y2</strong>: длина второй боковой жилки.</p>
                  <p><strong>Y3</strong>: расстояние между основаниями 1-й и 2-й боковых жилок.</p>
                  <p><strong>Y4</strong>: расстояние между концами этих жилок по краю листа.</p>
                  <p><strong>Y5</strong>: угол отклонения второй боковой жилки от центральной оси.</p>
                </div>
              </div>
            </div>

            <button
              onClick={() => setShowHelpModal(false)}
              className="w-full py-2 px-4 bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs rounded-xl cursor-pointer"
            >
              Ясно, вернуться к доводке
            </button>
          </div>
        </div>
      )}

      {showManualMapPrompt && (
        <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center z-50 animate-fade-in p-4">
          <div className="bg-white rounded-2xl w-full max-w-sm p-5 space-y-4 shadow-2xl relative animate-scale-up border border-slate-100 text-center">
            <div className="w-12 h-12 rounded-full bg-amber-100 border border-amber-200 flex items-center justify-center text-amber-600 mx-auto">
              <ShieldAlert size={24} />
            </div>
            
            <div className="space-y-1.5">
              <h3 className="font-extrabold text-slate-900 text-sm leading-snug">
                Координаты GPS недоступны
              </h3>
              <p className="text-[11px] text-slate-500 leading-relaxed">
                Не удалось автоматически определить координаты (GPS сигнал отсутствует, отключен или запрещен доступ).
              </p>
            </div>

            <div className="bg-amber-50/50 p-3 rounded-xl border border-amber-100 text-[11px] text-amber-800 text-left space-y-1.5 font-medium">
              <p className="font-bold flex items-center gap-1 text-amber-900 leading-none">
                📍 Рекомендуемый вариант:
              </p>
              <p className="leading-relaxed">
                Перейдите на вкладку «Карта» и укажите точное место сбора листа вручную на карте.
              </p>
            </div>

            <div className="flex flex-col gap-2 pt-1">
              <button
                onClick={() => {
                  if (tempPendingAnalysis) {
                    onSetPendingMeasurement?.(tempPendingAnalysis);
                    setShowManualMapPrompt(false);
                    // Reset scan screen stage back to select to be ready for next leaf
                    setStage("select");
                    onNavigateToMap?.();
                  }
                }}
                className="w-full py-2.5 rounded-xl text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 transition-all cursor-pointer flex items-center justify-center gap-1.5 shadow-sm"
              >
                <MapPin size={14} />
                Отметить на карте вручную
              </button>
              
              <button
                onClick={() => {
                  setShowManualMapPrompt(false);
                  // Trigger proceed with randomized fallback coords
                  setSaveStatusText("Сохранение...");
                  setIsSaving(true);
                  const destId = "temp_" + Date.now();
                  if (tempPendingAnalysis) {
                    onAddNewMeasurement(destId, tempPendingAnalysis);
                    setTimeout(() => {
                      setIsSaving(false);
                      setStage("saved");
                    }, 500);
                  }
                }}
                className="w-full py-2.5 rounded-xl text-xs font-semibold text-slate-600 border border-slate-200 bg-white hover:bg-slate-50 transition-all cursor-pointer"
              >
                Сохранить со случайной меткой
              </button>

              <button
                onClick={() => {
                  setShowManualMapPrompt(false);
                  setTempPendingAnalysis(null);
                }}
                className="w-full py-2 rounded-xl text-[10px] font-bold text-slate-400 hover:text-slate-500 hover:bg-slate-50 transition-all cursor-pointer"
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}

    </div>
  );
}
