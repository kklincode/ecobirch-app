import React, { useState, useMemo } from "react";
import { Territory, LeafAnalysis } from "../types";
import { getEnvironmentalRating } from "../utils";
import {
  ResponsiveContainer,
  AreaChart,
  Area,
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ReferenceLine,
  Legend,
} from "recharts";
import {
  TrendingDown,
  TrendingUp,
  Minus,
  Calendar,
  Layers,
  Leaf,
  Info,
  CheckCircle2,
  AlertTriangle,
  Flame,
  BarChart2,
  Activity,
  Sliders,
  ChevronRight,
} from "lucide-react";

interface FAWeeklyChartProps {
  territories: Territory[];
  onSelectTerritoryForScan?: (territoryId: string) => void;
  onLoadDemoData?: () => void;
}

interface DayDataPoint {
  dateKey: string; // YYYY-MM-DD
  dayLabel: string; // "03.10" or "Сегодня"
  weekday: string; // "Пн", "Вт", etc.
  fullDate: string; // "3 октября"
  timestamp: number;
  count: number;
  avgFA: number | null;
  minFA: number | null;
  maxFA: number | null;
  t1: number | null;
  t2: number | null;
  t3: number | null;
  t4: number | null;
  t5: number | null;
  leaves: { leaf: LeafAnalysis; territoryName: string }[];
  score: number | null;
  status: string | null;
  hexColor: string | null;
}

const RUS_MONTHS = [
  "янв", "фев", "мар", "апр", "май", "июн",
  "июл", "авг", "сен", "окт", "ноя", "дек"
];

const RUS_MONTHS_FULL = [
  "января", "февраля", "марта", "апреля", "мая", "июня",
  "июля", "августа", "сентября", "октября", "ноября", "декабря"
];

const RUS_WEEKDAYS = ["Вс", "Пн", "Вт", "Ср", "Чт", "Пт", "Сб"];

export default function FAWeeklyChart({
  territories,
  onSelectTerritoryForScan,
  onLoadDemoData,
}: FAWeeklyChartProps) {
  const [selectedTerritoryId, setSelectedTerritoryId] = useState<string>("all");
  const [chartMode, setChartMode] = useState<"fa" | "traits" | "volume">("fa");
  const [selectedDayKey, setSelectedDayKey] = useState<string | null>(null);

  // Filter leaves based on selected territory
  const activeLeavesWithTerritory = useMemo(() => {
    const list: { leaf: LeafAnalysis; territoryName: string; territoryId: string }[] = [];
    territories.forEach((t) => {
      if (selectedTerritoryId === "all" || t.id === selectedTerritoryId) {
        t.leaves.forEach((l) => {
          list.push({ leaf: l, territoryName: t.name, territoryId: t.id });
        });
      }
    });
    return list;
  }, [territories, selectedTerritoryId]);

  // Generate 7-day data sequence from 6 days ago up to today
  const weeklyData: DayDataPoint[] = useMemo(() => {
    const points: DayDataPoint[] = [];
    const now = new Date();

    for (let i = 6; i >= 0; i--) {
      const d = new Date(now.getTime() - i * 86400000);
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, "0");
      const dayNum = String(d.getDate()).padStart(2, "0");
      const dateKey = `${y}-${m}-${dayNum}`;

      const isToday = i === 0;
      const dayLabel = isToday ? "Сегодня" : `${dayNum}.${m}`;
      const weekday = RUS_WEEKDAYS[d.getDay()];
      const fullDate = `${d.getDate()} ${RUS_MONTHS_FULL[d.getMonth()]}`;

      // Filter leaves matching this calendar day
      const dayLeaves = activeLeavesWithTerritory.filter((item) => {
        const leafDate = new Date(item.leaf.timestamp);
        const ly = leafDate.getFullYear();
        const lm = String(leafDate.getMonth() + 1).padStart(2, "0");
        const ld = String(leafDate.getDate()).padStart(2, "0");
        return `${ly}-${lm}-${ld}` === dateKey;
      });

      const count = dayLeaves.length;

      if (count > 0) {
        const sumZ = dayLeaves.reduce((acc, curr) => acc + curr.leaf.zScore, 0);
        const avgFA = parseFloat((sumZ / count).toFixed(4));
        const zScores = dayLeaves.map((l) => l.leaf.zScore);
        const minFA = parseFloat(Math.min(...zScores).toFixed(4));
        const maxFA = parseFloat(Math.max(...zScores).toFixed(4));

        const avgT1 = parseFloat((dayLeaves.reduce((acc, c) => acc + c.leaf.traits.t1Width.yDiff, 0) / count).toFixed(4));
        const avgT2 = parseFloat((dayLeaves.reduce((acc, c) => acc + c.leaf.traits.t2VeinLen.yDiff, 0) / count).toFixed(4));
        const avgT3 = parseFloat((dayLeaves.reduce((acc, c) => acc + c.leaf.traits.t3VeinDist.yDiff, 0) / count).toFixed(4));
        const avgT4 = parseFloat((dayLeaves.reduce((acc, c) => acc + c.leaf.traits.t4EndDist.yDiff, 0) / count).toFixed(4));
        const avgT5 = parseFloat((dayLeaves.reduce((acc, c) => acc + c.leaf.traits.t5Angle.yDiff, 0) / count).toFixed(4));

        const rating = getEnvironmentalRating(avgFA);

        points.push({
          dateKey,
          dayLabel,
          weekday,
          fullDate,
          timestamp: d.getTime(),
          count,
          avgFA,
          minFA,
          maxFA,
          t1: avgT1,
          t2: avgT2,
          t3: avgT3,
          t4: avgT4,
          t5: avgT5,
          leaves: dayLeaves.map((dl) => ({ leaf: dl.leaf, territoryName: dl.territoryName })),
          score: rating.score,
          status: rating.status,
          hexColor: rating.hexColor,
        });
      } else {
        points.push({
          dateKey,
          dayLabel,
          weekday,
          fullDate,
          timestamp: d.getTime(),
          count: 0,
          avgFA: null,
          minFA: null,
          maxFA: null,
          t1: null,
          t2: null,
          t3: null,
          t4: null,
          t5: null,
          leaves: [],
          score: null,
          status: null,
          hexColor: null,
        });
      }
    }

    return points;
  }, [activeLeavesWithTerritory]);

  // Summary metrics for the 7 days
  const summaryMetrics = useMemo(() => {
    const daysWithData = weeklyData.filter((d) => d.avgFA !== null);
    const totalLeaves = weeklyData.reduce((acc, d) => acc + d.count, 0);

    if (daysWithData.length === 0) {
      return {
        overallAvgFA: null,
        totalLeaves: 0,
        daysMeasured: 0,
        trend: null,
        trendDiff: 0,
        rating: null,
      };
    }

    const totalZ = daysWithData.reduce((acc, d) => acc + (d.avgFA! * d.count), 0);
    const overallAvgFA = parseFloat((totalZ / totalLeaves).toFixed(4));
    const rating = getEnvironmentalRating(overallAvgFA);

    // Trend calculation: compare first available day vs last available day
    let trend: "improved" | "worsened" | "stable" | null = null;
    let trendDiff = 0;
    if (daysWithData.length >= 2) {
      const first = daysWithData[0].avgFA!;
      const last = daysWithData[daysWithData.length - 1].avgFA!;
      trendDiff = parseFloat((last - first).toFixed(4));
      if (Math.abs(trendDiff) < 0.0015) {
        trend = "stable";
      } else if (trendDiff < 0) {
        // Lower FA = more symmetrical = improvement!
        trend = "improved";
      } else {
        // Higher FA = higher asymmetry = deterioration
        trend = "worsened";
      }
    }

    return {
      overallAvgFA,
      totalLeaves,
      daysMeasured: daysWithData.length,
      trend,
      trendDiff,
      rating,
    };
  }, [weeklyData]);

  // Selected day data for detailed card
  const selectedDay = useMemo(() => {
    if (!selectedDayKey) {
      // Default to today or the latest day with data
      const latest = [...weeklyData].reverse().find((d) => d.count > 0);
      return latest || weeklyData[weeklyData.length - 1];
    }
    return weeklyData.find((d) => d.dateKey === selectedDayKey) || weeklyData[weeklyData.length - 1];
  }, [selectedDayKey, weeklyData]);

  // Recharts custom tooltip
  const CustomTooltip = ({ active, payload, label }: any) => {
    if (!active || !payload || !payload.length) return null;
    const data: DayDataPoint = payload[0]?.payload;
    if (!data) return null;

    if (data.avgFA === null) {
      return (
        <div className="bg-slate-900/95 backdrop-blur-md text-white p-2.5 rounded-xl text-xs shadow-xl border border-slate-700/60 font-sans">
          <p className="font-bold text-slate-300">{data.weekday}, {data.fullDate}</p>
          <p className="text-[11px] text-slate-400 mt-0.5">Нет замеров за этот день</p>
        </div>
      );
    }

    const rating = getEnvironmentalRating(data.avgFA);

    return (
      <div className="bg-slate-900/95 backdrop-blur-md text-white p-3 rounded-xl text-xs shadow-xl border border-slate-700/60 font-sans min-w-[210px]">
        <div className="flex items-center justify-between border-b border-slate-800 pb-1.5 mb-2">
          <span className="font-bold text-slate-200">
            {data.weekday}, {data.fullDate}
          </span>
          <span
            className="text-[10px] font-extrabold px-1.5 py-0.5 rounded"
            style={{ backgroundColor: rating.hexColor, color: rating.score === 2 ? "#0f172a" : "#ffffff" }}
          >
            Балл {rating.score}
          </span>
        </div>

        <div className="space-y-1">
          <div className="flex justify-between items-center">
            <span className="text-slate-400">Индекс ФА:</span>
            <span className="font-mono font-bold text-emerald-400 text-sm">
              {data.avgFA.toFixed(4)}
            </span>
          </div>

          <div className="flex justify-between items-center text-[11px]">
            <span className="text-slate-400">Качество среды:</span>
            <span className="font-medium text-slate-200 text-right max-w-[120px] truncate">
              {rating.status}
            </span>
          </div>

          <div className="flex justify-between items-center text-[11px]">
            <span className="text-slate-400">Исследовано листьев:</span>
            <span className="font-mono text-slate-200">{data.count} шт.</span>
          </div>

          {data.minFA !== null && data.maxFA !== null && data.count > 1 && (
            <div className="flex justify-between items-center text-[10px] text-slate-400 pt-1 border-t border-slate-800">
              <span>Диапазон:</span>
              <span className="font-mono">
                {data.minFA.toFixed(4)} – {data.maxFA.toFixed(4)}
              </span>
            </div>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-4">
      {/* Territory Filter & View Mode Bar */}
      <div className="flex flex-col gap-2.5 bg-white p-3.5 rounded-2xl border border-slate-100 shadow-2xs">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-1.5 text-xs font-bold text-slate-700">
            <Layers className="w-4 h-4 text-emerald-600" />
            <span>Локация:</span>
          </div>

          {/* Territory Select Dropdown */}
          <select
            value={selectedTerritoryId}
            onChange={(e) => setSelectedTerritoryId(e.target.value)}
            className="text-xs bg-slate-50 border border-slate-200 rounded-xl px-2.5 py-1.5 font-medium text-slate-800 focus:outline-none focus:ring-2 focus:ring-emerald-500/20 max-w-[200px] truncate"
          >
            <option value="all">Все территории ({territories.length})</option>
            {territories.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name} ({t.leaves.length} лист.)
              </option>
            ))}
          </select>
        </div>

        {/* Chart View Switcher */}
        <div className="grid grid-cols-3 gap-1 bg-slate-100 p-1 rounded-xl text-[11px] font-semibold">
          <button
            onClick={() => setChartMode("fa")}
            className={`py-1.5 px-2 rounded-lg transition-all flex items-center justify-center gap-1 ${
              chartMode === "fa"
                ? "bg-white text-emerald-700 shadow-xs font-bold"
                : "text-slate-500 hover:text-slate-800"
            }`}
          >
            <Activity className="w-3.5 h-3.5" />
            Индекс ФА
          </button>
          <button
            onClick={() => setChartMode("traits")}
            className={`py-1.5 px-2 rounded-lg transition-all flex items-center justify-center gap-1 ${
              chartMode === "traits"
                ? "bg-white text-emerald-700 shadow-xs font-bold"
                : "text-slate-500 hover:text-slate-800"
            }`}
          >
            <Sliders className="w-3.5 h-3.5" />
            5 признаков
          </button>
          <button
            onClick={() => setChartMode("volume")}
            className={`py-1.5 px-2 rounded-lg transition-all flex items-center justify-center gap-1 ${
              chartMode === "volume"
                ? "bg-white text-emerald-700 shadow-xs font-bold"
                : "text-slate-500 hover:text-slate-800"
            }`}
          >
            <BarChart2 className="w-3.5 h-3.5" />
            Замеры
          </button>
        </div>
      </div>

      {/* 7-Day KPI Metrics Summary Cards */}
      <div className="grid grid-cols-2 gap-2.5">
        {/* Metric 1: 7-day Avg FA */}
        <div className="bg-white p-3.5 rounded-2xl border border-slate-150 shadow-2xs">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-tight block mb-1">
            Средний ФА (7 дней)
          </span>
          <div className="flex items-baseline gap-2">
            <span className="text-xl font-black font-mono tracking-tight text-slate-900">
              {summaryMetrics.overallAvgFA !== null ? summaryMetrics.overallAvgFA.toFixed(4) : "—"}
            </span>
            {summaryMetrics.rating && (
              <span
                className="text-[10px] font-extrabold px-1.5 py-0.5 rounded-md"
                style={{
                  backgroundColor: summaryMetrics.rating.hexColor,
                  color: summaryMetrics.rating.score === 2 ? "#0f172a" : "#ffffff",
                }}
              >
                Балл {summaryMetrics.rating.score}
              </span>
            )}
          </div>
          <p className="text-[10px] text-slate-500 mt-1 truncate">
            {summaryMetrics.rating ? summaryMetrics.rating.status : "Нет данных за период"}
          </p>
        </div>

        {/* Metric 2: Trend & Volume */}
        <div className="bg-white p-3.5 rounded-2xl border border-slate-150 shadow-2xs">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-tight block mb-1">
            Тренд и замеры
          </span>
          <div className="flex items-center gap-2">
            {summaryMetrics.trend === "improved" && (
              <div className="flex items-center gap-1 text-emerald-600 font-extrabold text-sm">
                <TrendingDown className="w-4 h-4" />
                <span>Улучшение</span>
              </div>
            )}
            {summaryMetrics.trend === "worsened" && (
              <div className="flex items-center gap-1 text-rose-600 font-extrabold text-sm">
                <TrendingUp className="w-4 h-4" />
                <span>Рост стресса</span>
              </div>
            )}
            {summaryMetrics.trend === "stable" && (
              <div className="flex items-center gap-1 text-slate-600 font-extrabold text-sm">
                <Minus className="w-4 h-4" />
                <span>Стабильно</span>
              </div>
            )}
            {!summaryMetrics.trend && (
              <span className="text-sm font-bold text-slate-400">—</span>
            )}
          </div>
          <p className="text-[10px] text-slate-500 mt-1">
            Исследовано: <span className="font-bold text-slate-700">{summaryMetrics.totalLeaves}</span> лист. за {summaryMetrics.daysMeasured} дн.
          </p>
        </div>
      </div>

      {/* Main Recharts Container Card */}
      <div className="bg-white p-4 rounded-2xl border border-slate-150 shadow-2xs space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-bold text-slate-900 text-sm flex items-center gap-1.5">
              <Calendar className="w-4 h-4 text-emerald-600" />
              {chartMode === "fa" && "Динамика флуктуирующей асимметрии"}
              {chartMode === "traits" && "Морфометрические признаки (Y₁ – Y₅)"}
              {chartMode === "volume" && "Объем собранных образцов по дням"}
            </h3>
            <p className="text-[10px] text-slate-400 font-medium">
              Период: последние 7 календарных дней
            </p>
          </div>
          <span className="text-[10px] font-mono text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded-md font-bold">
            Recharts
          </span>
        </div>

        {/* Empty state if 0 leaves in 7 days */}
        {summaryMetrics.totalLeaves === 0 ? (
          <div className="h-56 flex flex-col items-center justify-center text-center p-4 bg-slate-50 rounded-xl border border-dashed border-slate-200">
            <Leaf className="w-10 h-10 text-slate-300 mb-2" />
            <p className="text-xs font-bold text-slate-700">Нет замеров за последние 7 дней</p>
            <p className="text-[11px] text-slate-400 max-w-[220px] mt-0.5 mb-3 leading-relaxed">
              Отсканируйте листья березы или загрузите демонстрационный набор данных.
            </p>
            {onLoadDemoData && (
              <button
                onClick={onLoadDemoData}
                className="bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs py-1.5 px-3 rounded-lg shadow-xs transition-all active:scale-95"
              >
                Загрузить демо-замеры
              </button>
            )}
          </div>
        ) : (
          <div className="w-full h-60 pt-2 select-none">
            <ResponsiveContainer width="100%" height="100%">
              {chartMode === "fa" ? (
                /* VIEW 1: AreaChart for Integral FA Score */
                <AreaChart
                  data={weeklyData}
                  margin={{ top: 12, right: 10, left: -22, bottom: 0 }}
                  onClick={(e: any) => {
                    if (e && e.activePayload && e.activePayload[0]) {
                      setSelectedDayKey(e.activePayload[0].payload.dateKey);
                    }
                  }}
                >
                  <defs>
                    <linearGradient id="faGradient" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#10b981" stopOpacity={0.4} />
                      <stop offset="95%" stopColor="#10b981" stopOpacity={0.02} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis
                    dataKey="dayLabel"
                    tick={{ fontSize: 10, fill: "#64748b" }}
                    axisLine={{ stroke: "#e2e8f0" }}
                    tickLine={false}
                  />
                  <YAxis
                    domain={[0.03, 0.085]}
                    tick={{ fontSize: 9, fill: "#64748b", fontFamily: "monospace" }}
                    axisLine={{ stroke: "#e2e8f0" }}
                    tickLine={false}
                    tickFormatter={(val) => val.toFixed(2)}
                  />
                  <Tooltip content={<CustomTooltip />} />

                  {/* Ecological Reference Thresholds (Захаров и др.) */}
                  <ReferenceLine
                    y={0.055}
                    stroke="#10b981"
                    strokeDasharray="3 3"
                    strokeWidth={1.5}
                    label={{
                      value: "Норма (0.055)",
                      position: "insideTopRight",
                      fill: "#059669",
                      fontSize: 8,
                      fontWeight: 700,
                    }}
                  />
                  <ReferenceLine
                    y={0.07}
                    stroke="#e11d48"
                    strokeDasharray="3 3"
                    strokeWidth={1.5}
                    label={{
                      value: "Критич. (0.070)",
                      position: "insideTopRight",
                      fill: "#e11d48",
                      fontSize: 8,
                      fontWeight: 700,
                    }}
                  />

                  <Area
                    type="monotone"
                    dataKey="avgFA"
                    name="Средний ФА"
                    stroke="#059669"
                    strokeWidth={2.5}
                    fillOpacity={1}
                    fill="url(#faGradient)"
                    connectNulls
                    dot={(props: any) => {
                      const { cx, cy, payload } = props;
                      if (!payload || payload.avgFA === null) return <circle key={`empty-${cx}-${cy}`} cx={cx} cy={cy} r={0} />;
                      const isSelected = selectedDay?.dateKey === payload.dateKey;
                      const fillColor = payload.hexColor || "#10b981";
                      return (
                        <circle
                          key={`dot-${payload.dateKey}`}
                          cx={cx}
                          cy={cy}
                          r={isSelected ? 6 : 4}
                          fill={fillColor}
                          stroke="#ffffff"
                          strokeWidth={isSelected ? 2.5 : 1.5}
                          className="cursor-pointer transition-all"
                        />
                      );
                    }}
                    activeDot={{ r: 7, stroke: "#047857", strokeWidth: 2 }}
                  />
                </AreaChart>
              ) : chartMode === "traits" ? (
                /* VIEW 2: LineChart for 5 Morphometric Traits */
                <LineChart
                  data={weeklyData}
                  margin={{ top: 12, right: 10, left: -22, bottom: 0 }}
                  onClick={(e: any) => {
                    if (e && e.activePayload && e.activePayload[0]) {
                      setSelectedDayKey(e.activePayload[0].payload.dateKey);
                    }
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis
                    dataKey="dayLabel"
                    tick={{ fontSize: 10, fill: "#64748b" }}
                    axisLine={{ stroke: "#e2e8f0" }}
                    tickLine={false}
                  />
                  <YAxis
                    domain={[0.01, 0.1]}
                    tick={{ fontSize: 9, fill: "#64748b", fontFamily: "monospace" }}
                    axisLine={{ stroke: "#e2e8f0" }}
                    tickLine={false}
                    tickFormatter={(val) => val.toFixed(2)}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Legend
                    verticalAlign="bottom"
                    iconSize={8}
                    wrapperStyle={{ fontSize: "9px", paddingTop: "6px" }}
                    formatter={(val) => (
                      <span className="text-slate-600 font-medium">{val}</span>
                    )}
                  />

                  <Line type="monotone" dataKey="t1" name="Y₁ Ширина" stroke="#0284c7" strokeWidth={1.8} dot={{ r: 2 }} connectNulls />
                  <Line type="monotone" dataKey="t2" name="Y₂ Длина ж." stroke="#10b981" strokeWidth={1.8} dot={{ r: 2 }} connectNulls />
                  <Line type="monotone" dataKey="t3" name="Y₃ Расст. осн." stroke="#f59e0b" strokeWidth={1.8} dot={{ r: 2 }} connectNulls />
                  <Line type="monotone" dataKey="t4" name="Y₄ Расст. кон." stroke="#8b5cf6" strokeWidth={1.8} dot={{ r: 2 }} connectNulls />
                  <Line type="monotone" dataKey="t5" name="Y₅ Угол" stroke="#ec4899" strokeWidth={1.8} dot={{ r: 2 }} connectNulls />
                </LineChart>
              ) : (
                /* VIEW 3: BarChart for Measurement Sample Count */
                <BarChart
                  data={weeklyData}
                  margin={{ top: 12, right: 10, left: -22, bottom: 0 }}
                  onClick={(e: any) => {
                    if (e && e.activePayload && e.activePayload[0]) {
                      setSelectedDayKey(e.activePayload[0].payload.dateKey);
                    }
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                  <XAxis
                    dataKey="dayLabel"
                    tick={{ fontSize: 10, fill: "#64748b" }}
                    axisLine={{ stroke: "#e2e8f0" }}
                    tickLine={false}
                  />
                  <YAxis
                    allowDecimals={false}
                    tick={{ fontSize: 9, fill: "#64748b", fontFamily: "monospace" }}
                    axisLine={{ stroke: "#e2e8f0" }}
                    tickLine={false}
                  />
                  <Tooltip content={<CustomTooltip />} />
                  <Bar
                    dataKey="count"
                    name="Листьев"
                    fill="#10b981"
                    radius={[6, 6, 0, 0]}
                    maxBarSize={36}
                  />
                </BarChart>
              )}
            </ResponsiveContainer>
          </div>
        )}

        {/* Zakharov Ecological Scale Indicator */}
        <div className="pt-2 border-t border-slate-100">
          <div className="flex items-center justify-between text-[10px] text-slate-500 font-bold mb-1.5">
            <span>Шкала оценки (Захаров В.М.):</span>
            <span className="font-mono text-slate-400">FA: 0.055 – 0.070+</span>
          </div>
          <div className="grid grid-cols-5 gap-1 text-[9px] text-center font-bold">
            <div className="bg-emerald-50 text-emerald-700 border border-emerald-200 py-1 rounded-md">
              I ≤0.055
            </div>
            <div className="bg-yellow-50 text-yellow-700 border border-yellow-200 py-1 rounded-md">
              II 0.060
            </div>
            <div className="bg-amber-50 text-amber-700 border border-amber-200 py-1 rounded-md">
              III 0.065
            </div>
            <div className="bg-rose-50 text-rose-700 border border-rose-200 py-1 rounded-md">
              IV 0.070
            </div>
            <div className="bg-red-50 text-red-800 border border-red-200 py-1 rounded-md">
              V &gt;0.070
            </div>
          </div>
        </div>
      </div>

      {/* Selected Day Drill-down Card */}
      {selectedDay && (
        <div className="bg-white p-4 rounded-2xl border border-slate-150 shadow-2xs space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-tight">
                Детализация замера
              </span>
              <h4 className="font-bold text-slate-900 text-sm">
                {selectedDay.weekday}, {selectedDay.fullDate}
              </h4>
            </div>

            {selectedDay.score !== null ? (
              <span
                className="text-xs font-extrabold px-2.5 py-1 rounded-lg shadow-2xs"
                style={{
                  backgroundColor: selectedDay.hexColor || "#10b981",
                  color: selectedDay.score === 2 ? "#0f172a" : "#ffffff",
                }}
              >
                Балл {selectedDay.score} • {selectedDay.status}
              </span>
            ) : (
              <span className="text-[11px] font-semibold text-slate-400 bg-slate-100 px-2.5 py-1 rounded-lg">
                Нет замеров
              </span>
            )}
          </div>

          {selectedDay.count > 0 ? (
            <div className="space-y-2.5 text-xs">
              <div className="grid grid-cols-3 gap-2 bg-slate-50 p-2.5 rounded-xl border border-slate-100 text-center font-mono">
                <div>
                  <span className="text-[9px] text-slate-400 uppercase font-sans font-bold block">
                    Среднее FA
                  </span>
                  <span className="text-sm font-bold text-slate-800">
                    {selectedDay.avgFA?.toFixed(4)}
                  </span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-400 uppercase font-sans font-bold block">
                    Образцов
                  </span>
                  <span className="text-sm font-bold text-slate-800">
                    {selectedDay.count} шт.
                  </span>
                </div>
                <div>
                  <span className="text-[9px] text-slate-400 uppercase font-sans font-bold block">
                    Разброс
                  </span>
                  <span className="text-sm font-bold text-slate-800">
                    {selectedDay.minFA && selectedDay.maxFA
                      ? `${(selectedDay.maxFA - selectedDay.minFA).toFixed(3)}`
                      : "0.000"}
                  </span>
                </div>
              </div>

              {/* 5 traits mini breakdown for selected day */}
              {selectedDay.t1 !== null && (
                <div className="space-y-1.5 pt-1">
                  <span className="text-[10px] font-bold text-slate-500 uppercase tracking-tight block">
                    Асимметрия 5 признаков:
                  </span>
                  <div className="grid grid-cols-5 gap-1 text-[10px] font-mono text-center">
                    <div className="bg-sky-50 border border-sky-100 p-1.5 rounded-lg">
                      <span className="block text-[8px] text-sky-700 font-sans font-bold">Y₁ Ширина</span>
                      <span className="font-bold text-sky-900">{selectedDay.t1.toFixed(3)}</span>
                    </div>
                    <div className="bg-emerald-50 border border-emerald-100 p-1.5 rounded-lg">
                      <span className="block text-[8px] text-emerald-700 font-sans font-bold">Y₂ Длина</span>
                      <span className="font-bold text-emerald-900">{selectedDay.t2?.toFixed(3)}</span>
                    </div>
                    <div className="bg-amber-50 border border-amber-100 p-1.5 rounded-lg">
                      <span className="block text-[8px] text-amber-700 font-sans font-bold">Y₃ Расст.1</span>
                      <span className="font-bold text-amber-900">{selectedDay.t3?.toFixed(3)}</span>
                    </div>
                    <div className="bg-purple-50 border border-purple-100 p-1.5 rounded-lg">
                      <span className="block text-[8px] text-purple-700 font-sans font-bold">Y₄ Расст.2</span>
                      <span className="font-bold text-purple-900">{selectedDay.t4?.toFixed(3)}</span>
                    </div>
                    <div className="bg-pink-50 border border-pink-100 p-1.5 rounded-lg">
                      <span className="block text-[8px] text-pink-700 font-sans font-bold">Y₅ Угол</span>
                      <span className="font-bold text-pink-900">{selectedDay.t5?.toFixed(3)}</span>
                    </div>
                  </div>
                </div>
              )}

              {/* Samples list for that day */}
              <div className="space-y-1.5 pt-1">
                <span className="text-[10px] font-bold text-slate-500 uppercase tracking-tight block">
                  Зарегистрированные листья ({selectedDay.leaves.length}):
                </span>
                <div className="max-h-36 overflow-y-auto space-y-1 pr-1">
                  {selectedDay.leaves.map(({ leaf, territoryName }) => {
                    const lRating = getEnvironmentalRating(leaf.zScore);
                    return (
                      <div
                        key={leaf.id}
                        className="flex items-center justify-between p-2 rounded-xl bg-slate-50 border border-slate-100 text-[11px]"
                      >
                        <div className="min-w-0 pr-2">
                          <p className="font-semibold text-slate-800 truncate">
                            {leaf.photoName || "Лист берёзы"}
                          </p>
                          <p className="text-[10px] text-slate-400 truncate">
                            {territoryName}
                          </p>
                        </div>
                        <div className="flex items-center gap-1.5 shrink-0">
                          <span className="font-mono font-bold text-slate-700">
                            {leaf.zScore.toFixed(4)}
                          </span>
                          <span
                            className="text-[9px] font-black px-1.5 py-0.5 rounded"
                            style={{
                              backgroundColor: lRating.hexColor,
                              color: lRating.score === 2 ? "#0f172a" : "#ffffff",
                            }}
                          >
                            Балл {lRating.score}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          ) : (
            <p className="text-xs text-slate-400 py-2">
              За выбранную дату замеры не проводились. Выберите другой день на графике выше.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
