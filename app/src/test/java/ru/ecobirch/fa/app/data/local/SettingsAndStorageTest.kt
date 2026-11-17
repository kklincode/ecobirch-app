package ru.ecobirch.fa.app.data.local

import android.graphics.Bitmap
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.ecobirch.fa.app.EcoBirchApplication
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = EcoBirchApplication::class)
class SettingsAndStorageTest {

    @Test
    fun settingsRepositoryPersistsAndTrimsValues() {
        val ctx = ApplicationProvider.getApplicationContext<EcoBirchApplication>()
        val repo = SettingsRepository(ctx)

        assertFalse(repo.settings.value.privacyPolicyAccepted)
        assertFalse(repo.settings.value.blindMeasurementMode)
        assertFalse(repo.settings.value.enableZakharovScale)

        repo.setPrivacyPolicyAccepted(true)
        repo.updateGeminiConfig(true, "  AIzaSyTestKey123  ")
        repo.setIncludeDemoInExport(true)
        repo.setBlindMeasurementMode(true)
        repo.setEnableZakharovScale(true)

        val reloaded = SettingsRepository(ctx).settings.value
        assertTrue(reloaded.privacyPolicyAccepted)
        assertTrue(reloaded.useCloudGeminiAi)
        assertEquals("AIzaSyTestKey123", reloaded.geminiApiKey)
        assertTrue(reloaded.includeDemoInExport)
        assertTrue(reloaded.blindMeasurementMode)
        assertTrue(reloaded.enableZakharovScale)
    }

    @Test
    fun photoStorageManagerSavesLoadsAndSafelyDeletesOwnedPhotosOnly() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<EcoBirchApplication>()
        val manager = PhotoStorageManager(ctx)

        val bmp = Bitmap.createBitmap(120, 160, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(android.graphics.Color.GREEN)
        val baos = ByteArrayOutputStream()
        bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos)
        val dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        val savedPath = manager.saveBase64DataUrl(dataUrl)
        assertTrue(File(savedPath).exists())

        val loaded = manager.loadBitmapFromPath(savedPath, 200)
        assertNotNull(loaded)

        // Create an unrelated file outside leaf_photos and verify deletePhotoIfOwned ignores it
        val foreignFile = File(ctx.filesDir, "foreign_keep.txt").apply { writeText("keep") }
        manager.deletePhotoIfOwned(foreignFile.absolutePath)
        assertTrue(foreignFile.exists())
        foreignFile.delete()

        // Owned photo gets deleted cleanly
        manager.deletePhotoIfOwned(savedPath)
        assertFalse(File(savedPath).exists())
        assertNull(manager.loadBitmapFromPath(savedPath))
    }
}
