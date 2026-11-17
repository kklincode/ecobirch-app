# Берёза — Цифровая морфометрия и ФА (*Betula pendula*, Android v3.0)

Нативное Android-приложение (Kotlin + Jetpack Compose + Material 3 + SQLite Room) для цифровой листовой морфометрии, статистической оценки флуктуирующей асимметрии (FA) берёзы повислой (*Betula pendula Roth.*) и полевого мониторинга с контролем биостатистических допущений (DA, AS, ME, псевдорепликация, слепой протокол).

---

## Научная архитектура v3.0

Полная спецификация приведена в [`docs/SCIENTIFIC_SPEC_V3.md`](docs/SCIENTIFIC_SPEC_V3.md), а специализированные научные инструкции — в каталоге [`docs/skills/`](docs/skills/).

### Ключевые научно-методические обновления
- **Иерархическая модель выборки (`Territory → Tree → Leaf`)**:
  - Каждое измерение привязывается к конкретному дереву (`treeId`, `treeLabel`), чтобы не допускать псевдорепликации.
  - Междревесная статистика (`treeMeanFA`, `treeLevelSE`, 95% ДИ по $t$-критерию Стьюдента) рассчитывается по усреднённым показателям первичных деревьев.
- **Знаковая асимметрия и проверка допущений FA (`LeafBioindicationEngine`)**:
  - Для каждого признака Y1–Y5 хранятся $X_{\text{л}}$, $X_{\text{п}}$, **знаковая** относительная асимметрия $A_s = (X_{\text{л}} - X_{\text{п}})/(X_{\text{л}} + X_{\text{п}})$ и модульный индекс $Y_i = |A_s|$.
  - Автоматически диагностируются **направленная асимметрия (DA)**, **антисимметрия (AS)**, **размерная зависимость** и **ошибка измерения (ME)** по слепым повторным замерам (`replicateOfLeafId`).
- **Слепой режим измерений (Kozlov 2015, 2025; Graham 2021)**:
  - Включается в настройках или запускается при нажатии **«Повтор (ME)»** в Журнале: скрывает название участка, $Z$, $Y_1\dots Y_5$ и баллы до сохранения результата.
- **Режим чистой морфометрии по умолчанию**:
  - Ориентировочная 5-балльная шкала В.М. Захарова (2000) отключена по умолчанию (`enableZakharovScale = false`) и при включении маркируется как региональный справочный ориентир с предупреждением о смешивающих факторах (Májeková et al. 2024; Adamovich & Olkova 2024; Erofeeva & Yakimov 2020).
- **Бесшовная миграция Room v1 → v2 (`MIGRATION_1_2`)**:
  - Пользовательские данные, сохранённые в версии 1, полностью сохраняются при обновлении с безопасными значениями по умолчанию.

---

## Сборка и тестирование

### Требования
- **JDK 17**
- **Android SDK** (`compileSdk = 35`, `build-tools = 35.0.0`)

### Команды сборки (рекомендуется запускать последовательно)

```bash
./gradlew compileDebugUnitTestKotlin
./gradlew testDebugUnitTest --rerun
./gradlew assembleDebug
./gradlew lintDebug
```

---

## Структура проекта

```
docs/
├── SCIENTIFIC_SPEC_V3.md               # Научно-техническое ТЗ v3.0 с DOI-ссылками
└── skills/                             # 7 модульных SKILL.md (литература, аудит, ботаника, CV, биостатистика, QA)
app/src/main/java/ru/ecobirch/fa/app/
├── domain/
│   ├── model/LeafModels.kt             # Иерархические модели, Signed/Unsigned признаки, DA/AS/ME отчёты
│   ├── math/LeafBioindicationEngine.kt # Биостатистика (DA, AS, ME, tree-level 95% CI)
│   └── sample/SampleTerritoriesFactory.kt # Изолированные демо-участки (6 деревьев, 12 первичных + 2 ME повтора)
├── vision/
│   └── LeafLandmarkDetector.kt         # Офлайн-детектор контура листа и 12 ориентиров
├── data/
│   ├── local/                          # Room DB v2 (MIGRATION_1_2), PhotoStorageManager (EXIF), SettingsRepository
│   ├── remote/                         # GeocodingService (OSM Nominatim/Photon), GeminiLeafAnalyzer
│   └── repository/JournalRepository.kt # Репозиторий и совместимый JSON импорт/экспорт
└── ui/
    ├── components/                     # LeafCalibrationCanvas (зум + лупа), FACharts, EcoPassportDialog, SettingsPrivacySheet
    ├── screens/                        # ScanScreen, MapScreen (OSM), JournalScreen, MethodologyScreen
    ├── state/MainViewModel.kt          # Состояние экранов, treeId, blind/ME режим, геолокация без GMS
    └── navigation/EcoBirchNav.kt       # Навигация Material 3
```
