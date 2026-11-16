package ru.ecobirch.fa.app

import android.app.Application
import org.osmdroid.config.Configuration
import ru.ecobirch.fa.app.data.local.EcoBirchDatabase
import ru.ecobirch.fa.app.data.local.PhotoStorageManager
import ru.ecobirch.fa.app.data.local.SettingsRepository
import ru.ecobirch.fa.app.data.remote.GeminiLeafAnalyzer
import ru.ecobirch.fa.app.data.remote.GeocodingService
import ru.ecobirch.fa.app.data.repository.JournalRepository
import java.io.File

class EcoBirchApplication : Application() {

    lateinit var database: EcoBirchDatabase
        private set
    lateinit var photoStorageManager: PhotoStorageManager
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var geocodingService: GeocodingService
        private set
    lateinit var geminiLeafAnalyzer: GeminiLeafAnalyzer
        private set
    lateinit var journalRepository: JournalRepository
        private set

    override fun onCreate() {
        super.onCreate()

        // Configure osmdroid tile cache and compliant User-Agent (no GMS needed)
        Configuration.getInstance().apply {
            userAgentValue = "BetulaBioindicationAndroid/2.0 (ru.ecobirch.fa.app)"
            osmdroidBasePath = File(cacheDir, "osmdroid").apply { mkdirs() }
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles").apply { mkdirs() }
        }

        // Safe crash logger: records trace for diagnostics without ever touching/clearing Room data
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val crashFile = File(filesDir, "last_crash.txt")
                crashFile.writeText(
                    "Timestamp: ${System.currentTimeMillis()}\n" +
                        "Thread: ${thread.name}\n" +
                        throwable.stackTraceToString()
                )
            }
            previousHandler?.uncaughtException(thread, throwable)
        }

        database = EcoBirchDatabase.getInstance(this)
        photoStorageManager = PhotoStorageManager(this)
        settingsRepository = SettingsRepository(this)
        geocodingService = GeocodingService()
        geminiLeafAnalyzer = GeminiLeafAnalyzer()
        journalRepository = JournalRepository(database.journalDao(), photoStorageManager)
    }
}
