import React, { useState, useEffect, useRef } from "react";
import { Territory, EnvironmentalRating } from "../types";
import { getEnvironmentalRating } from "../utils";
import { telegramHaptic } from "../telegram";
import { 
  Compass, Plus, Navigation, Info, AlertTriangle, 
  MapPin, Locate, RefreshCw, Crosshair, Check,
  Search, X, Loader2
} from "lucide-react";

declare global {
  interface Window {
    ymaps: any;
  }
}

// Setup Yandex API Key access safely across client and server
const API_KEY =
  (typeof process !== "undefined" && (process as any)?.env?.YANDEX_MAPS_API_KEY) ||
  (import.meta as any).env?.VITE_YANDEX_MAPS_API_KEY ||
  (globalThis as any).YANDEX_MAPS_API_KEY ||
  "";

interface MapScreenProps {
  territories: Territory[];
  onSelectTerritory: (territoryId: string) => void;
  onAddTerritory: (name: string, desc: string, lat: number, lng: number) => void;
  initialAddMode?: boolean;
  clearInitialAddMode?: () => void;
  hasPendingMeasurement?: boolean;
  onCancelPendingMeasurement?: () => void;
}

interface SearchResult {
  id?: string;
  name: string;
  description: string;
  fullAddress: string;
  coords: { lat: number; lng: number };
  isExistingTerritory?: boolean;
}

export default function MapScreen({ 
  territories, 
  onSelectTerritory, 
  onAddTerritory,
  initialAddMode = false,
  clearInitialAddMode,
  hasPendingMeasurement = false,
  onCancelPendingMeasurement
}: MapScreenProps) {
  const [selectedTerritory, setSelectedTerritory] = useState<Territory | null>(null);
  const [isAddingMode, setIsAddingMode] = useState(initialAddMode || hasPendingMeasurement);
  const [clickCoords, setClickCoords] = useState<{ lat: number; lng: number } | null>(null);
  const [formName, setFormName] = useState("");
  const [formDesc, setFormDesc] = useState("");
  
  const [isLoaded, setIsLoaded] = useState(false);
  const [loadError, setLoadError] = useState(false);
  const [mapInitializedVal, setMapInitialized] = useState(0);
  const [isLocating, setIsLocating] = useState(false);
  const [userLocation, setUserLocation] = useState<{ lat: number; lng: number } | null>(null);

  // Address Search state
  const [searchQuery, setSearchQuery] = useState("");
  const [searchResults, setSearchResults] = useState<SearchResult[]>([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showResultsDropdown, setShowResultsDropdown] = useState(false);
  const searchContainerRef = useRef<HTMLDivElement>(null);

  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<any>(null);
  const isAddingModeRef = useRef(isAddingMode);
  const userPlacemarkRef = useRef<any>(null);

  // Auto-activate adding mode if navigated with initialAddMode = true
  useEffect(() => {
    if (initialAddMode) {
      setIsAddingMode(true);
      clearInitialAddMode?.();
    }
  }, [initialAddMode, clearInitialAddMode]);

  // Handle pending measurement auto-adds and defaults
  useEffect(() => {
    if (hasPendingMeasurement) {
      setIsAddingMode(true);
      setFormName("Новая точка замера");
      setFormDesc("Добавлена вручную на карте после сканирования");
    }
  }, [hasPendingMeasurement]);

  // Sync isAddingMode with a ref to avoid stale closures in map click listener
  useEffect(() => {
    isAddingModeRef.current = isAddingMode;
  }, [isAddingMode]);

  // Close search dropdown on outside click
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (searchContainerRef.current && !searchContainerRef.current.contains(e.target as Node)) {
        setShowResultsDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const [isSearchOpen, setIsSearchOpen] = useState(false);

  // Debounced address search as user types (works in both adding mode and browse search mode)
  useEffect(() => {
    if (!isAddingMode && !isSearchOpen) return;
    const trimmed = searchQuery.trim();
    if (trimmed.length < 2) {
      setSearchResults([]);
      setShowResultsDropdown(false);
      return;
    }

    const timer = setTimeout(() => {
      handleAddressSearch(trimmed);
    }, 380);

    return () => clearTimeout(timer);
  }, [searchQuery, isAddingMode, isSearchOpen]);

  const handleAddressSearch = async (queryText?: string) => {
    const q = (queryText !== undefined ? queryText : searchQuery).trim();
    if (!q) {
      setSearchResults([]);
      setShowResultsDropdown(false);
      return;
    }

    setIsSearching(true);
    setShowResultsDropdown(true);

    try {
      // 1. Instant local matching against existing territories
      const localMatches: SearchResult[] = territories
        .filter(t =>
          t.name.toLowerCase().includes(q.toLowerCase()) ||
          (t.description && t.description.toLowerCase().includes(q.toLowerCase()))
        )
        .slice(0, 4)
        .map(t => ({
          id: t.id,
          name: t.name,
          description: t.description || "Сохраненная точка обследования",
          fullAddress: `Координаты: ${t.latitude.toFixed(4)}, ${t.longitude.toFixed(4)}`,
          coords: { lat: t.latitude, lng: t.longitude },
          isExistingTerritory: true
        }));

      // 2. Query server geocoding endpoint (OSM Nominatim + Photon fallback)
      let geocodeResults: SearchResult[] = [];
      try {
        const response = await fetch(`/api/geocode?q=${encodeURIComponent(q)}`);
        if (response.ok) {
          const data = await response.json();
          if (data && Array.isArray(data.results)) {
            geocodeResults = data.results.map((r: any) => ({
              name: r.name,
              description: r.description || "",
              fullAddress: r.fullAddress || r.name,
              coords: r.coords,
              isExistingTerritory: false
            }));
          }
        }
      } catch (apiErr) {
        console.warn("Server geocoding API error:", apiErr);
      }

      // 3. Fallback: try Yandex Maps client-side geocoder if server returned nothing
      if (geocodeResults.length === 0 && window.ymaps && typeof window.ymaps.geocode === "function") {
        try {
          const res = await window.ymaps.geocode(q, { results: 5 });
          const geoObjects = res.geoObjects;
          const count = geoObjects.getLength();
          for (let i = 0; i < count; i++) {
            const obj = geoObjects.get(i);
            const coords = obj.geometry.getCoordinates();
            if (coords && coords.length === 2) {
              const fullAddress = obj.getAddressLine() || "";
              const name = obj.getName() || fullAddress;
              const desc = obj.getDescription() || "";
              geocodeResults.push({
                name,
                description: desc,
                fullAddress,
                coords: { lat: coords[0], lng: coords[1] },
                isExistingTerritory: false
              });
            }
          }
        } catch {
          // ignore client ymaps geocoding errors
        }
      }

      // Combine local matches and geocoded places
      const combined = [...localMatches, ...geocodeResults];
      setSearchResults(combined);
    } catch (err) {
      console.warn("Geocoding search failed:", err);
      setSearchResults([]);
    } finally {
      setIsSearching(false);
    }
  };

  const handleSelectSearchResult = (result: SearchResult) => {
    telegramHaptic("selection");
    setSearchQuery(result.name);
    setShowResultsDropdown(false);

    // Pan map to chosen coordinates
    if (mapInstanceRef.current) {
      mapInstanceRef.current.setCenter([result.coords.lat, result.coords.lng], 15, {
        duration: 350
      });
    }

    // If user clicked an existing survey point
    if (result.isExistingTerritory && result.id) {
      const found = territories.find(t => t.id === result.id);
      if (found) {
        setSelectedTerritory(found);
      }
      return;
    }

    // If in adding mode (or opened from search)
    setClickCoords(result.coords);
    setFormName(result.name);
    setFormDesc(result.fullAddress || result.description);
    if (!isAddingMode) {
      setIsAddingMode(true);
    }
  };

  const getTerritoryRating = (territory: Territory): EnvironmentalRating => {
    if (territory.leaves.length === 0) {
      return {
        fa: 0,
        score: 0,
        status: "Нет данных",
        color: "bg-slate-400 text-white",
        hexColor: "#93a1b7",
        description: "На этой территории еще нет замеров листьев березы."
      };
    }
    const sumZ = territory.leaves.reduce((sum, leaf) => sum + leaf.zScore, 0);
    const avgFA = sumZ / territory.leaves.length;
    return getEnvironmentalRating(avgFA);
  };

  const handleMapClickInternal = async (lat: number, lng: number) => {
    if (isAddingModeRef.current) {
      telegramHaptic("selection");
      setShowResultsDropdown(false);
      setClickCoords({ lat, lng });
      setFormName("Определение адреса...");
      setFormDesc(`Координаты: ${lat.toFixed(4)}, ${lng.toFixed(4)}`);
      
      // 1. Try server reverse geocoding first (fast, accurate OSM data in Russian)
      try {
        const res = await fetch(`/api/reverse-geocode?lat=${lat}&lng=${lng}`);
        if (res.ok) {
          const data = await res.json();
          if (data && data.success && data.name) {
            setFormName(data.name);
            setFormDesc(data.fullAddress || `Координаты: ${lat.toFixed(4)}, ${lng.toFixed(4)}`);
            return;
          }
        }
      } catch (err) {
        console.warn("Reverse geocode fetch failed:", err);
      }

      // 2. Fallback to client-side Yandex geocode if present
      if (window.ymaps && window.ymaps.geocode) {
        try {
          const res = await window.ymaps.geocode([lat, lng]);
          const firstGeoObject = res.geoObjects.get(0);
          if (firstGeoObject) {
            const fullAddress = firstGeoObject.getAddressLine();
            const shortName = firstGeoObject.getName() || "Точка обследования";
            setFormName(shortName);
            setFormDesc(fullAddress || "Определено автоматически");
            return;
          }
        } catch {
          // ignore
        }
      }

      // 3. Fallback coordinates
      setFormName("Точка обследования");
      setFormDesc(`Координаты: ${lat.toFixed(4)}, ${lng.toFixed(4)}`);
    }
  };

  const handleSaveLocation = () => {
    if (!clickCoords || !formName) return;
    telegramHaptic("success");
    onAddTerritory(formName, formDesc, clickCoords.lat, clickCoords.lng);
    
    // Reset state
    setFormName("");
    setFormDesc("");
    setClickCoords(null);
    setSearchQuery("");
    setSearchResults([]);
    setShowResultsDropdown(false);
    setIsAddingMode(false);
  };

  // Determine dynamic map center
  const defaultCenter = territories.length > 0
    ? { 
        lat: territories.reduce((sum, t) => sum + t.latitude, 0) / territories.length,
        lng: territories.reduce((sum, t) => sum + t.longitude, 0) / territories.length
      }
    : { lat: 43.16, lng: 131.90 }; // Default Vladivostok fallback

  // Dynamically Load Yandex.Maps API script
  useEffect(() => {
    if (typeof window === "undefined") return;

    if (window.ymaps) {
      setIsLoaded(true);
      return;
    }

    const existingScript = document.getElementById("yandex-maps-script");
    if (existingScript) {
      const handleLoad = () => setIsLoaded(true);
      const handleError = () => setLoadError(true);
      
      existingScript.addEventListener("load", handleLoad);
      existingScript.addEventListener("error", handleError);
      return () => {
        existingScript.removeEventListener("load", handleLoad);
        existingScript.removeEventListener("error", handleError);
      };
    }

    const script = document.createElement("script");
    script.id = "yandex-maps-script";
    const apiKeyQuery = API_KEY ? `&apikey=${API_KEY}` : "";
    script.src = `https://api-maps.yandex.ru/2.1/?lang=ru_RU${apiKeyQuery}`;
    script.type = "text/javascript";
    script.async = true;
    script.onload = () => setIsLoaded(true);
    script.onerror = () => setLoadError(true);

    document.body.appendChild(script);
  }, []);

  // Initialize Map
  useEffect(() => {
    if (!isLoaded || !mapRef.current || mapInstanceRef.current) return;

    const initMap = () => {
      if (!window.ymaps || !mapRef.current) return;

      window.ymaps.ready(() => {
        if (mapInstanceRef.current) return;

        const map = new window.ymaps.Map(mapRef.current, {
          center: [defaultCenter.lat, defaultCenter.lng],
          zoom: territories.length > 0 ? 11 : 12,
          controls: ["zoomControl", "typeSelector"]
        }, {
          suppressMapOpenBlock: true,
          autoFitToViewport: "always"
        });

        // Enable responsive mobile touch gestures
        map.behaviors.enable(["drag", "scrollZoom", "multiTouch", "dblClickZoom"]);

        mapInstanceRef.current = map;

        map.events.add("click", (e: any) => {
          const coords = e.get("coords");
          if (coords && coords.length === 2) {
            handleMapClickInternal(coords[0], coords[1]);
          }
        });

        // Trigger marker redraw once map is ready
        setMapInitialized(prev => prev + 1);
      });
    };

    initMap();

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.destroy();
        mapInstanceRef.current = null;
      }
    };
  }, [isLoaded]);

  // Handle Telegram viewport resize and window resize to maintain 100% full height
  useEffect(() => {
    const handleResize = () => {
      if (mapInstanceRef.current && mapInstanceRef.current.container) {
        mapInstanceRef.current.container.fitToViewport();
      }
    };

    window.addEventListener("resize", handleResize);
    window.Telegram?.WebApp?.onEvent?.("viewportChanged", handleResize);

    return () => {
      window.removeEventListener("resize", handleResize);
      window.Telegram?.WebApp?.offEvent?.("viewportChanged", handleResize);
    };
  }, []);

  // Handle markers drawing and updates
  useEffect(() => {
    if (!mapInstanceRef.current || !window.ymaps) return;

    const map = mapInstanceRef.current;
    map.geoObjects.removeAll();

    // Re-add user location marker if available
    if (userLocation) {
      const userPlacemark = new window.ymaps.Placemark(
        [userLocation.lat, userLocation.lng],
        {
          hintContent: "Ваше текущее местоположение",
          iconCaption: "Вы здесь",
          balloonContent: "Ваша геопозиция по GPS"
        },
        {
          preset: "islands#geolocationIcon",
          iconColor: "#0284c7"
        }
      );
      map.geoObjects.add(userPlacemark);
    }

    if (isAddingMode) {
      if (clickCoords) {
        const tempPlacemark = new window.ymaps.Placemark(
          [clickCoords.lat, clickCoords.lng],
          {
            hintContent: formName || "Новая точка",
            iconCaption: formName && formName !== "Определение адреса..." ? formName : "Новая точка",
            balloonContent: "Нажмите 'Сохранить точку' в панели внизу"
          },
          {
            preset: "islands#dotIcon",
            iconColor: "#10b981"
          }
        );
        map.geoObjects.add(tempPlacemark);
      }
    } else {
      territories.forEach((territory) => {
        const rating = getTerritoryRating(territory);
        const isSelected = selectedTerritory?.id === territory.id;

        // Determine official Yandex Maps color preset based on quality score
        let colorName = "gray";
        if (rating.score === 1) colorName = "green";
        else if (rating.score === 2) colorName = "yellow";
        else if (rating.score === 3) colorName = "orange";
        else if (rating.score === 4) colorName = "red";
        else if (rating.score === 5) colorName = "darkRed";

        const presetStr = isSelected 
          ? `islands#${colorName}CircleDotIcon` 
          : `islands#${colorName}Icon`;

        const placemark = new window.ymaps.Placemark(
          [territory.latitude, territory.longitude],
          {
            hintContent: territory.name,
            iconCaption: territory.name,
            balloonContentHeader: `<div class="font-bold text-slate-900 text-xs">${territory.name}</div>`,
            balloonContentBody: `<div class="text-[10px] text-slate-500 leading-relaxed">${territory.description || "Без описания"}</div>`
          },
          {
            preset: presetStr,
            iconColor: rating.hexColor
          }
        );

        placemark.events.add("click", () => {
          telegramHaptic("selection");
          setSelectedTerritory(territory);
        });

        map.geoObjects.add(placemark);
      });
    }
  }, [territories, isAddingMode, clickCoords, selectedTerritory, mapInitializedVal, formName, userLocation]);

  // Center/Pan Map on selected territory changes
  useEffect(() => {
    if (selectedTerritory && mapInstanceRef.current) {
      mapInstanceRef.current.setCenter([selectedTerritory.latitude, selectedTerritory.longitude], 13, {
        duration: 350
      });
    }
  }, [selectedTerritory]);

  // Request current GPS position
  const handleLocateUser = () => {
    telegramHaptic("medium");
    if (!navigator.geolocation) {
      alert("Геолокация не поддерживается вашим устройством");
      return;
    }

    setIsLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsLocating(false);
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        setUserLocation({ lat, lng });

        if (mapInstanceRef.current) {
          mapInstanceRef.current.setCenter([lat, lng], 14, { duration: 400 });
        }

        // If in adding mode, place pin at user's current GPS position directly
        if (isAddingModeRef.current) {
          handleMapClickInternal(lat, lng);
        }
      },
      (err) => {
        setIsLocating(false);
        console.warn("Geolocation error:", err);
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 30000 }
    );
  };

  return (
    <div className="flex-1 flex flex-col bg-slate-50 relative overflow-hidden h-full">
      
      {/* Top Header / Mode Control */}
      <div className="px-4 py-3 bg-white border-b border-slate-200/80 shadow-xs z-20 flex justify-between items-center shrink-0">
        <div>
          <h2 className="text-sm font-bold text-slate-900 tracking-tight flex items-center gap-1.5">
            <Compass size={16} className="text-emerald-500" />
            Экологическая карта
          </h2>
          <p className="text-[10px] text-slate-400">
            {territories.length > 0 
              ? `Отмечено точек: ${territories.length}` 
              : "Нажмите «+ Точка» для добавления"}
          </p>
        </div>
        
        <div className="flex items-center gap-2">
          {/* Add Point Mode Button */}
          {!isAddingMode ? (
            <>
              <button
                onClick={() => {
                  telegramHaptic("medium");
                  setIsSearchOpen(prev => {
                    const next = !prev;
                    if (next) {
                      setTimeout(() => {
                        const el = document.getElementById("address-search-input");
                        el?.focus();
                      }, 120);
                    }
                    return next;
                  });
                }}
                className={`px-2.5 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1.5 transition-all cursor-pointer active:scale-95 border ${
                  isSearchOpen
                    ? "bg-emerald-50 text-emerald-700 border-emerald-300 font-bold"
                    : "bg-slate-100 hover:bg-slate-200 text-slate-700 border-slate-200/80"
                }`}
                title="Поиск по адресу или журналу"
              >
                <Search size={14} className={isSearchOpen ? "text-emerald-600" : "text-slate-500"} />
                <span className="hidden sm:inline">Поиск</span>
              </button>
              <button
                onClick={() => {
                  telegramHaptic("medium");
                  setIsAddingMode(true);
                  setSelectedTerritory(null);
                  setClickCoords(null);
                  setTimeout(() => {
                    const el = document.getElementById("address-search-input");
                    el?.focus();
                  }, 120);
                }}
                className="px-3 py-1.5 rounded-xl text-xs font-bold flex items-center gap-1.5 transition-all bg-emerald-600 hover:bg-emerald-700 text-white shadow-xs cursor-pointer active:scale-95"
              >
                <Plus size={14} />
                <span>+ Точка</span>
              </button>
            </>
          ) : (
            <button
              onClick={() => {
                telegramHaptic("light");
                if (hasPendingMeasurement && isAddingMode) {
                  onCancelPendingMeasurement?.();
                }
                setIsAddingMode(false);
                setSelectedTerritory(null);
                setClickCoords(null);
                setSearchQuery("");
                setSearchResults([]);
                setShowResultsDropdown(false);
              }}
              className="px-3 py-1.5 rounded-xl text-xs font-semibold flex items-center gap-1 transition-all bg-slate-800 text-white cursor-pointer active:scale-95"
            >
              Отмена
              <Plus size={14} className="rotate-45" />
            </button>
          )}
        </div>
      </div>

      {/* Address Search Bar directly above the map */}
      {(isAddingMode || isSearchOpen) && (
        <div 
          ref={searchContainerRef}
          className="bg-white border-b border-slate-200 px-3 py-2 z-30 shrink-0 relative shadow-xs"
        >
          <div className="flex items-center gap-2">
            <div className="relative flex-1">
              <div className="absolute inset-y-0 left-0 pl-2.5 flex items-center pointer-events-none text-slate-400">
                {isSearching ? (
                  <Loader2 size={14} className="animate-spin text-emerald-600" />
                ) : (
                  <Search size={14} className="text-emerald-600" />
                )}
              </div>
              <input
                id="address-search-input"
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                onFocus={() => {
                  if (searchResults.length > 0) setShowResultsDropdown(true);
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    handleAddressSearch();
                  }
                }}
                placeholder={
                  isAddingMode 
                    ? "Поиск по адресу (город, улица, парк, координаты)..." 
                    : "Поиск по названию точки или адресу..."
                }
                autoComplete="off"
                className="w-full pl-8 pr-7 py-1.5 text-xs bg-slate-50 hover:bg-white focus:bg-white rounded-xl border border-slate-200 focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 text-slate-900 placeholder:text-slate-400 font-medium transition-all"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => {
                    setSearchQuery("");
                    setSearchResults([]);
                    setShowResultsDropdown(false);
                  }}
                  className="absolute inset-y-0 right-0 pr-2 flex items-center text-slate-400 hover:text-slate-600 cursor-pointer"
                  title="Очистить"
                >
                  <X size={14} />
                </button>
              )}
            </div>

            <button
              type="button"
              onClick={() => handleAddressSearch()}
              disabled={!searchQuery.trim() || isSearching}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1 shrink-0 ${
                searchQuery.trim() && !isSearching
                  ? "bg-emerald-600 hover:bg-emerald-700 text-white cursor-pointer active:scale-95 shadow-xs"
                  : "bg-slate-100 text-slate-400 pointer-events-none"
              }`}
            >
              {isSearching ? (
                <>
                  <Loader2 size={13} className="animate-spin" />
                  <span className="hidden sm:inline">Поиск...</span>
                </>
              ) : (
                <span>Найти</span>
              )}
            </button>
          </div>

          {/* Interactive Suggestions Dropdown list */}
          {showResultsDropdown && (
            <div className="absolute top-full left-2 right-2 mt-1 bg-white rounded-2xl shadow-2xl border border-slate-200/90 z-40 max-h-60 overflow-y-auto divide-y divide-slate-100 animate-fade-in">
              {searchResults.length > 0 ? (
                <>
                  <div className="px-3 py-1.5 bg-slate-50 text-[10px] font-semibold text-slate-400 uppercase tracking-wider flex justify-between items-center">
                    <span>Найдено: {searchResults.length}</span>
                    <span className="text-[9px] lowercase font-normal">
                      {isAddingMode ? "кликните для выбора метки" : "кликните для перехода"}
                    </span>
                  </div>
                  {searchResults.map((item, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => handleSelectSearchResult(item)}
                      className="w-full text-left px-3 py-2.5 hover:bg-emerald-50/80 active:bg-emerald-100 transition-colors flex items-start gap-2.5 cursor-pointer group"
                    >
                      {item.isExistingTerritory ? (
                        <div className="w-6 h-6 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center shrink-0 mt-0.5">
                          <Navigation size={13} className="text-emerald-700" />
                        </div>
                      ) : (
                        <div className="w-6 h-6 rounded-lg bg-sky-100 text-sky-700 flex items-center justify-center shrink-0 mt-0.5">
                          <MapPin size={13} className="text-sky-700 group-hover:scale-110 transition-transform" />
                        </div>
                      )}
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-1.5">
                          <p className="text-xs font-bold text-slate-900 group-hover:text-emerald-800 line-clamp-1">
                            {item.name}
                          </p>
                          {item.isExistingTerritory ? (
                            <span className="px-1.5 py-0.5 rounded text-[9px] font-bold bg-emerald-100 text-emerald-800 shrink-0">
                              В журнале
                            </span>
                          ) : (
                            <span className="px-1.5 py-0.5 rounded text-[9px] font-semibold bg-slate-100 text-slate-600 shrink-0">
                              Адрес
                            </span>
                          )}
                        </div>
                        <p className="text-[10px] text-slate-500 line-clamp-1 mt-0.5">
                          {item.fullAddress || item.description}
                        </p>
                      </div>
                    </button>
                  ))}
                </>
              ) : (
                !isSearching && (
                  <div className="p-4 text-center">
                    <p className="text-xs font-semibold text-slate-700">Ничего не найдено</p>
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      Попробуйте уточнить запрос (город, улица, координаты) или кликните прямо на карте
                    </p>
                  </div>
                )
              )}
            </div>
          )}
        </div>
      )}

      {/* Adding instruction alert banner */}
      {isAddingMode && !clickCoords && (
        <div className={`${hasPendingMeasurement ? "bg-amber-50 text-amber-900 border-amber-200" : "bg-emerald-50 text-emerald-800 border-emerald-100"} border-b px-3 py-1.5 text-[11px] font-semibold text-center z-10 shrink-0 leading-relaxed shadow-xs`}>
          {hasPendingMeasurement ? (
            <div className="flex flex-col gap-0.5 items-center justify-center">
              <span className="font-extrabold flex items-center gap-1 text-amber-700">⚠️ Нет сигнала GPS / Разместите вручную</span>
              <span>Введите адрес выше или кликните по карте в месте сбора листьев</span>
            </div>
          ) : (
            "📍 Найдите адрес выше или кликните по карте для установки контрольной точки"
          )}
        </div>
      )}

      {/* Interactive Map Container - Occupies 100% of the remaining space */}
      <div id="yandex-map-stage" className="flex-1 w-full h-full relative select-none overflow-hidden">
        <div className="w-full h-full bg-slate-100 overflow-hidden relative">
          
          {/* Custom Loader Overlay */}
          {!isLoaded && !loadError && (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-slate-50 text-slate-400 gap-2 z-10">
              <Compass size={32} className="text-emerald-500 animate-spin" />
              <span className="text-xs font-semibold text-slate-500 font-sans">Инициализация карты...</span>
            </div>
          )}

          {/* Load Error / Fallback View */}
          {loadError && (
            <div className="absolute inset-0 flex flex-col items-center justify-center bg-white p-6 text-center gap-3 z-10">
              <AlertTriangle size={36} className="text-amber-500" />
              <span className="text-sm font-bold text-slate-800">Карты работают в локальном режиме</span>
              <p className="text-xs text-slate-500 max-w-[260px] leading-relaxed">
                Вы можете продолжить обследования. Координаты фиксируются через GPS вашего смартфона.
              </p>
              <button
                onClick={() => window.location.reload()}
                className="px-4 py-2 bg-emerald-600 text-white rounded-xl text-xs font-bold flex items-center gap-1.5 shadow-sm active:scale-95"
              >
                <RefreshCw size={14} />
                <span>Обновить страницу</span>
              </button>
            </div>
          )}

          {/* Actual DOM target for Yandex Maps */}
          <div 
            ref={mapRef} 
            className="w-full h-full" 
            style={{ 
              display: isLoaded && !loadError ? "block" : "none",
              touchAction: "pan-x pan-y"
            }} 
          />

        </div>

        {/* Floating GPS Geolocation Button */}
        {isLoaded && !loadError && (
          <button
            onClick={handleLocateUser}
            disabled={isLocating}
            className="absolute top-3 right-3 z-30 p-2.5 bg-white/95 backdrop-blur-md rounded-2xl shadow-lg border border-slate-200/80 text-slate-700 hover:text-emerald-600 active:scale-90 transition-all cursor-pointer"
            title="Определить мое местоположение по GPS"
          >
            <Locate size={18} className={isLocating ? "animate-spin text-emerald-600" : ""} />
          </button>
        )}
      </div>

      {/* Floating Add Node Form Panel */}
      {isAddingMode && clickCoords && (
        <div className="absolute bottom-2 left-2 right-2 bg-white/95 backdrop-blur-md p-4 rounded-2xl border border-slate-200/80 shadow-2xl z-30 animate-fade-in flex flex-col gap-3">
          <div className="flex justify-between items-center">
            <span className="text-xs font-bold text-slate-900 flex items-center gap-1">
              📍 Новая точка обследования
            </span>
            <span className="text-[10px] font-mono text-slate-500 font-semibold">
              {clickCoords.lat.toFixed(4)}°, {clickCoords.lng.toFixed(4)}°
            </span>
          </div>

          <div className="space-y-2">
            <input
              type="text"
              placeholder="Название точки (напр. Сквер, Парк, Улица)..."
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              onFocus={(e) => {
                if (formName.trim().toLowerCase() === "определение адреса...") {
                  setFormName("");
                } else {
                  e.target.select();
                }
              }}
              className="w-full text-xs px-3 py-2 rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500 text-slate-900 font-medium placeholder-slate-400"
              maxLength={50}
              autoFocus
            />
            <input
              type="text"
              placeholder="Ориентир (напр. 50м от дороги, лесополоса)..."
              value={formDesc}
              onChange={(e) => setFormDesc(e.target.value)}
              className="w-full text-xs px-3 py-2 rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-emerald-500 text-slate-900 placeholder-slate-400"
              maxLength={100}
            />
          </div>

          <div className="flex gap-2">
            <button
              onClick={() => setClickCoords(null)}
              className="flex-1 py-2 text-xs font-semibold text-slate-600 bg-slate-100 rounded-xl hover:bg-slate-200 cursor-pointer active:scale-95 transition-all"
            >
              Сброс
            </button>
            <button
              onClick={handleSaveLocation}
              disabled={!formName.trim()}
              className={`flex-1 py-2 text-xs font-bold rounded-xl text-white transition-all active:scale-95 ${
                formName.trim() ? "bg-emerald-600 hover:bg-emerald-700 cursor-pointer shadow-sm" : "bg-slate-300 pointer-events-none"
              }`}
            >
              Сохранить точку
            </button>
          </div>
        </div>
      )}

      {/* Detail Overlay of Selected Pin */}
      {selectedTerritory && !isAddingMode && (
        <div className="absolute bottom-2 left-2 right-2 bg-white p-4 rounded-2xl border border-slate-200 shadow-2xl z-30 animate-fade-in flex flex-col gap-3">
          
          <div className="flex justify-between items-start">
            <div className="space-y-0.5 max-w-[70%]">
              <h3 className="font-bold text-slate-950 text-sm line-clamp-1">{selectedTerritory.name}</h3>
              <p className="text-[11px] text-slate-500 line-clamp-2 leading-relaxed">{selectedTerritory.description || "Без описания"}</p>
            </div>
            
            {/* Color Status Indicator */}
            {(() => {
              const rating = getTerritoryRating(selectedTerritory);
              return (
                <div className="flex flex-col items-end gap-1 shrink-0">
                  <span className={`text-[10px] uppercase tracking-wider font-extrabold px-2 py-0.5 rounded-full ${rating.color.split(" ")[0]} text-white`}>
                    Балл {rating.score || "—"}
                  </span>
                  <span className="text-[10px] font-mono text-slate-400 font-semibold">{rating.status}</span>
                </div>
              );
            })()}
          </div>

          {/* Metric Details Stats */}
          <div className="grid grid-cols-3 gap-2 bg-slate-50 p-2 rounded-xl text-center border border-slate-100">
            <div className="flex flex-col items-center">
              <span className="text-[9px] text-slate-400 font-semibold font-mono">ЛИСТЬЕВ</span>
              <span className="text-sm font-extrabold text-slate-800">{selectedTerritory.leaves.length}</span>
            </div>
            <div className="flex flex-col items-center border-x border-slate-200">
              <span className="text-[9px] text-slate-400 font-semibold font-mono">ПОКАЗАТЕЛЬ FA</span>
              <span className="text-sm font-extrabold text-slate-800">
                {selectedTerritory.leaves.length > 0
                  ? (selectedTerritory.leaves.reduce((sum, leaf) => sum + leaf.zScore, 0) / selectedTerritory.leaves.length).toFixed(4)
                  : "—"}
              </span>
            </div>
            <div className="flex flex-col items-center">
              <span className="text-[9px] text-slate-400 font-semibold font-mono">КООРДИНАТЫ</span>
              <span className="text-[10px] font-bold text-slate-600 font-mono mt-0.5 leading-tight">
                {selectedTerritory.latitude.toFixed(3)}°<br />
                {selectedTerritory.longitude.toFixed(3)}°
              </span>
            </div>
          </div>

          {/* Actions */}
          <div className="flex gap-2">
            <button
              onClick={() => setSelectedTerritory(null)}
              className="flex-1 py-2 text-xs font-semibold text-slate-600 border border-slate-200 rounded-xl hover:bg-slate-50 cursor-pointer active:scale-95"
            >
              Закрыть
            </button>
            <button
              onClick={() => {
                telegramHaptic("medium");
                onSelectTerritory(selectedTerritory.id);
              }}
              className="flex-1 py-2 text-xs font-bold text-white bg-emerald-600 rounded-xl hover:bg-emerald-700 flex items-center justify-center gap-1 cursor-pointer shadow-sm active:scale-95"
            >
              В журнал
              <Navigation size={12} />
            </button>
          </div>

        </div>
      )}

      {/* Map Stats Quick Legend */}
      {!selectedTerritory && !isAddingMode && (
        <div className="absolute bottom-3 left-3 bg-white/95 backdrop-blur-md p-2.5 rounded-2xl border border-slate-200/80 shadow-md pointer-events-none text-[9px] text-slate-600 font-semibold space-y-1 z-10 max-w-[210px]">
          <p className="font-bold text-slate-800 text-[10px] mb-1 pb-0.5 border-b border-slate-100 flex items-center gap-1">
            <Info size={11} className="text-emerald-600" />
            Баллы качества среды
          </p>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-emerald-500 shrink-0" />
            <span>1 — Норма (&le; 0.055)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-yellow-400 shrink-0" />
            <span>2 — Начальное (&le; 0.060)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-amber-500 shrink-0" />
            <span>3 — Среднее (&le; 0.065)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-rose-500 shrink-0" />
            <span>4 — Значительное (&le; 0.070)</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-red-800 shrink-0" />
            <span>5 — Критическое (&gt; 0.070)</span>
          </div>
        </div>
      )}

    </div>
  );
}
