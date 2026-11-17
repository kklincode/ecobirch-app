package ru.ecobirch.fa.app.domain.math

import ru.ecobirch.fa.app.domain.model.AsymmetryPattern
import ru.ecobirch.fa.app.domain.model.EnvironmentalRating
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.domain.model.LeafTraits
import ru.ecobirch.fa.app.domain.model.MeasurementErrorReport
import ru.ecobirch.fa.app.domain.model.TerritoryStatistics
import ru.ecobirch.fa.app.domain.model.TraitDiagnostics
import ru.ecobirch.fa.app.domain.model.TraitValues
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Pure scientific morphometric & biostatistical engine for Betula pendula leaf asymmetry.
 *
 * Implements:
 * 1. Isotropic aspect-ratio geometry & consistent midrib-relative angles (Y1..Y5, Z).
 * 2. Preservation of signed relative asymmetry A_si = (X_L - X_R)/(X_L + X_R) alongside |A_si|.
 * 3. Pre-interpretation statistical screening (Palmer & Strobeck 1986; Graham 2021):
 *    - Directional Asymmetry (DA) via one-sample t-test on signed A_si
 *    - Antisymmetry (AS) via excess kurtosis screening
 *    - Size dependence via Pearson correlation |A_si| vs midrib length
 *    - Measurement Error (ME%) via paired blind repeat measurements (Kozlov 2015, 2025)
 * 4. Hierarchical tree-level aggregation (Site -> Tree -> Leaf) to prevent pseudoreplication.
 * 5. Optional Zakharov 5-point reference classification (<0.040 / 0.040–0.044 / 0.045–0.049 / 0.050–0.054 / >0.054).
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

    private fun tCritical95(df: Int): Double = when {
        df <= 0 -> 0.0
        df < T_CRITICAL_95.size -> T_CRITICAL_95[df]
        else -> 1.96
    }

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

        val xLeft1 = getPointToLineDistance(points.leftWidthMax, points.base, points.tip, aspectRatio) * linearScale
        val xRight1 = getPointToLineDistance(points.rightWidthMax, points.base, points.tip, aspectRatio) * linearScale

        val xLeft2 = getDistance(points.leftVein2Base, points.leftVein2End, aspectRatio) * linearScale
        val xRight2 = getDistance(points.rightVein2Base, points.rightVein2End, aspectRatio) * linearScale

        val xLeft3 = getDistance(points.leftVein1Base, points.leftVein2Base, aspectRatio) * linearScale
        val xRight3 = getDistance(points.rightVein1Base, points.rightVein2Base, aspectRatio) * linearScale

        val xLeft4 = getDistance(points.leftVein1End, points.leftVein2End, aspectRatio) * linearScale
        val xRight4 = getDistance(points.rightVein1End, points.rightVein2End, aspectRatio) * linearScale

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
     * Reference 5-point V.M. Zakharov (2000/2003) scale for Betula pendula.
     * Should be treated as an optional reference classification contingent on DA/AS/ME validation
     * and local environmental context (soil, climate, herbivory).
     */
    fun getEnvironmentalRating(fa: Double): EnvironmentalRating {
        val safeFa = if (fa.isFinite() && fa >= 0.0) fa else 0.0
        return when {
            safeFa < 0.040 -> EnvironmentalRating(
                fa = safeFa,
                score = 1,
                status = "Низкая асимметрия",
                rangeLabel = "< 0.040",
                hexColorLong = 0xFF10B981,
                description = "Диапазон условной нормы по шкале Захарова. Требуется подтверждение отсутствия DA/AS и учёт условий участка."
            )
            safeFa < 0.045 -> EnvironmentalRating(
                fa = safeFa,
                score = 2,
                status = "Небольшие отклонения",
                rangeLabel = "0.040 – 0.044",
                hexColorLong = 0xFFEAB308,
                description = "Начальный сдвиг индекса. Может отражать как антропогенные, так и почвенно-климатические факторы."
            )
            safeFa < 0.050 -> EnvironmentalRating(
                fa = safeFa,
                score = 3,
                status = "Умеренные отклонения",
                rangeLabel = "0.045 – 0.049",
                hexColorLong = 0xFFF59E0B,
                description = "Умеренное повышение морфологической асимметрии. Оцените вклад DA, ошибки измерения и эдафических факторов."
            )
            safeFa <= 0.054 -> EnvironmentalRating(
                fa = safeFa,
                score = 4,
                status = "Выраженные отклонения",
                rangeLabel = "0.050 – 0.054",
                hexColorLong = 0xFFF43F5E,
                description = "Выраженная асимметрия пластины. Не приравнивайте автоматически к загрязнению воздуха без анализа почвы и контроля."
            )
            else -> EnvironmentalRating(
                fa = safeFa,
                score = 5,
                status = "Высокая асимметрия",
                rangeLabel = "> 0.054",
                hexColorLong = 0xFF991B1B,
                description = "Высокое отклонение индекса по Захарову. Проверьте выборку на направленную асимметрию, повреждения и условия произрастания."
            )
        }
    }

    fun calculateTerritoryStatistics(leaves: List<LeafAnalysis>): TerritoryStatistics {
        val primaryLeaves = leaves.filter { it.replicateOfLeafId.isNullOrBlank() }
        val replicates = leaves.filter { !it.replicateOfLeafId.isNullOrBlank() }
        val n = primaryLeaves.size

        if (n == 0) {
            return TerritoryStatistics(
                sampleCount = 0,
                primaryLeafCount = 0,
                treeCount = 0,
                replicateCount = replicates.size,
                meanFA = 0.0,
                treeMeanFA = 0.0,
                standardDeviation = 0.0,
                standardError = 0.0,
                treeLevelSE = 0.0,
                ci95HalfWidth = 0.0,
                treeLevelCi95HalfWidth = 0.0,
                minZ = 0.0,
                maxZ = 0.0,
                isReliable = false,
                isHierarchicallyReliable = false,
                faValidationStatus = "Нет первичных замеров",
                indicativeRating = null
            )
        }

        val zValues = primaryLeaves.map { if (it.zScore.isFinite()) it.zScore else 0.0 }
        val mean = zValues.sum() / n
        val minZ = zValues.minOrNull() ?: mean
        val maxZ = zValues.maxOrNull() ?: mean

        val leafVariance: Double
        val sd: Double
        val se: Double
        val ci95: Double
        if (n >= 2) {
            leafVariance = zValues.sumOf { (it - mean).pow(2) } / (n - 1)
            sd = sqrt(leafVariance)
            se = sd / sqrt(n.toDouble())
            ci95 = tCritical95(n - 1) * se
        } else {
            leafVariance = 0.0
            sd = 0.0
            se = 0.0
            ci95 = 0.0
        }

        // Hierarchical aggregation by treeId (prevents pseudoreplication across leaves of one tree)
        val treeMeans = primaryLeaves
            .groupBy { it.treeId.ifBlank { "tree_1" } }
            .values
            .map { treeLeaves ->
                treeLeaves.map { if (it.zScore.isFinite()) it.zScore else 0.0 }.average()
            }
        val treeCount = treeMeans.size
        val treeMeanFA = treeMeans.average()
        val treeLevelSE: Double
        val treeLevelCi95: Double
        if (treeCount >= 2) {
            val treeVar = treeMeans.sumOf { (it - treeMeanFA).pow(2) } / (treeCount - 1)
            treeLevelSE = sqrt(treeVar) / sqrt(treeCount.toDouble())
            treeLevelCi95 = tCritical95(treeCount - 1) * treeLevelSE
        } else {
            treeLevelSE = se
            treeLevelCi95 = ci95
        }

        // Per-trait statistical diagnostics (DA, AS, size dependence)
        val traitDiagnostics = computeTraitDiagnostics(primaryLeaves)
        val hasDA = traitDiagnostics.any { it.hasDirectionalAsymmetry }
        val hasAS = traitDiagnostics.any { it.hasAntisymmetrySuspected }
        val hasSizeDep = traitDiagnostics.any { it.hasSizeDependence }

        // Measurement error (ME%) from blind duplicate measurements
        val meReport = computeMeasurementErrorReport(primaryLeaves, replicates, leafVariance)

        val isReliable = n >= TerritoryStatistics.RELIABLE_MIN_SAMPLES
        val isHierarchicallyReliable = isReliable &&
            treeCount >= TerritoryStatistics.MIN_TREES_RECOMMENDED &&
            !hasDA &&
            !hasAS

        val status = when {
            hasDA -> "Выявлена направленная асимметрия (DA)"
            hasAS -> "Подозрение на антисимметрию (AS)"
            hasSizeDep -> "Зависимость от размера листа"
            !isReliable -> "Предварительная выборка (n < ${TerritoryStatistics.RELIABLE_MIN_SAMPLES})"
            treeCount < TerritoryStatistics.MIN_TREES_RECOMMENDED ->
                "Риск псевдорепликации (< ${TerritoryStatistics.MIN_TREES_RECOMMENDED} деревьев)"
            meReport == null || meReport.pairedLeafCount < 2 ->
                "Требуются повторные замеры (ME)"
            !meReport.isAcceptableME ->
                "Высокая ошибка измерения (ME ≥ 25%)"
            else -> "FA статистически проверена"
        }

        val siteIndex = if (treeCount >= 2) treeMeanFA else mean

        return TerritoryStatistics(
            sampleCount = n,
            primaryLeafCount = n,
            treeCount = treeCount,
            replicateCount = replicates.size,
            meanFA = mean,
            treeMeanFA = treeMeanFA,
            standardDeviation = sd,
            standardError = se,
            treeLevelSE = treeLevelSE,
            ci95HalfWidth = ci95,
            treeLevelCi95HalfWidth = treeLevelCi95,
            minZ = minZ,
            maxZ = maxZ,
            isReliable = isReliable,
            isHierarchicallyReliable = isHierarchicallyReliable,
            hasDirectionalAsymmetry = hasDA,
            hasAntisymmetry = hasAS,
            hasSizeDependence = hasSizeDep,
            traitDiagnostics = traitDiagnostics,
            measurementError = meReport,
            faValidationStatus = status,
            indicativeRating = getEnvironmentalRating(siteIndex)
        )
    }

    fun computeTraitDiagnostics(primaryLeaves: List<LeafAnalysis>): List<TraitDiagnostics> {
        val n = primaryLeaves.size
        val useMmSizes = n > 0 && primaryLeaves.all { (it.axisReferenceLengthMm ?: 0.0) > 0.0 }
        val leafSizes = primaryLeaves.map { leaf ->
            if (useMmSizes) {
                leaf.axisReferenceLengthMm!!
            } else {
                getDistance(leaf.points.base, leaf.points.tip, leaf.aspectRatio)
            }
        }

        val extractors: List<Pair<String, (LeafTraits) -> TraitValues>> = listOf(
            "Y1 · Ширина" to { it.t1Width },
            "Y2 · Длина 2-й жилки" to { it.t2VeinLen },
            "Y3 · Осн. 1–2 жилок" to { it.t3VeinDist },
            "Y4 · Края 1–2 жилок" to { it.t4EndDist },
            "Y5 · Угол 2-й жилки" to { it.t5Angle }
        )

        return extractors.mapIndexed { idx, (label, getter) ->
            val values = primaryLeaves.map { getter(it.traits) }
            val unsignedY = values.map { it.yDiff }
            val signedA = values.map { it.signedRelative }

            val meanUnsigned = if (n > 0) unsignedY.average() else 0.0
            val meanSigned = if (n > 0) signedA.average() else 0.0

            var tStat = 0.0
            var hasDA = false
            var kurtosis = 0.0
            var hasAS = false

            if (n >= 5) {
                val varSigned = signedA.sumOf { (it - meanSigned).pow(2) } / (n - 1)
                val sdSigned = sqrt(varSigned)
                val seSigned = sdSigned / sqrt(n.toDouble())
                if (seSigned > EPSILON) {
                    tStat = abs(meanSigned) / seSigned
                    hasDA = tStat > tCritical95(n - 1)
                }

                // Unbiased Fisher sample excess kurtosis when n >= 10
                if (n >= 10 && sdSigned > EPSILON) {
                    val m4 = signedA.sumOf { ((it - meanSigned) / sdSigned).pow(4) }
                    val nD = n.toDouble()
                    val term1 = (nD * (nD + 1.0)) / ((nD - 1.0) * (nD - 2.0) * (nD - 3.0)) * m4
                    val term2 = (3.0 * (nD - 1.0).pow(2)) / ((nD - 2.0) * (nD - 3.0))
                    kurtosis = term1 - term2
                    hasAS = kurtosis < -1.0
                }
            }

            val rSize = if (n >= 8) pearsonCorrelation(leafSizes, unsignedY) else 0.0
            val hasSizeDep = n >= 8 && abs(rSize) > 0.45

            val pattern = when {
                n < 5 -> AsymmetryPattern.INSUFFICIENT_DATA
                hasDA -> AsymmetryPattern.DIRECTIONAL
                hasAS -> AsymmetryPattern.ANTISYMMETRY
                else -> AsymmetryPattern.FLUCTUATING
            }

            TraitDiagnostics(
                traitIndex = idx + 1,
                label = label,
                sampleCount = n,
                meanUnsignedY = meanUnsigned,
                meanSignedRelative = meanSigned,
                directionalTStat = tStat,
                hasDirectionalAsymmetry = hasDA,
                excessKurtosis = kurtosis,
                hasAntisymmetrySuspected = hasAS,
                sizeCorrelationR = rSize,
                hasSizeDependence = hasSizeDep,
                pattern = pattern
            )
        }
    }

    private fun computeMeasurementErrorReport(
        primaryLeaves: List<LeafAnalysis>,
        replicates: List<LeafAnalysis>,
        primaryVariance: Double
    ): MeasurementErrorReport? {
        if (replicates.isEmpty()) return null
        val byId = primaryLeaves.associateBy { it.id }
        val pairs = replicates.mapNotNull { rep ->
            val orig = rep.replicateOfLeafId?.let { byId[it] }
            if (orig != null && orig.zScore.isFinite() && rep.zScore.isFinite()) {
                orig.zScore to rep.zScore
            } else {
                null
            }
        }
        if (pairs.isEmpty()) return null

        val absDiffs = pairs.map { (z1, z2) -> abs(z1 - z2) }
        val meanAbsDiff = absDiffs.average()
        val sqDiffSum = pairs.sumOf { (z1, z2) -> (z1 - z2).pow(2) }
        val techVariance = sqDiffSum / (2.0 * pairs.size)
        val totalVar = maxOf(primaryVariance, techVariance, 1e-12)
        val mePct = ((techVariance / totalVar) * 100.0).coerceIn(0.0, 100.0)

        return MeasurementErrorReport(
            pairedLeafCount = pairs.size,
            meanAbsoluteDiffZ = meanAbsDiff,
            technicalVarianceME = techVariance,
            sampleVarianceTotal = primaryVariance,
            measurementErrorPercent = mePct,
            isAcceptableME = pairs.size >= 2 && mePct < 25.0
        )
    }

    private fun pearsonCorrelation(xs: List<Double>, ys: List<Double>): Double {
        if (xs.size != ys.size || xs.size < 3) return 0.0
        val meanX = xs.average()
        val meanY = ys.average()
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in xs.indices) {
            val dx = xs[i] - meanX
            val dy = ys[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        val denom = sqrt(denX * denY)
        return if (denom < EPSILON) 0.0 else (num / denom).coerceIn(-1.0, 1.0)
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
