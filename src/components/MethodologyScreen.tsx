import React, { useState } from "react";
import { BookOpen, Info, HelpCircle, Layers, ArrowRight, Eye } from "lucide-react";
import { telegramHaptic } from "../telegram";

export default function MethodologyScreen() {
  const [activeTab, setActiveTab] = useState<"theory" | "traits" | "scale" | "birch">("theory");
  const [selectedTrait, setSelectedTrait] = useState<number | null>(null);

  const traitsHelp = [
    {
      id: 1,
      title: "Признак 1: Ширина левой и правой половины листа",
      desc: "Измеряется максимальное расстояние от центральной (главной) жилки до края листовой пластинки на левой и правой сторонах (Хл1 и Хп1).",
      points: "Измеряется перпендикуляр от линии главной жилки до точек максимальной ширины края."
    },
    {
      id: 2,
      title: "Признак 2: Длина второй жилки второго порядка от основания",
      desc: "Вторая крупная жилка, отходящая от центральной жилки, считается от основания листа вверх (Хл2 и Хп2). Измеряется её длина от основания ответвления до края.",
      points: "Измеряется расстояние от пазухи ответвления до кончика жилки на левой и правой сторонах листа."
    },
    {
      id: 3,
      title: "Признак 3: Расстояние между основаниями 1-й и 2-й жилок",
      desc: "Измеряется расстояние между точками отхождения первой и второй боковых жилок от центральной жилки листа вверх (Хл3 и Хп3).",
      points: "Измеряется шаг ответвлений на самом стебле центральной жилки."
    },
    {
      id: 4,
      title: "Признак 4: Расстояние между концами 1-й и 2-й жилок",
      desc: "Расстояние по прямой между точками выхода первой и второй жилок второго порядка на зубчатый край листа (Хл4 и Хп4).",
      points: "Измеряет внешнее расстояние на зубцах края листа."
    },
    {
      id: 5,
      title: "Признак 5: Угол между главной жилкой и 2-й боковой жилкой",
      desc: "Угол сопряжения (в градусах) между вектором центральной жилки(вверх) и вектором второй боковой жилки (Хл5 и Хп5).",
      points: "Рассчитывается тригонометрически через векторы соответствующих точек."
    }
  ];

  return (
    <div className="flex-1 overflow-y-auto px-4 py-5 flex flex-col bg-slate-50 text-slate-800">
      
      {/* Header */}
      <div className="flex items-center gap-3 mb-5">
        <div className="p-2.5 rounded-xl bg-emerald-50 text-emerald-600 border border-emerald-100">
          <BookOpen className="w-6 h-6" />
        </div>
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">Методика оценки</h1>
          <p className="text-xs text-slate-500 font-mono">Берёза повислая (Betula pendula) — Биоиндикация</p>
        </div>
      </div>

      {/* Tabs */}
      <div className="grid grid-cols-4 gap-1 bg-slate-100 p-1 rounded-xl mb-5 text-[10px] sm:text-xs font-semibold">
        <button
          onClick={() => {
            telegramHaptic("light");
            setActiveTab("theory");
          }}
          className={`py-2 rounded-lg transition-all text-center ${
            activeTab === "theory"
              ? "bg-white text-emerald-700 shadow-sm font-bold"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          Суть
        </button>
        <button
          onClick={() => {
            telegramHaptic("light");
            setActiveTab("traits");
          }}
          className={`py-2 rounded-lg transition-all text-center ${
            activeTab === "traits"
              ? "bg-white text-emerald-700 shadow-sm font-bold"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          Признаки
        </button>
        <button
          onClick={() => {
            telegramHaptic("light");
            setActiveTab("scale");
          }}
          className={`py-2 rounded-lg transition-all text-center ${
            activeTab === "scale"
              ? "bg-white text-emerald-700 shadow-sm font-bold"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          Шкала
        </button>
        <button
          onClick={() => {
            telegramHaptic("light");
            setActiveTab("birch");
          }}
          className={`py-2 rounded-lg transition-all text-center ${
            activeTab === "birch"
              ? "bg-white text-emerald-700 shadow-sm font-bold"
              : "text-slate-600 hover:text-slate-900"
          }`}
        >
          Берёза
        </button>
      </div>

      {/* Content Area */}
      {activeTab === "theory" && (
        <div className="space-y-4 text-sm leading-relaxed text-slate-600">
          <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-xs">
            <h3 className="font-semibold text-slate-900 mb-2 flex items-center gap-2">
              <Info size={16} className="text-emerald-500" />
              1. Цель и биоиндикация
            </h3>
            <p className="text-slate-600">
              Определение качества окружающей среды по <strong>флуктуирующей асимметрии (FA)</strong> листьев берёзы повислой. 
              Метод основан на том, что экологический стресс (загрязнение, смог, токсины) нарушает стабильный симметричный генезис листовой пластины на ранней стадии.
            </p>
          </div>

          <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-xs">
            <h3 className="font-semibold text-slate-900 mb-2 flex items-center gap-2">
              <Layers size={16} className="text-emerald-500" />
              2. Правила сбора листьев
            </h3>
            <ul className="list-disc pl-5 space-y-1.5 text-slate-600">
              <li>Листья собирают с <strong>нижней части кроны</strong> деревьев.</li>
              <li>Сбор ведут строго с <strong>укороченных побегов</strong> (не с вытянутых ростовых веток).</li>
              <li>Берут листья примерно одинакового размера, без рваных краёв и поражений гусеницами.</li>
              <li>Для полной экологической экспертизы точки отбирают по <strong>30 деревьев</strong>, с каждого по <strong>100 листьев</strong>. В приложении допускается точечный замер одного или нескольких характерных листьев.</li>
            </ul>
          </div>

          <div className="bg-emerald-50/50 p-4 rounded-xl border border-emerald-100 text-xs text-emerald-800">
            <h4 className="font-bold mb-1">💡 Механизм оценки асимметрии:</h4>
            <p className="text-emerald-700/90 mt-1">
              Показатель элиминирует разницу в размерах листьев за счет соотношения левых и правых измерений. Рассчитывается разница длин признаков слева и справа, нормированная на их суммарную величину. Это позволяет получить чистый показатель асимметрии развития, независимый от абсолютного размера листа.
            </p>
          </div>

          <div className="bg-amber-50 p-4 rounded-xl border border-amber-200 text-xs text-amber-900 flex gap-2">
            <HelpCircle size={18} className="text-amber-600 shrink-0 mt-0.5" />
            <div>
              <p className="font-semibold mb-0.5">Ограничение метода:</p>
              Биоиндикация выявляет комплексную стрессовую нагрузку, но не разделяет её на фракции PM2.5, солями свинца или диоксид азота. Это интегральная оценка здоровья биоты.
            </div>
          </div>
        </div>
      )}

      {activeTab === "traits" && (
        <div className="space-y-4">
          <p className="text-xs text-slate-500 mb-2 text-center">
            Нажмите на признак ниже, чтобы увидеть карту измерения на листе
          </p>

          {/* Interactive Leaf SVG Diagram */}
          <div className="relative bg-slate-900 aspect-square rounded-2xl overflow-hidden flex items-center justify-center p-4 border border-slate-800 shadow-inner">
            
            {/* Legend inside SVG Container */}
            <div className="absolute top-3 left-3 bg-slate-950/80 px-2 py-1 rounded text-[10px] font-mono text-slate-400 border border-slate-800 z-10">
              {selectedTrait ? `Активен Признак ${selectedTrait}` : "Выберите признак ниже"}
            </div>

            <svg viewBox="0 0 100 100" className="w-full h-full max-w-[280px]">
              {/* Central Leaf Body base shape */}
              <path
                d="M50,90 C40,90 20,75 16,55 C12,35 30,22 50,12 C70,22 88,35 84,55 C80,75 60,90 50,90 Z"
                fill="#052e16"
                stroke="#15803d"
                strokeWidth="1.5"
                opacity="0.9"
              />
              
              {/* Petiole Stem */}
              <line x1="50" y1="90" x2="50" y2="96" stroke="#15803d" strokeWidth="2.5" />
              
              {/* Main Vein */}
              <line x1="50" y1="90" x2="50" y2="12" stroke="#4ade80" strokeWidth="2" strokeDasharray={selectedTrait === 1 || selectedTrait === 5 ? "1 1" : undefined} />
              
              {/* Side Vein 1 Left */}
              <line x1="50" y1="70" x2="22" y2="52" stroke="#4ade80" strokeWidth="1.5" opacity={selectedTrait === 3 || selectedTrait === 4 ? 1 : 0.6} />
              {/* Side Vein 1 Right */}
              <line x1="50" y1="70" x2="78" y2="52" stroke="#4ade80" strokeWidth="1.5" opacity={selectedTrait === 3 || selectedTrait === 4 ? 1 : 0.6} />

              {/* Side Vein 2 Left (Active for trait 2, 3, 4, 5) */}
              <line x1="50" y1="50" x2="26" y2="32" stroke="#4ade80" strokeWidth="1.8" opacity={selectedTrait === 2 || selectedTrait === 3 || selectedTrait === 4 || selectedTrait === 5 ? 1 : 0.6} />
              {/* Side Vein 2 Right */}
              <line x1="50" y1="50" x2="74" y2="32" stroke="#4ade80" strokeWidth="1.8" opacity={selectedTrait === 2 || selectedTrait === 3 || selectedTrait === 4 || selectedTrait === 5 ? 1 : 0.6} />

              {/* Dynamic highlights based on selection */}
              {selectedTrait === 1 && (
                <>
                  {/* Trait 1: Max Width lines */}
                  <line x1="50" y1="55" x2="18" y2="55" stroke="#facc15" strokeWidth="2" strokeDasharray="2 1" />
                  <line x1="50" y1="55" x2="82" y2="55" stroke="#facc15" strokeWidth="2" strokeDasharray="2 1" />
                  <circle cx="18" cy="55" r="2" fill="#ef4444" />
                  <circle cx="82" cy="55" r="2" fill="#ef4444" />
                  <text x="32" y="52" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хл1</text>
                  <text x="62" y="52" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хп1</text>
                </>
              )}

              {selectedTrait === 2 && (
                <>
                  {/* Trait 2: Secondary vein length */}
                  <circle cx="26" cy="32" r="2" fill="#ef4444" />
                  <circle cx="74" cy="32" r="2" fill="#ef4444" />
                  <text x="21" y="27" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хл2</text>
                  <text x="73" y="27" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хп2</text>
                  <path d="M26,32 L50,50" stroke="#facc15" strokeWidth="2" />
                  <path d="M74,32 L50,50" stroke="#facc15" strokeWidth="2" />
                </>
              )}

              {selectedTrait === 3 && (
                <>
                  {/* Trait 3: Distance between bases of 1st and 2nd veins */}
                  <circle cx="50" cy="70" r="1.8" fill="#facc15" />
                  <circle cx="50" cy="50" r="1.8" fill="#facc15" />
                  <line x1="50" y1="70" x2="50" y2="50" stroke="#facc15" strokeWidth="3" />
                  <text x="53" y="62" fill="#facc15" fontSize="6" fontWeight="bold" fontFamily="monospace">Хл3 / Хп3</text>
                </>
              )}

              {selectedTrait === 4 && (
                <>
                  {/* Trait 4: Distance between ends of 1st and 2nd veins */}
                  <line x1="22" y1="52" x2="26" y2="32" stroke="#facc15" strokeWidth="2" strokeDasharray="1 1" />
                  <line x1="78" y1="52" x2="74" y2="32" stroke="#facc15" strokeWidth="2" strokeDasharray="1 1" />
                  <circle cx="22" cy="52" r="1.5" fill="#ef4444" />
                  <circle cx="26" cy="32" r="1.5" fill="#ef4444" />
                  <circle cx="78" cy="52" r="1.5" fill="#ef4444" />
                  <circle cx="74" cy="32" r="1.5" fill="#ef4444" />
                  <text x="14" y="44" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хл4</text>
                  <text x="80" y="44" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хп4</text>
                </>
              )}

              {selectedTrait === 5 && (
                <>
                  {/* Trait 5: Angle between main vein and 2nd vein */}
                  {/* Angle arc left */}
                  <path d="M50,42 A8,8 0 0,0 44.5,44" fill="none" stroke="#facc15" strokeWidth="1.5" />
                  {/* Angle arc right */}
                  <path d="M50,42 A8,8 0 0,1 55.4,44.2" fill="none" stroke="#facc15" strokeWidth="1.5" />
                  <text x="40" y="40" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хл5</text>
                  <text x="55" y="40" fill="#facc15" fontSize="5" fontWeight="bold" fontFamily="monospace">Хп5</text>
                </>
              )}
            </svg>
          </div>

          {/* Traits Selector Buttons */}
          <div className="space-y-2">
            {traitsHelp.map((trait) => (
              <button
                key={trait.id}
                onClick={() => setSelectedTrait(trait.id === selectedTrait ? null : trait.id)}
                className={`w-full text-left p-3.5 rounded-xl border transition-all flex flex-col ${
                  selectedTrait === trait.id
                    ? "bg-slate-900 text-white border-slate-900 shadow-md"
                    : "bg-white text-slate-700 border-slate-100 hover:border-slate-200"
                }`}
              >
                <div className="flex items-center justify-between w-full">
                  <span className="font-semibold text-sm line-clamp-1">{trait.title}</span>
                  <div className="flex items-center gap-1">
                    <span className={`text-[10px] font-mono px-1.5 py-0.5 rounded ${selectedTrait === trait.id ? "bg-emerald-950 text-emerald-400" : "bg-slate-100 text-slate-500"}`}>
                      {trait.id === 1 ? "Хл1 / Хп1" : trait.id === 2 ? "Хл2 / Хп2" : trait.id === 3 ? "Хл3 / Хп3" : trait.id === 4 ? "Хл4 / Хп4" : "Угол°"}
                    </span>
                    <Eye size={14} className={selectedTrait === trait.id ? "text-emerald-400" : "text-slate-400"} />
                  </div>
                </div>
                
                {selectedTrait === trait.id && (
                  <div className="mt-2.5 pt-2.5 border-t border-slate-800 text-slate-300 text-xs leading-relaxed space-y-2">
                    <p>{trait.desc}</p>
                    <p className="text-[11px] text-slate-400 font-sans italic">{trait.points}</p>
                  </div>
                )}
              </button>
            ))}
          </div>

        </div>
      )}

      {activeTab === "scale" && (
        <div className="space-y-4">
          <div className="bg-white p-4 rounded-xl border border-slate-100">
            <h3 className="font-semibold text-slate-900 mb-2 flex items-center gap-2">
              <Layers size={16} className="text-emerald-500" />
              Шкала интегрального показателя FA
            </h3>
            <p className="text-xs text-slate-500 leading-relaxed">
              Интегральный показатель флуктуирующей асимметрии (FA) получается вычислением среднего значения Z для всех листьев на исследуемой территории. Результат интерпретируется по пятибалльной шкале П.М. Мазурова:
            </p>
          </div>

          <div className="space-y-2.5">
            {/* Degree cards */}
            {[
              { score: 1, range: "до 0.055", label: "Условно нормальное", desc: "Растения развиваются стабильно, антропогенный стресс отсутствует.", style: "bg-emerald-500 text-white", ring: "ring-emerald-500/20" },
              { score: 2, range: "0.055 – 0.060", label: "Начальные отклонения", desc: "Слабое фоновое загрязнение или слабая реакция стабильности.", style: "bg-yellow-400 text-slate-900", ring: "ring-yellow-400/20" },
              { score: 3, range: "0.060 – 0.065", label: "Средний уровень загрязнения", desc: "Заметное антропогенное воздействие, умеренное подавление развития.", style: "bg-amber-500 text-white", ring: "ring-amber-500/20" },
              { score: 4, range: "0.065 – 0.070", label: "Значительное загрязнение", desc: "Высокая экологическая напряженность на территории.", style: "bg-rose-500 text-white", ring: "ring-rose-500/20" },
              { score: 5, range: "более 0.070", label: "Критическое состояние", desc: "Сильное техногенное давление, критический уровень токсикации.", style: "bg-red-800 text-white", ring: "ring-red-800/20" },
            ].map((lv) => (
              <div
                key={lv.score}
                className="bg-white p-3 rounded-xl border border-slate-100 flex items-start gap-3.5 shadow-xs"
              >
                <div className={`w-8 h-8 rounded-lg shrink-0 flex items-center justify-center font-bold text-sm ${lv.style}`}>
                  {lv.score}
                </div>
                <div className="flex-1 space-y-0.5">
                  <div className="flex items-center justify-between">
                    <span className="font-bold text-slate-900 text-sm">{lv.label}</span>
                    <span className="text-[11px] font-mono text-slate-500 font-semibold">{lv.range}</span>
                  </div>
                  <p className="text-xs text-slate-500 leading-relaxed">{lv.desc}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="bg-slate-900 text-white p-4 rounded-xl border border-slate-800 text-xs leading-relaxed mt-2">
            Вычисление интегрального показателя флуктуирующей асимметрии (FA) на территории требует анализа не менее 15-30 листьев с разных берез в радиусе 100 метров, после чего усредняется их индивидуальный показатель Z.
          </div>
        </div>
      )}

      {activeTab === "birch" && (
        <div className="space-y-4 text-xs leading-relaxed">
          {/* Card 1: Overview */}
          <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-xs space-y-2">
            <h3 className="font-bold text-slate-900 text-sm flex items-center gap-2">
              <span className="p-1.5 rounded-lg bg-emerald-50 text-emerald-600">🌿</span>
              Берёза повислая (Betula pendula)
            </h3>
            <p className="text-slate-600">
              Основной вид-биоиндикатор в умеренных широтах Евразии, используемый в экологическом мониторинге благодаря высокой чувствительности к атмосферному загрязнению и техногенному воздействию. Также известна как <strong>Берёза бородавчатая (Betula verrucosa)</strong>.
            </p>
          </div>

          {/* Card 2: Key Differences Grid */}
          <div className="bg-white p-4 rounded-xl border border-slate-100 shadow-xs space-y-3">
            <h4 className="font-bold text-slate-900 text-xs uppercase tracking-wider text-emerald-700">
              Отличия от других видов берёз
            </h4>

            <div className="space-y-3">
              {/* vs Betula pubescens */}
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 space-y-2">
                <div className="flex justify-between items-center border-b border-slate-200 pb-1.5">
                  <span className="font-bold text-slate-800 text-[11px]">Берёза пушистая (Betula pubescens)</span>
                  <span className="text-[9px] bg-amber-100 text-amber-800 font-extrabold px-1.5 py-0.5 rounded font-mono">Сходный вид</span>
                </div>
                <p className="text-slate-600">
                  Часто растет по соседству, но непригодна для данного метода оценки из-за генетической склонности к сильной естественной изменчивости и опушению листьев.
                </p>
                <div className="grid grid-cols-2 gap-2 text-[10px] mt-1 pt-1">
                  <div className="bg-white p-2 rounded border border-slate-150">
                    <span className="font-bold text-emerald-700 block mb-0.5">Б. повислая:</span>
                    • Ветви тонкие, <strong>повислые</strong>, усеяны мелкими <strong>бородавочками</strong> (желёзками).<br/>
                    • Листья треугольно-ромбические с клиновидным основанием, голые.<br/>
                    • Кора у основания черная, грубая, с глубокими трещинами.
                  </div>
                  <div className="bg-white p-2 rounded border border-slate-150">
                    <span className="font-bold text-slate-700 block mb-0.5">Б. пушистая:</span>
                    • Ветви направлены вверх/в стороны, <strong>без бородавочек</strong>, молодые побеги мягко опушены.<br/>
                    • Листья округлые или яйцевидные, у основания закругленные.<br/>
                    • Кора ствола белая и гладкая почти до самой земли.
                  </div>
                </div>
              </div>

              {/* vs Betula nana */}
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 space-y-1">
                <span className="font-bold text-slate-800 text-[11px] block">Берёза карликовая (Betula nana)</span>
                <p className="text-slate-600">
                  Представляет собой не дерево, а стелющийся кустарник (высотой до 1 м). Листья у неё мелкие, округлые (шириной до 1.5 см), с притупленной верхушкой и крупными круглыми зубцами. Распространена на севере, в тундре и на верховых болотах.
                </p>
              </div>

              {/* vs Betula ermanii */}
              <div className="p-3 bg-slate-50 rounded-lg border border-slate-100 space-y-1">
                <span className="font-bold text-slate-800 text-[11px] block">Берёза каменная / Эрмана (Betula ermanii)</span>
                <p className="text-slate-600">
                  Горный и восточноазиатский вид. Имеет исключительно прочную и тяжелую древесину. Кора не гладкая, а розовато-бурая или серая, трескается и шелушится крупными лохматыми пластами. Листья яйцевидные, с выраженными рельефными жилками (до 10 пар).
                </p>
              </div>
            </div>
          </div>

          {/* Card 3: Why it matters */}
          <div className="bg-emerald-900 text-emerald-50 p-4 rounded-xl border border-emerald-850 space-y-1.5">
            <h4 className="font-bold text-xs text-emerald-300 font-mono">⚠️ Важность точной идентификации:</h4>
            <p className="text-[11px] text-emerald-100/95 leading-relaxed">
              Использование листьев других видов берёз (например, пушистой или гибридных форм) в расчетах флуктуирующей асимметрии может дать <strong>недостоверный результат</strong>, так как их форма изначально менее стабильна на генетическом уровне. Для экологического мониторинга собирайте листья <strong>только с повислых ветвей березы бородавчатой</strong>.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
