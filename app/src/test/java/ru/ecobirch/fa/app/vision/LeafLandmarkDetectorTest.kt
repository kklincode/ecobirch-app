package ru.ecobirch.fa.app.vision

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.ecobirch.fa.app.EcoBirchApplication
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.DetectionSource
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = EcoBirchApplication::class)
class LeafLandmarkDetectorTest {

    @Test
    fun detectsGreenSymmetricBirchLeafOnWhitePaper() {
        val w = 320
        val h = 400
        val pixels = IntArray(w * h) { Color.WHITE }
        val leafColor = Color.rgb(68, 138, 52)
        val stemColor = Color.rgb(74, 130, 50)

        // Draw symmetric ovate/diamond birch leaf directly into pixel buffer
        // Tip at y = 0.12*h, widest at y = 0.50*h (half-width 0.34*w), blade base at y = 0.84*h
        val tipY = (h * 0.12f).toInt()
        val baseY = (h * 0.84f).toInt()
        val midY = (h * 0.50f).toInt()
        val cx = w / 2f

        for (y in tipY..baseY) {
            val halfW = if (y <= midY) {
                val t = (y - tipY).toFloat() / (midY - tipY).toFloat()
                w * 0.34f * t.pow(0.85f)
            } else {
                val t = (baseY - y).toFloat() / (baseY - midY).toFloat()
                w * 0.34f * sqrt(t)
            }
            val xStart = (cx - halfW).toInt().coerceIn(0, w - 1)
            val xEnd = (cx + halfW).toInt().coerceIn(0, w - 1)
            for (x in xStart..xEnd) {
                pixels[y * w + x] = leafColor
            }
        }

        // Thin petiole tail from baseY to 0.93*h
        val stemEndY = (h * 0.93f).toInt()
        for (y in baseY..stemEndY) {
            for (x in (w / 2 - 2)..(w / 2 + 2)) {
                pixels[y * w + x] = stemColor
            }
        }

        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(pixels, 0, w, 0, 0, w, h)

        val result = LeafLandmarkDetector.detectLandmarks(bmp)
        assertEquals(result.statusMessage, DetectionSource.LOCAL_CONTOUR, result.source)
        assertTrue(result.confidence > 0.5f)
        assertTrue(result.points.tip.y < result.points.leftWidthMax.y)
        assertTrue(result.points.leftWidthMax.y < result.points.base.y)
        assertTrue(result.points.leftWidthMax.x < 40f)
        assertTrue(result.points.rightWidthMax.x > 60f)
        assertTrue(abs(result.points.tip.x - 50f) < 6f)

        val traits = LeafBioindicationEngine.calculateLeafTraits(result.points, w.toFloat() / h.toFloat())
        val z = LeafBioindicationEngine.calculateZScore(traits)
        assertTrue("Expected low Z-score for symmetric synthetic leaf, got $z", z < 0.045)
    }

    @Test
    fun blankWhiteSheetFallsBackSafelyToManualDefault() {
        val bmp = Bitmap.createBitmap(240, 300, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.WHITE)
        val result = LeafLandmarkDetector.detectLandmarks(bmp)
        assertEquals(DetectionSource.MANUAL_DEFAULT, result.source)
        assertEquals(0f, result.confidence)
    }

    @Test
    fun tinyBitmapFallsBackWithoutCrash() {
        val bmp = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val result = LeafLandmarkDetector.detectLandmarks(bmp)
        assertEquals(DetectionSource.MANUAL_DEFAULT, result.source)
    }
}
