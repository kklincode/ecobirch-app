# Betula — ЭкоБерёза ФА (Android)

Нативное Android-приложение (Kotlin + Jetpack Compose + Material 3) для экспресс-биоиндикации качества окружающей среды по флуктуирующей асимметрии (FA) листьев берёзы повислой (*Betula pendula Roth.*) по методике чл.-корр. РАН В.М. Захарова.

Проект разработан на базе веб-прототипа [kklincode/ecobirch-app](https://github.com/kklincode/ecobirch-app) с устранением всех критических замечаний из `ecobirch-audit.md`.

---

## Ключевые особенности и исправления

- **Полностью нативный Android-стек (без Chrome/TWA и без зависимости от GMS)**:
  - `applicationId = "ru.ecobirch.fa.app"`, `minSdk = 24`, `targetSdk = 35` (актуально для RuStore).
  - Работает офлайн на любых Android-устройствах, включая смартфоны без Google Play Services.
- **Научно корректная математика (`LeafBioindicationEngine`)**:
  - Восстановлена классическая 5-балльная шкала Захарова:
    - **I балл:** `< 0.040` — Условно нормальное
    - **II балл:** `0.040 – 0.044` — Начальные отклонения
    - **III балл:** `0.045 – 0.049` — Средний уровень загрязнения
    - **IV балл:** `0.050 – 0.054` — Существенные отклонения
    - **V балл:** `> 0.054` — Критическое состояние
  - Изотропная геометрия с учётом пропорций снимка (`aspectRatio`).
  - Признак 5 (угол 2-й боковой жилки) вычисляется относительно единого вектора центральной жилки (`base -> tip`).
  - Честные единицы измерения: по умолчанию `% от длины оси листа` (без фиктивных миллиметров), опциональная ручная калибровка эталона в `мм`.
  - Статистика по территориям: $n$, $SD$, $SE$, 95% доверительный интервал ($t$-критерий Стьюдента) и маркировка `Предв. (n < 10)` при малой выборке.
- **Высокоточная калибровка 12 точек (`LeafCalibrationCanvas`)**:
  - Жесты pinch-to-zoom (`1×–5×`) и панорамирование.
  - Живая круглая **лупа 2.6×** при перетаскивании каждой точки.
  - Панель быстрого выбора всех 12 ориентиров + крестовина микросдвига (`0.3%` шаг).
- **Автономная работа и приватность (152-ФЗ)**:
  - Локальный детектор контура листа (`LeafLandmarkDetector`) на чистом Kotlin/Bitmap без отправки фото в облако.
  - Опциональная поддержка Gemini Vision API (только при добровольном согласии и вводе собственного API-ключа в Настройках).
  - Надёжная локальная БД **SQLite Room** + хранение сжатых фото в `filesDir/leaf_photos/` с чтением EXIF GPS/ориентации.
  - Безопасная обработка сбоев (логирование в `last_crash.txt` без удаления журнала) и двойное подтверждение очистки с предложением экспорта в JSON.
- **Карта OpenStreetMap (`osmdroid`)**:
  - Цветовые маркеры территорий по шкале Захарова.
  - Поиск адресов (Nominatim + Photon + прямые координаты) и обратный геокодинг без платных API-ключей.

---

## Сборка в Android Studio или через CLI

### Требования
- **JDK 17**
- **Android SDK** (`compileSdk = 35`, `build-tools = 35.0.0`)

### Команды сборки

```bash
# Запуск модульных тестов доменной математики и статистики
./gradlew testDebugUnitTest

# Сборка отладочного APK
./gradlew assembleDebug

# Сборка релизного APK / AAB (для RuStore потребуется настроить signingConfig)
./gradlew assembleRelease
```

---

## Структура проекта

```
app/src/main/java/ru/ecobirch/fa/app/
├── domain/
│   ├── model/LeafModels.kt             # Модели точек, признаков Y1–Y5, территорий
│   ├── math/LeafBioindicationEngine.kt # Формулы Захарова, геометрия и статистика (n, SE, 95% CI)
│   └── sample/SampleTerritoriesFactory.kt # Учебные шаблоны и изолированные демо-территории
├── vision/
│   └── LeafLandmarkDetector.kt         # Офлайн-сегментация контура листа и авто-разметка 12 точек
├── data/
│   ├── local/                          # Room DB, PhotoStorageManager (EXIF), SettingsRepository
│   ├── remote/                         # GeocodingService (OSM Nominatim/Photon), GeminiLeafAnalyzer
│   └── repository/JournalRepository.kt # Репозиторий и совместимый JSON импорт/экспорт
└── ui/
    ├── components/                     # LeafCalibrationCanvas (зум + лупа), FACharts, EcoPassport, Настройки
    ├── screens/                        # ScanScreen (CameraX), MapScreen (OSM), JournalScreen, MethodologyScreen
    ├── state/MainViewModel.kt          # Состояние экранов и геолокация без GMS
    └── navigation/EcoBirchNav.kt       # Навигация Material 3
```
