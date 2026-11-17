package ru.ecobirch.fa.app.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val privacyPolicyAccepted: Boolean = false,
    val useCloudGeminiAi: Boolean = false,
    val geminiApiKey: String = "",
    val includeDemoInExport: Boolean = false,
    val blindMeasurementMode: Boolean = false,
    val enableZakharovScale: Boolean = false
)

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences("betula_settings_v2", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun load(): AppSettings = AppSettings(
        privacyPolicyAccepted = prefs.getBoolean(KEY_PRIVACY_ACCEPTED, false),
        useCloudGeminiAi = prefs.getBoolean(KEY_USE_GEMINI, false),
        geminiApiKey = prefs.getString(KEY_GEMINI_API_KEY, "") ?: "",
        includeDemoInExport = prefs.getBoolean(KEY_INCLUDE_DEMO_EXPORT, false),
        blindMeasurementMode = prefs.getBoolean(KEY_BLIND_MODE, false),
        enableZakharovScale = prefs.getBoolean(KEY_ENABLE_ZAKHAROV, false)
    )

    fun setPrivacyPolicyAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_ACCEPTED, accepted).apply()
        _settings.value = load()
    }

    fun updateGeminiConfig(enabled: Boolean, apiKey: String) {
        prefs.edit()
            .putBoolean(KEY_USE_GEMINI, enabled)
            .putString(KEY_GEMINI_API_KEY, apiKey.trim())
            .apply()
        _settings.value = load()
    }

    fun setIncludeDemoInExport(include: Boolean) {
        prefs.edit().putBoolean(KEY_INCLUDE_DEMO_EXPORT, include).apply()
        _settings.value = load()
    }

    fun setBlindMeasurementMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLIND_MODE, enabled).apply()
        _settings.value = load()
    }

    fun setEnableZakharovScale(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLE_ZAKHAROV, enabled).apply()
        _settings.value = load()
    }

    companion object {
        private const val KEY_PRIVACY_ACCEPTED = "privacy_policy_accepted"
        private const val KEY_USE_GEMINI = "use_cloud_gemini_ai"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_INCLUDE_DEMO_EXPORT = "include_demo_export"
        private const val KEY_BLIND_MODE = "blind_measurement_mode"
        private const val KEY_ENABLE_ZAKHAROV = "enable_zakharov_scale"
    }
}
