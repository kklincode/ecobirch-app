package ru.ecobirch.fa.app.domain.math

import ru.ecobirch.fa.app.domain.model.EnvironmentalRating
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.domain.model.LeafTraits
import ru.ecobirch.fa.app.domain.model.TerritoryStatistics
import ru.ecobirch.fa.app.domain.model.TraitValues
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure scientific morphometric math engine based on V.M. Zakharov's methodology
 * for Betula pendula (Silver Birch) fluctuating asymmetry bioindication.
 *
 * Audit fixes implemented:
 * 1. Classic Zakharov thresholds: < 0.040 / 0.040–0.044 / 0.045–0.049 / 0.050–0.054 / > 0.054.
 * 2. Aspect-ratio-aware isotropic geometry for accurate lengths and angles on any photo ratio.
 * 3. Honest scale: normalized to midrib axis length (100% = base->tip) unless explicitly calibrated in mm.
 * 4. Trait 5 angle computed relative to the consistent central midrib vector (base -> tip).
 * 5. Full sample statistics (n, SD, SE, 95% CI via Student's t) with reliability flag (n >= 10).
 */
object LeafBioindicationEngine {

    private const val EPSILON = 1e-9

    // Two-tailed t-distribution critical values for alpha = 0.05 (95% CI), indexed by df = n - 1
    private val T_CRITICAL_95 = doubleArrayOf(
        0.0,     // df = 0 unused
        12.706,  // df = 1
        4.303,   // df = 2
        3.182,   // df = 3
        2.776,   // df = 4
        2.571,   // df = 5
        2.447,   // df = 6
        2.365,   // df = 7
        2.306,   // df = 8
        2.262,   // df = 9
        2.228,   // df = 10
        2.201,   // df = 11
        2.179,   // df = 12
        2.160,   // df = 13
        2.145,   // df = 14
        2.131,   // df = 15
        2.120,   // df = 16
        2.110,   // df = 17
        2.101,   // df = 18
        2.093,   // df = 19
        2.086,   // df = 20
        2.080,   // df = 21
        2.074,   // df = 22
        2.069,   // df = 23
        2.064,   // df = 24
        2.060,   // df = 25
        2.056,   // df = 26
        2.052,   // df = 27
        2.048,   // df = 28
        2.045,   // df = 29
        2.042    // df = 30
    )

    private fun toIsoX(point: LeafPoint, aspectRatio: Float): Double {
        val safeRatio = if (aspectRatio.isFinite() && aspectRatio > 0.05f) aspectRatio.toDouble() else 1.0
        return point.x.toDouble() * safeRatio
    }

    fun getDistance(p1: LeafPoint, p2: LeafPoint, aspectRatio: Float = 1f): Double {
        val dx = toIsoX(p2, aspectRatio) - toIsoX(p1, aspectRatio)
        val dy = (p2.y - p1.y).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    fun getPointToLineDistance(
        p0: LeafPoint,
        lineStart: LeafPoint,
        lineEnd: LeafPoint,
        aspectRatio: Float = 1f
    ): Double {
        val x0 = toIsoX(p0, aspectRatio)
        val y0 = p0.y.toDouble()
        val x1 = toIsoX(lineStart, aspectRatio)
        val y1 = lineStart.y.toDouble()
        val x2 = toIsoX(lineEnd, aspectRatio)
        val y2 = lineEnd.y.toDouble()

        val numerator = abs((y2 - y1) * x0 - (x2 - x1) * y0 + x2 * y1 - y2 * x1)
        val denominator = sqrt((y2 - y1).pow(2) + (x2 - x1).pow(2))
        return if (denominator < EPSILON) 0.0 else numerator / denominator
    }

    /**
     * Orthogonal projection of [point] onto the infinite line passing through [lineStart] and [lineEnd].
     * Used for visually rendering the true perpendicular width segment on the canvas.
     */
    fun projectPointOntoLine(
        point: LeafPoint,
        lineStart: LeafPoint,
        lineEnd: LeafPoint,
        aspectRatio: Float = 1f
    ): LeafPoint {
        val safeRatio = if (aspectRatio.isFinite() && aspectRatio > 0.05f) aspectRatio.toDouble() else 1.0
        val ax = lineStart.x.toDouble() * safeRatio
        val ay = lineStart.y.toDouble()
        val bx = lineEnd.x.toDouble() * safeRatio
        val by = lineEnd.y.toDouble()
        val px = point.x.toDouble() * safeRatio
        val py = point.y.toDouble()

        val abx = bx - ax
        val aby = by - ay
        val abLenSq = abx * abx + aby * aby
        if (abLenSq < EPSILON) return lineStart

        val t = ((px - ax) * abx + (py - ay) * aby) / abLenSq
        val projX = (ax + t * abx) / safeRatio
        val projY = ay + t * aby
        return LeafPoint(projX.toFloat(), projY.toFloat())
    }

    /**
     * Computes angle in degrees (0..180) between the central midrib vector (base -> tip)
     * and the secondary vein vector (veinBase -> veinEnd).
     * Measuring both left and right sides against the identical midrib axis avoids distance-to-tip distortion.
     */
    fun getAngleWithMainVein(
        midribBase: LeafPoint,
        midribTip: LeafPoint,
        veinBase: LeafPoint,
        veinEnd: LeafPoint,
        aspectRatio: Float = 1f
    ): Double {
        val ax = toIsoX(midribTip, aspectRatio) - toIsoX(midribBase, aspectRatio)
        val ay = (midribTip.y - midribBase.y).toDouble()

        val bx = toIsoX(veinEnd, aspectRatio) - toIsoX(veinBase, aspectRatio)
        val by = (veinEnd.y - veinBase.y).toDouble()

        val magA = sqrt(ax * ax + ay * ay)
        val magB = sqrt(bx * bx + by * by)
        if (magA < EPSILON || magB < EPSILON) return 0.0

        val cosTheta = ((ax * bx + ay * by) / (magA * magB)).coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(cosTheta))
    }

    fun computeRelativeAsymmetry(xLeft: Double, xRight: Double): TraitValues {
        val safeL = if (xLeft.isFinite() && xLeft > 0.0) xLeft else 0.0
        val safeR = if (xRight.isFinite() && xRight > 0.0) xRight else 0.0
        val sum = safeL + safeR
        val y = if (sum < EPSILON) 0.0 else (abs(safeL - safeR) / sum).coerceIn(0.0, 1.0)
        return TraitValues(xLeft = safeL, xRight = safeR, yDiff = y)
    }

    /**
     * Computes all 5 bilateral traits.
     * When [axisReferenceLengthMm] is null, linear traits X1..X4 are expressed in normalized units
     * (% of midrib axis length |base->tip|). When non-null (>0), values are in millimeters (mm).
     */
    fun calculateLeafTraits(
        points: LeafPoints,
        aspectRatio: Float = 0.8f,
        axisReferenceLengthMm: Double? = null
    ): LeafTraits {
        val midribLength = getDistance(points.base, points.tip, aspectRatio)
        val linearScale = when {
            midribLength < EPSILON -> 0.0
            axisReferenceLengthMm != null && axisReferenceLengthMm > 0.0 -> axisReferenceLengthMm / midribLength
            else -> 100.0 / midribLength
        }

        // Trait 1: Perpendicular max blade width from central midrib
        val xLeft1 = getPointToLineDistance(points.leftWidthMax, points.base, points.tip, aspectRatio) * linearScale
        val xRight1 = getPointToLineDistance(points.rightWidthMax, points.base, points.tip, aspectRatio) * linearScale

        // Trait 2: Length of 2nd secondary vein
        val xLeft2 = getDistance(points.leftVein2Base, points.leftVein2End, aspectRatio) * linearScale
        val xRight2 = getDistance(points.rightVein2Base, points.rightVein2End, aspectRatio) * linearScale

        // Trait 3: Distance between bases of 1st and 2nd secondary veins
        val xLeft3 = getDistance(points.leftVein1Base, points.leftVein2Base, aspectRatio) * linearScale
        val xRight3 = getDistance(points.rightVein1Base, points.rightVein2Base, aspectRatio) * linearScale

        // Trait 4: Distance between marginal ends of 1st and 2nd secondary veins
        val xLeft4 = getDistance(points.leftVein1End, points.leftVein2End, aspectRatio) * linearScale
        val xRight4 = getDistance(points.rightVein1End, points.rightVein2End, aspectRatio) * linearScale

        // Trait 5: Angle (degrees) between main central vein (base -> tip) and 2nd secondary vein
        val xLeft5 = getAngleWithMainVein(points.base, points.tip, points.leftVein2Base, points.leftVein2End, aspectRatio)
        val xRight5 = getAngleWithMainVein(points.base, points.tip, points.rightVein2Base, points.rightVein2End, aspectRatio)

        return LeafTraits(
            t1Width = computeRelativeAsymmetry(xLeft1, xRight1),
            t2VeinLen = computeRelativeAsymmetry(xLeft2, xRight2),
            t3VeinDist = computeRelativeAsymmetry(xLeft3, xRight3),
            t4EndDist = computeRelativeAsymmetry(xLeft4, xRight4),
            t5Angle = computeRelativeAsymmetry(xLeft5, xRight5)
        )
    }

    fun calculateZScore(traits: LeafTraits): Double {
        return (
            traits.t1Width.yDiff +
                traits.t2VeinLen.yDiff +
                traits.t3VeinDist.yDiff +
                traits.t4EndDist.yDiff +
                traits.t5Angle.yDiff
            ) / 5.0
    }

    /**
     * Classic V.M. Zakharov (2000/2003) 5-point environmental quality scale for Betula pendula:
     *   I   < 0.040
     *   II  0.040 – 0.044
     *   III 0.045 – 0.049
     *   IV  0.050 – 0.054
     *   V   > 0.054
     */
    fun getEnvironmentalRating(fa: Double): EnvironmentalRating {
        val safeFa = if (fa.isFinite() && fa >= 0.0) fa else 0.0
        return when {
            safeFa < 0.040 -> EnvironmentalRating(
                fa = safeFa,
                score = 1,
                status = "Условно нормальное",
                rangeLabel = "< 0.040",
                hexColorLong = 0xFF10B981,
                description = "Чистая среда. Стабильное развитие листовой пластины без заметного антропогенного стресса."
            )
            safeFa < 0.045 -> EnvironmentalRating(
                fa = safeFa,
                score = 2,
                status = "Начальные отклонения",
                rangeLabel = "0.040 – 0.044",
                hexColorLong = 0xFFEAB308,
                description = "Слабое антропогенное воздействие. Незначительные начальные отклонения развития от нормы."
            )
            safeFa < 0.050 -> EnvironmentalRating(
                fa = safeFa,
                score = 3,
                status = "Средний уровень загрязнения",
                rangeLabel = "0.045 – 0.049",
                hexColorLong = 0xFFF59E0B,
                description = "Умеренная антропогенная нагрузка. Качество атмосферной среды заметно снижено."
            )
            safeFa <= 0.054 -> EnvironmentalRating(
                fa = safeFa,
                score = 4,
                status = "Существенные отклонения",
                rangeLabel = "0.050 – 0.054",
                hexColorLong = 0xFFF43F5E,
                description = "Высокая стрессовая нагрузка. Существенное загрязнение среды (характерно для магистралей и промзон)."
            )
            else -> EnvironmentalRating(
                fa = safeFa,
                score = 5,
                status = "Критическое состояние",
                rangeLabel = "> 0.054",
                hexColorLong = 0xFF991B1B,
                description = "Критическая экологическая нагрузка. Сильное угнетение стабильности развития растений."
            )
        }
    }

    fun calculateTerritoryStatistics(leaves: List<LeafAnalysis>): TerritoryStatistics {
        val n = leaves.size
        if (n == 0) {
            return TerritoryStatistics(
                sampleCount = 0,
                meanFA = 0.0,
                standardDeviation = 0.0,
                standardError = 0.0,
                ci95HalfWidth = 0.0,
                minZ = 0.0,
                maxZ = 0.0,
                isReliable = false,
                indicativeRating = null
            )
        }

        val zValues = leaves.map { if (it.zScore.isFinite()) it.zScore else 0.0 }
        val mean = zValues.sum() / n
        val minZ = zValues.minOrNull() ?: mean
        val maxZ = zValues.maxOrNull() ?: mean

        val sd: Double
        val se: Double
        val ci95: Double
        if (n >= 2) {
            val variance = zValues.sumOf { (it - mean).pow(2) } / (n - 1)
            sd = sqrt(variance)
            se = sd / sqrt(n.toDouble())
            val df = n - 1
            val tCrit = if (df < T_CRITICAL_95.size) T_CRITICAL_95[df] else 1.96
            ci95 = tCrit * se
        } else {
            sd = 0.0
            se = 0.0
            ci95 = 0.0
        }

        return TerritoryStatistics(
            sampleCount = n,
            meanFA = mean,
            standardDeviation = sd,
            standardError = se,
            ci95HalfWidth = ci95,
            minZ = minZ,
            maxZ = maxZ,
            isReliable = n >= TerritoryStatistics.RELIABLE_MIN_SAMPLES,
            indicativeRating = getEnvironmentalRating(mean)
        )
    }

    fun getDefaultLeafPoints(): LeafPoints = LeafPoints(
        base = LeafPoint(50f, 88f),
        tip = LeafPoint(50f, 15f),
        leftVein1Base = LeafPoint(50f, 70f),
        leftVein1End = LeafPoint(22f, 52f),
        leftVein2Base = LeafPoint(50f, 51f),
        leftVein2End = LeafPoint(26f, 32f),
        leftWidthMax = LeafPoint(16f, 56f),
        rightVein1Base = LeafPoint(50f, 70f),
        rightVein1End = LeafPoint(78f, 52f),
        rightVein2Base = LeafPoint(50f, 51f),
        rightVein2End = LeafPoint(74f, 32f),
        rightWidthMax = LeafPoint(84f, 56f)
    )
}
