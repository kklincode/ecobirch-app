package ru.ecobirch.fa.app.ui.state

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import ru.ecobirch.fa.app.EcoBirchApplication
import ru.ecobirch.fa.app.data.local.AppSettings
import ru.ecobirch.fa.app.data.remote.GeocodePlace
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.LandmarkKey
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.domain.sample.SampleTerritoriesFactory
import ru.ecobirch.fa.app.vision.LeafLandmarkDetector
import java.io.File
import java.util.UUID
import kotlin.coroutines.resume

enum class ScanStage {
    SELECT,
    PROCESSING,
    ADJUST,
    SAVED
}

data class ScanUiState(
    val stage: ScanStage = ScanStage.SELECT,
    val photoPath: String = "template",
    val photoName: String = "Симметричный шаблон",
    val photoBitmap: Bitmap? = null,
    val isTemplate: Boolean = true,
    val aspectRatio: Float = 0.8f,
    val points: LeafPoints = LeafBioindicationEngine.getDefaultLeafPoints(),
    val activeLandmark: LandmarkKey = LandmarkKey.BASE,
    val axisReferenceLengthMm: Double? = null,
    val detectionSource: DetectionSource = DetectionSource.TEMPLATE,
    val statusMessage: String = "",
    val processingHint: String = "Анализ морфологии листа...",
    val exifLatitude: Double? = null,
    val exifLongitude: Double? = null,
    val targetTerritoryId: String? = null,
    val isSaving: Boolean = false,
    val savedLeaf: LeafAnalysis? = null,
    val savedTerritoryName: String = "",
    val showLocationChooserDialog: Boolean = false,
    val pendingLeafForSave: LeafAnalysis? = null
)

data class MapUiState(
    val searchQuery: String = "",
    val searchResults: List<GeocodePlace> = emptyList(),
    val isSearching: Boolean = false,
    val isAddMode: Boolean = false,
    val pendingPinLat: Double? = null,
    val pendingPinLng: Double? = null,
    val pendingAddressName: String = "",
    val pendingAddressDesc: String = "",
    val isResolvingAddress: Boolean = false,
    val selectedTerritoryId: String? = null,
    val pendingLeafToAttach: LeafAnalysis? = null,
    val centerLat: Double = 55.7558,
    val centerLng: Double = 37.6173,
    val centerTrigger: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EcoBirchApplication
    private val repo = app.journalRepository
    private val photoManager = app.photoStorageManager
    private val settingsRepo = app.settingsRepository
    private val geocoder = app.geocodingService
    private val gemini = app.geminiLeafAnalyzer
    private val leafPhotoCache = mutableMapOf<String, Bitmap?>()

    val territories: StateFlow<List<Territory>> = repo.observeTerritories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = settingsRepo.settings

    private val _scanState = MutableStateFlow(ScanUiState())
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _mapState = MutableStateFlow(MapUiState())
    val mapState: StateFlow<MapUiState> = _mapState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _activeTabRoute = MutableStateFlow("scan")
    val activeTabRoute: StateFlow<String> = _activeTabRoute.asStateFlow()

    private val _focusedJournalTerritoryId = MutableStateFlow<String?>(null)
    val focusedJournalTerritoryId: StateFlow<String?> = _focusedJournalTerritoryId.asStateFlow()

    private val _lastCrashLog = MutableStateFlow<String?>(loadCrashLogIfExists())
    val lastCrashLog: StateFlow<String?> = _lastCrashLog.asStateFlow()

    private fun loadCrashLogIfExists(): String? {
        val f = File(app.filesDir, "last_crash.txt")
        return if (f.exists()) runCatching { f.readText() }.getOrNull() else null
    }

    fun dismissCrashLog() {
        runCatching { File(app.filesDir, "last_crash.txt").delete() }
        _lastCrashLog.value = null
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun navigateToTab(route: String) {
        _activeTabRoute.value = route
    }

    fun startScanForTerritory(territoryId: String) {
        _scanState.update {
            it.copy(
                stage = ScanStage.SELECT,
                targetTerritoryId = territoryId
            )
        }
        _activeTabRoute.value = "scan"
    }

    fun openTerritoryInJournal(territoryId: String) {
        _focusedJournalTerritoryId.value = territoryId
        _activeTabRoute.value = "journal"
    }

    fun consumeFocusedJournalTerritory() {
        _focusedJournalTerritoryId.value = null
    }

    // ------------------------------------------------------
    // SCAN & CALIBRATION FLOW
    // ------------------------------------------------------

    fun selectTemplate(type: String) {
        val (pts, name) = when (type) {
            "healthy" -> SampleTerritoriesFactory.healthyTemplatePoints to "Шаблон: Чистый лесопарк (БС-1)"
            "moderate" -> SampleTerritoriesFactory.moderateTemplatePoints to "Шаблон: Городской сквер (ГС-1)"
            else -> SampleTerritoriesFactory.stressedTemplatePoints to "Шаблон: Автомагистраль (ПР-1)"
        }
        _scanState.update {
            it.copy(
                stage = ScanStage.ADJUST,
                photoPath = "template",
                photoName = name,
                photoBitmap = null,
                isTemplate = true,
                aspectRatio = 0.8f,
                points = pts,
                activeLandmark = LandmarkKey.BASE,
                axisReferenceLengthMm = null,
                detectionSource = DetectionSource.TEMPLATE,
                statusMessage = "Учебный векторный шаблон для тренировки калибровки.",
                exifLatitude = null,
                exifLongitude = null
            )
        }
    }

    fun handlePhotoUri(uri: Uri, name: String = "Лист_${System.currentTimeMillis() % 10000}.jpg") {
        viewModelScope.launch {
            _scanState.update {
                it.copy(
                    stage = ScanStage.PROCESSING,
                    processingHint = "Загрузка и подготовка снимка..."
                )
            }
            val loaded = photoManager.importFromUri(uri, name)
            if (loaded == null) {
                _scanState.update { it.copy(stage = ScanStage.SELECT) }
                showToast("Не удалось открыть изображение")
                return@launch
            }

            val currentSettings = settingsRepo.settings.value
            val useGemini = currentSettings.useCloudGeminiAi &&
                currentSettings.privacyPolicyAccepted &&
                currentSettings.geminiApiKey.isNotBlank()

            var detection = if (useGemini) {
                _scanState.update { it.copy(processingHint = "Распознавание через Gemini Vision...") }
                val base64 = withContext(Dispatchers.Default) {
                    photoManager.encodeBitmapToJpegBase64(loaded.bitmap)
                }
                gemini.analyzeLeaf(base64, currentSettings.geminiApiKey)
            } else {
                null
            }

            if (detection == null) {
                _scanState.update { it.copy(processingHint = "Локальный поиск контура и жилок листа...") }
                detection = withContext(Dispatchers.Default) {
                    LeafLandmarkDetector.detectLandmarks(loaded.bitmap)
                }
            }

            _scanState.update {
                it.copy(
                    stage = ScanStage.ADJUST,
                    photoPath = loaded.savedPath,
                    photoName = loaded.displayName,
                    photoBitmap = loaded.bitmap,
                    isTemplate = false,
                    aspectRatio = loaded.aspectRatio,
                    points = detection.points,
                    activeLandmark = LandmarkKey.BASE,
                    axisReferenceLengthMm = null,
                    detectionSource = detection.source,
                    statusMessage = detection.statusMessage,
                    exifLatitude = loaded.exifLatitude,
                    exifLongitude = loaded.exifLongitude
                )
            }
        }
    }

    fun setActiveLandmark(key: LandmarkKey) {
        _scanState.update { it.copy(activeLandmark = key) }
    }

    fun updateLandmark(key: LandmarkKey, point: LeafPoint) {
        _scanState.update {
            it.copy(
                activeLandmark = key,
                points = it.points.withUpdated(key, point)
            )
        }
    }

    fun nudgeActiveLandmark(dxPercent: Float, dyPercent: Float) {
        _scanState.update { st ->
            val current = st.points[st.activeLandmark]
            val updated = LeafPoint(current.x + dxPercent, current.y + dyPercent)
            st.copy(points = st.points.withUpdated(st.activeLandmark, updated))
        }
    }

    fun setAxisReferenceLengthMm(mm: Double?) {
        _scanState.update {
            it.copy(axisReferenceLengthMm = if (mm != null && mm > 5.0 && mm < 300.0) mm else null)
        }
    }

    fun resetToPhotoSelect() {
        _scanState.update {
            it.copy(
                stage = ScanStage.SELECT,
                pendingLeafForSave = null,
                showLocationChooserDialog = false
            )
        }
    }

    fun initiateSaveMeasurement(hasLocationPermission: Boolean) {
        val st = _scanState.value
        val traits = LeafBioindicationEngine.calculateLeafTraits(
            points = st.points,
            aspectRatio = st.aspectRatio,
            axisReferenceLengthMm = st.axisReferenceLengthMm
        )
        val zScore = LeafBioindicationEngine.calculateZScore(traits)
        val leaf = LeafAnalysis(
            id = "leaf_${UUID.randomUUID().toString().take(8)}",
            photoUrl = st.photoPath,
            photoName = st.photoName,
            points = st.points,
            isTemplate = st.isTemplate,
            traits = traits,
            zScore = zScore,
            timestamp = SampleTerritoriesFactory.formatIso(System.currentTimeMillis()),
            aspectRatio = st.aspectRatio,
            axisReferenceLengthMm = st.axisReferenceLengthMm,
            detectionSource = st.detectionSource,
            exifLatitude = st.exifLatitude,
            exifLongitude = st.exifLongitude
        )

        // If user already started scan for an explicit territory, save straight to it!
        val presetId = st.targetTerritoryId
        if (!presetId.isNullOrBlank()) {
            val target = territories.value.find { it.id == presetId }
            if (target != null) {
                viewModelScope.launch {
                    _scanState.update { it.copy(isSaving = true) }
                    repo.addMeasurementToTerritory(target.id, leaf)
                    _scanState.update {
                        it.copy(
                            isSaving = false,
                            stage = ScanStage.SAVED,
                            savedLeaf = leaf,
                            savedTerritoryName = target.name
                        )
                    }
                }
                return
            }
        }

        // Otherwise, if EXIF GPS exists or Location permission is granted, offer quick save or territory choice
        viewModelScope.launch {
            _scanState.update { it.copy(isSaving = true) }
            val coords = when {
                st.exifLatitude != null && st.exifLongitude != null ->
                    st.exifLatitude to st.exifLongitude
                hasLocationPermission ->
                    getDeviceLocationWithTimeout(8000L)
                else -> null
            }

            if (coords != null && territories.value.none { !it.isDemo }) {
                // First real territory at current GPS location
                val place = geocoder.reverseGeocode(coords.first, coords.second)
                val newTerritory = Territory(
                    id = "terr_${UUID.randomUUID().toString().take(8)}",
                    name = place.name,
                    description = place.fullAddress,
                    latitude = coords.first,
                    longitude = coords.second,
                    leaves = listOf(leaf),
                    timestamp = SampleTerritoriesFactory.formatIso(System.currentTimeMillis()),
                    isDemo = false
                )
                repo.saveTerritory(newTerritory)
                _scanState.update {
                    it.copy(
                        isSaving = false,
                        stage = ScanStage.SAVED,
                        savedLeaf = leaf,
                        savedTerritoryName = newTerritory.name,
                        targetTerritoryId = newTerritory.id
                    )
                }
            } else {
                // Show territory chooser (existing territories OR create at GPS OR pick on OSM map)
                _scanState.update {
                    it.copy(
                        isSaving = false,
                        pendingLeafForSave = leaf,
                        exifLatitude = coords?.first ?: it.exifLatitude,
                        exifLongitude = coords?.second ?: it.exifLongitude,
                        showLocationChooserDialog = true
                    )
                }
            }
        }
    }

    fun dismissLocationChooser() {
        _scanState.update { it.copy(showLocationChooserDialog = false, pendingLeafForSave = null) }
    }

    fun savePendingLeafToExistingTerritory(territory: Territory) {
        val leaf = _scanState.value.pendingLeafForSave ?: return
        viewModelScope.launch {
            _scanState.update { it.copy(isSaving = true, showLocationChooserDialog = false) }
            repo.addMeasurementToTerritory(territory.id, leaf)
            _scanState.update {
                it.copy(
                    isSaving = false,
                    stage = ScanStage.SAVED,
                    savedLeaf = leaf,
                    savedTerritoryName = territory.name,
                    targetTerritoryId = territory.id,
                    pendingLeafForSave = null
                )
            }
        }
    }

    fun savePendingLeafAtNewCoordinates(lat: Double, lng: Double, customName: String = "") {
        val leaf = _scanState.value.pendingLeafForSave ?: return
        viewModelScope.launch {
            _scanState.update { it.copy(isSaving = true, showLocationChooserDialog = false) }
            val place = geocoder.reverseGeocode(lat, lng)
            val finalName = customName.trim().ifBlank { place.name }
            val newTerritory = Territory(
                id = "terr_${UUID.randomUUID().toString().take(8)}",
                name = finalName,
                description = place.fullAddress,
                latitude = lat,
                longitude = lng,
                leaves = listOf(leaf),
                timestamp = SampleTerritoriesFactory.formatIso(System.currentTimeMillis()),
                isDemo = false
            )
            repo.saveTerritory(newTerritory)
            _scanState.update {
                it.copy(
                    isSaving = false,
                    stage = ScanStage.SAVED,
                    savedLeaf = leaf,
                    savedTerritoryName = newTerritory.name,
                    targetTerritoryId = newTerritory.id,
                    pendingLeafForSave = null
                )
            }
        }
    }

    fun routePendingLeafToMapPicker() {
        val leaf = _scanState.value.pendingLeafForSave ?: return
        _scanState.update { it.copy(showLocationChooserDialog = false, pendingLeafForSave = null) }
        _mapState.update {
            it.copy(
                isAddMode = true,
                pendingLeafToAttach = leaf,
                selectedTerritoryId = null
            )
        }
        _activeTabRoute.value = "map"
        showToast("Нажмите на карту OpenStreetMap, чтобы выбрать место сбора листа")
    }

    // ------------------------------------------------------
    // MAP & GEOCODING FLOW
    // ------------------------------------------------------

    fun updateMapSearchQuery(q: String) {
        _mapState.update { it.copy(searchQuery = q) }
    }

    fun performMapSearch() {
        val q = _mapState.value.searchQuery.trim()
        if (q.isBlank()) return
        viewModelScope.launch {
            _mapState.update { it.copy(isSearching = true) }
            val results = geocoder.search(q)
            _mapState.update { it.copy(isSearching = false, searchResults = results) }
            if (results.isEmpty()) showToast("Ничего не найдено")
        }
    }

    fun selectGeocodeResult(place: GeocodePlace) {
        _mapState.update {
            it.copy(
                searchResults = emptyList(),
                centerLat = place.latitude,
                centerLng = place.longitude,
                centerTrigger = it.centerTrigger + 1,
                pendingPinLat = if (it.isAddMode) place.latitude else it.pendingPinLat,
                pendingPinLng = if (it.isAddMode) place.longitude else it.pendingPinLng,
                pendingAddressName = if (it.isAddMode) place.name else it.pendingAddressName,
                pendingAddressDesc = if (it.isAddMode) place.fullAddress else it.pendingAddressDesc
            )
        }
    }

    fun toggleMapAddMode() {
        _mapState.update {
            it.copy(
                isAddMode = !it.isAddMode,
                pendingPinLat = null,
                pendingPinLng = null,
                pendingAddressName = "",
                pendingAddressDesc = "",
                selectedTerritoryId = null
            )
        }
    }

    fun onMapTapped(lat: Double, lng: Double) {
        val state = _mapState.value
        if (!state.isAddMode) {
            _mapState.update { it.copy(selectedTerritoryId = null) }
            return
        }
        _mapState.update {
            it.copy(
                pendingPinLat = lat,
                pendingPinLng = lng,
                isResolvingAddress = true,
                pendingAddressName = "Определение адреса...",
                pendingAddressDesc = "${"%.5f".format(lat)}, ${"%.5f".format(lng)}"
            )
        }
        viewModelScope.launch {
            val resolved = geocoder.reverseGeocode(lat, lng)
            _mapState.update {
                it.copy(
                    isResolvingAddress = false,
                    pendingAddressName = resolved.name,
                    pendingAddressDesc = resolved.fullAddress
                )
            }
        }
    }

    fun updatePendingTerritoryFields(name: String, desc: String) {
        _mapState.update { it.copy(pendingAddressName = name, pendingAddressDesc = desc) }
    }

    fun confirmCreateTerritoryFromMap() {
        val st = _mapState.value
        val lat = st.pendingPinLat ?: return
        val lng = st.pendingPinLng ?: return
        val pendingLeaf = st.pendingLeafToAttach

        viewModelScope.launch {
            val newTerritory = Territory(
                id = "terr_${UUID.randomUUID().toString().take(8)}",
                name = st.pendingAddressName.trim().ifBlank { "Точка ${"%.3f".format(lat)}°" },
                description = st.pendingAddressDesc.trim(),
                latitude = lat,
                longitude = lng,
                leaves = if (pendingLeaf != null) listOf(pendingLeaf) else emptyList(),
                timestamp = SampleTerritoriesFactory.formatIso(System.currentTimeMillis()),
                isDemo = false
            )
            repo.saveTerritory(newTerritory)
            _mapState.update {
                it.copy(
                    isAddMode = false,
                    pendingPinLat = null,
                    pendingPinLng = null,
                    pendingLeafToAttach = null,
                    selectedTerritoryId = newTerritory.id
                )
            }
            if (pendingLeaf != null) {
                _scanState.update {
                    it.copy(
                        stage = ScanStage.SAVED,
                        savedLeaf = pendingLeaf,
                        savedTerritoryName = newTerritory.name,
                        targetTerritoryId = newTerritory.id
                    )
                }
                showToast("Замер сохранён в точку «${newTerritory.name}»")
            } else {
                showToast("Территория добавлена на карту")
            }
        }
    }

    fun selectMapTerritory(territoryId: String?) {
        _mapState.update { it.copy(selectedTerritoryId = territoryId) }
    }

    fun centerMapOnTerritory(territory: Territory) {
        _mapState.update {
            it.copy(
                centerLat = territory.latitude,
                centerLng = territory.longitude,
                centerTrigger = it.centerTrigger + 1,
                selectedTerritoryId = territory.id
            )
        }
    }

    fun centerMapOnUserGps(hasPermission: Boolean) {
        if (!hasPermission) {
            showToast("Требуется разрешение на геолокацию")
            return
        }
        viewModelScope.launch {
            val coords = getDeviceLocationWithTimeout(7000L)
            if (coords != null) {
                _mapState.update {
                    it.copy(
                        centerLat = coords.first,
                        centerLng = coords.second,
                        centerTrigger = it.centerTrigger + 1
                    )
                }
            } else {
                showToast("Не удалось получить координаты GPS")
            }
        }
    }

    // ------------------------------------------------------
    // JOURNAL, IMPORT/EXPORT, DEMO & SETTINGS
    // ------------------------------------------------------

    fun deleteLeaf(leafId: String) {
        viewModelScope.launch {
            repo.deleteLeaf(leafId)
            showToast("Замер удалён")
        }
    }

    fun deleteTerritory(territoryId: String) {
        viewModelScope.launch {
            repo.deleteTerritory(territoryId)
            if (_mapState.value.selectedTerritoryId == territoryId) {
                _mapState.update { it.copy(selectedTerritoryId = null) }
            }
            showToast("Территория удалена")
        }
    }

    fun loadDemoTerritories() {
        viewModelScope.launch {
            repo.loadDemoTerritories()
            showToast("Загружены 3 демо-территории (по 14 листьев)")
        }
    }

    fun clearDemoTerritories() {
        viewModelScope.launch {
            repo.clearDemoTerritories()
            showToast("Демо-данные удалены")
        }
    }

    fun clearAllJournalData() {
        viewModelScope.launch {
            repo.clearAllUserTerritories()
            showToast("Журнал полностью очищен")
        }
    }

    fun exportJournalToUri(uri: Uri) {
        viewModelScope.launch {
            val includeDemo = settingsRepo.settings.value.includeDemoInExport
            val jsonText = repo.exportJournalToJson(includeDemo = includeDemo)
            runCatching {
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(jsonText.toByteArray(Charsets.UTF_8))
                    }
                }
            }.onSuccess {
                showToast("Журнал успешно экспортирован в JSON")
            }.onFailure {
                showToast("Ошибка записи файла: ${it.localizedMessage}")
            }
        }
    }

    fun importJournalFromUri(uri: Uri) {
        viewModelScope.launch {
            val raw = withContext(Dispatchers.IO) {
                runCatching {
                    app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }
            if (raw.isNullOrBlank()) {
                showToast("Не удалось прочитать файл JSON")
                return@launch
            }
            repo.importJournalFromJson(raw)
                .onSuccess { count -> showToast("Импортировано территорий: $count") }
                .onFailure { showToast("Некорректный формат JSON") }
        }
    }

    suspend fun getThumbnailBitmap(photoPath: String): Bitmap? {
        if (photoPath.isBlank() || photoPath == "template" || photoPath == "stored_locally" || photoPath == "stored_offline") {
            return null
        }
        return leafPhotoCache[photoPath] ?: photoManager.loadBitmapFromPath(photoPath, 360).also {
            leafPhotoCache[photoPath] = it
        }
    }

    fun setPrivacyAccepted(accepted: Boolean) {
        settingsRepo.setPrivacyPolicyAccepted(accepted)
    }

    fun updateGeminiConfig(enabled: Boolean, apiKey: String) {
        settingsRepo.updateGeminiConfig(enabled, apiKey)
        showToast("Настройки сохранены")
    }

    fun setIncludeDemoInExport(include: Boolean) {
        settingsRepo.setIncludeDemoInExport(include)
    }

    fun shareLeafPassport(context: Context, territory: Territory, leaf: LeafAnalysis) {
        val rating = LeafBioindicationEngine.getEnvironmentalRating(leaf.zScore)
        val unit = if (leaf.axisReferenceLengthMm != null) "мм" else "% оси"
        val summary = buildString {
            appendLine("Эко-паспорт листа берёзы повислой (Betula pendula)")
            appendLine("Территория: ${territory.name}")
            appendLine("Координаты: ${"%.5f".format(territory.latitude)}, ${"%.5f".format(territory.longitude)}")
            appendLine("Образец: ${leaf.photoName}")
            appendLine("Интегральный показатель Z: ${"%.4f".format(leaf.zScore)}")
            appendLine("Класс по шкале Захарова: Балл ${rating.score} (${rating.status}, ${rating.rangeLabel})")
            appendLine()
            appendLine("Признаки Y1–Y5:")
            appendLine("• Y1 (Ширина): ${"%.4f".format(leaf.traits.t1Width.yDiff)} (Л=${"%.1f".format(leaf.traits.t1Width.xLeft)}, П=${"%.1f".format(leaf.traits.t1Width.xRight)} $unit)")
            appendLine("• Y2 (2-я жилка): ${"%.4f".format(leaf.traits.t2VeinLen.yDiff)}")
            appendLine("• Y3 (Осн. жилок): ${"%.4f".format(leaf.traits.t3VeinDist.yDiff)}")
            appendLine("• Y4 (Края жилок): ${"%.4f".format(leaf.traits.t4EndDist.yDiff)}")
            appendLine("• Y5 (Угол): ${"%.4f".format(leaf.traits.t5Angle.yDiff)} (Л=${"%.1f".format(leaf.traits.t5Angle.xLeft)}°, П=${"%.1f".format(leaf.traits.t5Angle.xRight)}°)")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Эко-паспорт листа: ${territory.name}")
            putExtra(Intent.EXTRA_TEXT, summary)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться эко-паспортом"))
    }

    @SuppressLint("MissingPermission")
    private suspend fun getDeviceLocationWithTimeout(timeoutMs: Long): Pair<Double, Double>? {
        val lm = app.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        // Check quick recent cached fix (< 2 minutes old)
        val bestLast = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }

        if (bestLast != null && System.currentTimeMillis() - bestLast.time < 120_000L) {
            return bestLast.latitude to bestLast.longitude
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val provider = when {
                lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                else -> return bestLast?.let { it.latitude to it.longitude }
            }
            val fresh = withTimeoutOrNull(timeoutMs) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    runCatching {
                        lm.getCurrentLocation(
                            provider,
                            signal,
                            ContextCompat.getMainExecutor(app)
                        ) { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                    }.onFailure {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            }
            if (fresh != null) return fresh.latitude to fresh.longitude
        }

        return bestLast?.let { it.latitude to it.longitude }
    }
}
