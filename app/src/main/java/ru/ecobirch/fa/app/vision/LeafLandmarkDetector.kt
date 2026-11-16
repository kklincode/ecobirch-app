package ru.ecobirch.fa.app.vision

import android.graphics.Bitmap
import android.graphics.Color
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class LeafDetectionResult(
    val points: LeafPoints,
    val confidence: Float,
    val source: DetectionSource,
    val statusMessage: String
)

/**
 * Pure on-device morphological leaf contour & landmark detector (zero network required).
 * Works best on a green/yellow-green birch leaf photographed against a bright/white sheet (A4 paper).
 */
object LeafLandmarkDetector {

    private const val WORK_MAX_DIM = 220

    fun detectLandmarks(bitmap: Bitmap): LeafDetectionResult {
        val wSrc = bitmap.width
        val hSrc = bitmap.height
        if (wSrc < 16 || hSrc < 16) {
            return fallback("Изображение слишком маленькое — установлены точки по умолчанию")
        }

        val scale = WORK_MAX_DIM.toFloat() / max(wSrc, hSrc).toFloat()
        val w = max(24, (wSrc * scale).roundToInt())
        val h = max(24, (hSrc * scale).roundToInt())
        val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)

        try {
            val pixels = IntArray(w * h)
            scaled.getPixels(pixels, 0, w, 0, 0, w, h)

            // 1. Estimate background brightness/color from image border pixels
            var bgLumSum = 0.0
            var bgCount = 0
            for (x in 0 until w) {
                for (y in listOf(0, 1, h - 2, h - 1)) {
                    val c = pixels[y * w + x]
                    bgLumSum += 0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)
                    bgCount++
                }
            }
            val avgBgLum = if (bgCount > 0) bgLumSum / bgCount else 200.0

            // 2. Segment leaf blade pixels using Excess Green (ExG), HSV hue/saturation, and contrast against light paper
            val mask = BooleanArray(w * h)
            for (i in pixels.indices) {
                val c = pixels[i]
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)

                val exG = 2 * g - r - b
                val lum = 0.299 * r + 0.587 * g + 0.114 * b
                val maxRgb = max(r, max(g, b))
                val minRgb = min(r, min(g, b))
                val sat = if (maxRgb == 0) 0f else (maxRgb - minRgb).toFloat() / maxRgb.toFloat()

                val isGreenLeaf = exG > 14 && g > b
                val isYellowAutumnLeaf = sat > 0.22f && g >= b && (r + g) > 2 * b + 20 && lum < 235
                val isDarkOnBrightPaper = avgBgLum > 165.0 && lum < avgBgLum - 42.0 && sat > 0.10f

                mask[i] = isGreenLeaf || isYellowAutumnLeaf || isDarkOnBrightPaper
            }

            // 3. Keep largest connected foreground component to reject background specks
            val largestMask = extractLargestComponent(mask, w, h)
            val area = largestMask.count { it }
            val areaRatio = area.toFloat() / (w * h).toFloat()

            if (areaRatio < 0.03f || areaRatio > 0.90f) {
                return fallback("Контур листа не выделен автоматически — калибруйте точки вручную")
            }

            // 4. Analyze row-by-row bounding profile to separate thin petiole from leaf blade
            val rowLeft = IntArray(h) { -1 }
            val rowRight = IntArray(h) { -1 }
            val rowWidth = IntArray(h) { 0 }
            var maxRowWidth = 0
            var maxWidthRow = h / 2

            for (y in 0 until h) {
                var minX = w
                var maxX = -1
                for (x in 0 until w) {
                    if (largestMask[y * w + x]) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                    }
                }
                if (maxX >= minX) {
                    rowLeft[y] = minX
                    rowRight[y] = maxX
                    val rw = maxX - minX + 1
                    rowWidth[y] = rw
                    if (rw > maxRowWidth) {
                        maxRowWidth = rw
                        maxWidthRow = y
                    }
                }
            }

            if (maxRowWidth < w * 0.12f) {
                return fallback("Слишком узкий контур листа — калибруйте точки вручную")
            }

            // Find top apex (tip) and bottom blade junction (base) avoiding thin petiole tail
            val topRows = (0 until h).filter { rowWidth[it] >= 2 }
            if (topRows.size < 10) {
                return fallback("Недостаточно данных контура — проверьте точки вручную")
            }
            val tipY = topRows.first()
            val tipX = (rowLeft[tipY] + rowRight[tipY]) / 2f

            // Walk down from maxWidthRow to detect where blade narrows below 16% of maxWidth (petiole transition)
            var baseY = topRows.last()
            val bladeCutoffWidth = max(4, (maxRowWidth * 0.16f).roundToInt())
            for (y in maxWidthRow until h) {
                if (rowWidth[y] < bladeCutoffWidth) {
                    baseY = max(maxWidthRow + 4, y - 1)
                    break
                }
            }
            val baseX = if (rowLeft[baseY] >= 0) {
                (rowLeft[baseY] + rowRight[baseY]) / 2f
            } else {
                (rowLeft[maxWidthRow] + rowRight[maxWidthRow]) / 2f
            }

            val bladeHeight = (baseY - tipY).toFloat()
            if (bladeHeight < h * 0.18f) {
                return fallback("Контур фрагментирован — расставьте точки вручную")
            }

            // Helper to interpolate midrib X at any vertical row y
            fun midribXAt(y: Float): Float {
                val t = ((y - tipY) / bladeHeight).coerceIn(0f, 1f)
                return tipX + t * (baseX - tipX)
            }

            // 5. Find left & right max perpendicular width points from the midrib line
            var bestLeftDist = -1f
            var leftMaxPt = LeafPoint(20f, 55f)
            var bestRightDist = -1f
            var rightMaxPt = LeafPoint(80f, 55f)

            for (y in (tipY + (bladeHeight * 0.12f).toInt())..(baseY - (bladeHeight * 0.05f).toInt())) {
                if (rowWidth[y] <= 0) continue
                val mx = midribXAt(y.toFloat())
                val lDist = mx - rowLeft[y]
                if (lDist > bestLeftDist) {
                    bestLeftDist = lDist
                    leftMaxPt = toPercent(rowLeft[y].toFloat(), y.toFloat(), w, h)
                }
                val rDist = rowRight[y] - mx
                if (rDist > bestRightDist) {
                    bestRightDist = rDist
                    rightMaxPt = toPercent(rowRight[y].toFloat(), y.toFloat(), w, h)
                }
            }

            // 6. Place 1st and 2nd secondary vein bases along lower-mid blade & trace margins at ~48 deg
            val v1BaseY = baseY - bladeHeight * 0.24f
            val v2BaseY = baseY - bladeHeight * 0.48f

            val leftV1Base = toPercent(midribXAt(v1BaseY), v1BaseY, w, h)
            val rightV1Base = toPercent(midribXAt(v1BaseY), v1BaseY, w, h)
            val leftV2Base = toPercent(midribXAt(v2BaseY), v2BaseY, w, h)
            val rightV2Base = toPercent(midribXAt(v2BaseY), v2BaseY, w, h)

            // Trace along ~48 degree upward diagonal from vein base to hit the detected leaf contour edge
            val leftV1End = traceToMargin(largestMask, w, h, midribXAt(v1BaseY), v1BaseY, -0.74f, -0.67f)
            val rightV1End = traceToMargin(largestMask, w, h, midribXAt(v1BaseY), v1BaseY, 0.74f, -0.67f)
            val leftV2End = traceToMargin(largestMask, w, h, midribXAt(v2BaseY), v2BaseY, -0.68f, -0.73f)
            val rightV2End = traceToMargin(largestMask, w, h, midribXAt(v2BaseY), v2BaseY, 0.68f, -0.73f)

            val detected = LeafPoints(
                base = toPercent(baseX, baseY.toFloat(), w, h),
                tip = toPercent(tipX, tipY.toFloat(), w, h),
                leftVein1Base = leftV1Base,
                leftVein1End = leftV1End,
                leftVein2Base = leftV2Base,
                leftVein2End = leftV2End,
                leftWidthMax = leftMaxPt,
                rightVein1Base = rightV1Base,
                rightVein1End = rightV1End,
                rightVein2Base = rightV2Base,
                rightVein2End = rightV2End,
                rightWidthMax = rightMaxPt
            )

            return LeafDetectionResult(
                points = detected,
                confidence = min(0.92f, 0.55f + areaRatio),
                source = DetectionSource.LOCAL_CONTOUR,
                statusMessage = "Контур листа найден локально. Уточните жилки с помощью лупы."
            )
        } finally {
            if (scaled !== bitmap) scaled.recycle()
        }
    }

    private fun traceToMargin(
        mask: BooleanArray,
        w: Int,
        h: Int,
        startX: Float,
        startY: Float,
        dirX: Float,
        dirY: Float
    ): LeafPoint {
        var lastInsideX = startX
        var lastInsideY = startY
        var missStreak = 0
        val maxSteps = max(w, h)
        for (step in 1..maxSteps) {
            val fx = startX + dirX * step
            val fy = startY + dirY * step
            val ix = fx.roundToInt()
            val iy = fy.roundToInt()
            if (ix < 1 || ix >= w - 1 || iy < 1 || iy >= h - 1) break
            if (mask[iy * w + ix]) {
                lastInsideX = fx
                lastInsideY = fy
                missStreak = 0
            } else {
                missStreak++
                if (missStreak >= 3) break
            }
        }
        // Fallback if ray never left start (noisy edge)
        if (abs(lastInsideX - startX) < 3f && abs(lastInsideY - startY) < 3f) {
            lastInsideX = (startX + dirX * w * 0.25f).coerceIn(2f, (w - 2).toFloat())
            lastInsideY = (startY + dirY * h * 0.20f).coerceIn(2f, (h - 2).toFloat())
        }
        return toPercent(lastInsideX, lastInsideY, w, h)
    }

    private fun toPercent(x: Float, y: Float, w: Int, h: Int): LeafPoint {
        return LeafPoint(
            x = ((x / w.toFloat()) * 100f).coerceIn(2f, 98f),
            y = ((y / h.toFloat()) * 100f).coerceIn(2f, 98f)
        )
    }

    private fun extractLargestComponent(mask: BooleanArray, w: Int, h: Int): BooleanArray {
        val visited = BooleanArray(w * h)
        val queue = IntArray(w * h)
        var bestComponent: IntArray? = null
        var bestSize = 0

        for (start in mask.indices) {
            if (!mask[start] || visited[start]) continue
            var head = 0
            var tail = 0
            queue[tail++] = start
            visited[start] = true

            while (head < tail) {
                val curr = queue[head++]
                val cx = curr % w
                val cy = curr / w

                if (cx > 0) {
                    val n = curr - 1
                    if (mask[n] && !visited[n]) { visited[n] = true; queue[tail++] = n }
                }
                if (cx + 1 < w) {
                    val n = curr + 1
                    if (mask[n] && !visited[n]) { visited[n] = true; queue[tail++] = n }
                }
                if (cy > 0) {
                    val n = curr - w
                    if (mask[n] && !visited[n]) { visited[n] = true; queue[tail++] = n }
                }
                if (cy + 1 < h) {
                    val n = curr + w
                    if (mask[n] && !visited[n]) { visited[n] = true; queue[tail++] = n }
                }
            }

            if (tail > bestSize) {
                bestSize = tail
                bestComponent = queue.copyOf(tail)
            }
        }

        val out = BooleanArray(w * h)
        bestComponent?.forEach { idx -> out[idx] = true }
        return out
    }

    private fun fallback(msg: String): LeafDetectionResult {
        return LeafDetectionResult(
            points = LeafBioindicationEngine.getDefaultLeafPoints(),
            confidence = 0f,
            source = DetectionSource.MANUAL_DEFAULT,
            statusMessage = msg
        )
    }
}
