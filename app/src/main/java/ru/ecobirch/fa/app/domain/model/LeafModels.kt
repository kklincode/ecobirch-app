package ru.ecobirch.fa.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LeafPoint(
    val x: Float, // 0f..100f percent of canvas width
    val y: Float  // 0f..100f percent of canvas height
)

enum class LandmarkKey(
    val shortTitle: String,
    val fullTitle: String,
    val group: LandmarkGroup
) {
    BASE("Осн.", "Основание листа (черешок)", LandmarkGroup.AXIS),
    TIP("Верш.", "Вершина листа (апекс)", LandmarkGroup.AXIS),
    LEFT_VEIN_1_BASE("Л1 осн", "Левая жилка 1 — основание", LandmarkGroup.VEIN_BASE),
    LEFT_VEIN_1_END("Л1 край", "Левая жилка 1 — край", LandmarkGroup.VEIN_END),
    LEFT_VEIN_2_BASE("Л2 осн", "Левая жилка 2 — основание", LandmarkGroup.VEIN_BASE),
    LEFT_VEIN_2_END("Л2 край", "Левая жилка 2 — край", LandmarkGroup.VEIN_END),
    LEFT_WIDTH_MAX("Л. шир", "Макс. ширина слева", LandmarkGroup.WIDTH),
    RIGHT_VEIN_1_BASE("П1 осн", "Правая жилка 1 — основание", LandmarkGroup.VEIN_BASE),
    RIGHT_VEIN_1_END("П1 край", "Правая жилка 1 — край", LandmarkGroup.VEIN_END),
    RIGHT_VEIN_2_BASE("П2 осн", "Правая жилка 2 — основание", LandmarkGroup.VEIN_BASE),
    RIGHT_VEIN_2_END("П2 край", "Правая жилка 2 — край", LandmarkGroup.VEIN_END),
    RIGHT_WIDTH_MAX("П. шир", "Макс. ширина справа", LandmarkGroup.WIDTH)
}

enum class LandmarkGroup {
    AXIS,
    VEIN_BASE,
    VEIN_END,
    WIDTH
}

@Serializable
data class LeafPoints(
    val base: LeafPoint,
    val tip: LeafPoint,
    val leftVein1Base: LeafPoint,
    val leftVein1End: LeafPoint,
    val leftVein2Base: LeafPoint,
    val leftVein2End: LeafPoint,
    val leftWidthMax: LeafPoint,
    val rightVein1Base: LeafPoint,
    val rightVein1End: LeafPoint,
    val rightVein2Base: LeafPoint,
    val rightVein2End: LeafPoint,
    val rightWidthMax: LeafPoint
) {
    operator fun get(key: LandmarkKey): LeafPoint = when (key) {
        LandmarkKey.BASE -> base
        LandmarkKey.TIP -> tip
        LandmarkKey.LEFT_VEIN_1_BASE -> leftVein1Base
        LandmarkKey.LEFT_VEIN_1_END -> leftVein1End
        LandmarkKey.LEFT_VEIN_2_BASE -> leftVein2Base
        LandmarkKey.LEFT_VEIN_2_END -> leftVein2End
        LandmarkKey.LEFT_WIDTH_MAX -> leftWidthMax
        LandmarkKey.RIGHT_VEIN_1_BASE -> rightVein1Base
        LandmarkKey.RIGHT_VEIN_1_END -> rightVein1End
        LandmarkKey.RIGHT_VEIN_2_BASE -> rightVein2Base
        LandmarkKey.RIGHT_VEIN_2_END -> rightVein2End
        LandmarkKey.RIGHT_WIDTH_MAX -> rightWidthMax
    }

    fun withUpdated(key: LandmarkKey, point: LeafPoint): LeafPoints {
        val clamped = LeafPoint(
            x = point.x.coerceIn(1f, 99f),
            y = point.y.coerceIn(1f, 99f)
        )
        return when (key) {
            LandmarkKey.BASE -> copy(base = clamped)
            LandmarkKey.TIP -> copy(tip = clamped)
            LandmarkKey.LEFT_VEIN_1_BASE -> copy(leftVein1Base = clamped)
            LandmarkKey.LEFT_VEIN_1_END -> copy(leftVein1End = clamped)
            LandmarkKey.LEFT_VEIN_2_BASE -> copy(leftVein2Base = clamped)
            LandmarkKey.LEFT_VEIN_2_END -> copy(leftVein2End = clamped)
            LandmarkKey.LEFT_WIDTH_MAX -> copy(leftWidthMax = clamped)
            LandmarkKey.RIGHT_VEIN_1_BASE -> copy(rightVein1Base = clamped)
            LandmarkKey.RIGHT_VEIN_1_END -> copy(rightVein1End = clamped)
            LandmarkKey.RIGHT_VEIN_2_BASE -> copy(rightVein2Base = clamped)
            LandmarkKey.RIGHT_VEIN_2_END -> copy(rightVein2End = clamped)
            LandmarkKey.RIGHT_WIDTH_MAX -> copy(rightWidthMax = clamped)
        }
    }
}

@Serializable
data class TraitValues(
    val xLeft: Double,
    val xRight: Double,
    val yDiff: Double
) {
    val signedDiff: Double
        get() = xLeft - xRight

    val signedRelative: Double
        get() {
            val sum = xLeft + xRight
            return if (sum <= 1e-9) 0.0 else (xLeft - xRight) / sum
        }
}

@Serializable
data class LeafTraits(
    val t1Width: TraitValues,
    val t2VeinLen: TraitValues,
    val t3VeinDist: TraitValues,
    val t4EndDist: TraitValues,
    val t5Angle: TraitValues
)

@Serializable
enum class DetectionSource {
    TEMPLATE,
    LOCAL_CONTOUR,
    GEMINI_AI,
    MANUAL_DEFAULT
}

@Serializable
data class LeafAnalysis(
    val id: String,
    val photoUrl: String = "",
    val photoName: String,
    val points: LeafPoints,
    val isTemplate: Boolean = false,
    val traits: LeafTraits,
    val zScore: Double,
    val timestamp: String,
    val aspectRatio: Float = 0.8f,
    val axisReferenceLengthMm: Double? = null,
    val detectionSource: DetectionSource = DetectionSource.MANUAL_DEFAULT,
    val exifLatitude: Double? = null,
    val exifLongitude: Double? = null,
    val treeId: String = "tree_1",
    val treeLabel: String = "Дерево №1",
    val replicateOfLeafId: String? = null,
    val isBlindMeasurement: Boolean = false,
    val leafIntegrityVerified: Boolean = true
)

@Serializable
data class Territory(
    val id: String,
    val name: String,
    val description: String = "",
    val latitude: Double,
    val longitude: Double,
    val leaves: List<LeafAnalysis> = emptyList(),
    val timestamp: String,
    val isDemo: Boolean = false
)

data class EnvironmentalRating(
    val fa: Double,
    val score: Int, // 1..5
    val status: String,
    val rangeLabel: String,
    val hexColorLong: Long,
    val description: String
)

enum class AsymmetryPattern {
    FLUCTUATING,
    DIRECTIONAL,
    ANTISYMMETRY,
    INSUFFICIENT_DATA
}

data class TraitDiagnostics(
    val traitIndex: Int,
    val label: String,
    val sampleCount: Int,
    val meanUnsignedY: Double,
    val meanSignedRelative: Double,
    val directionalTStat: Double,
    val hasDirectionalAsymmetry: Boolean,
    val excessKurtosis: Double,
    val hasAntisymmetrySuspected: Boolean,
    val sizeCorrelationR: Double,
    val hasSizeDependence: Boolean,
    val pattern: AsymmetryPattern
)

data class MeasurementErrorReport(
    val pairedLeafCount: Int,
    val meanAbsoluteDiffZ: Double,
    val technicalVarianceME: Double,
    val sampleVarianceTotal: Double,
    val measurementErrorPercent: Double,
    val isAcceptableME: Boolean
)

data class TerritoryStatistics(
    val sampleCount: Int,
    val primaryLeafCount: Int = sampleCount,
    val treeCount: Int = if (sampleCount > 0) 1 else 0,
    val replicateCount: Int = 0,
    val meanFA: Double,
    val treeMeanFA: Double = meanFA,
    val standardDeviation: Double,
    val standardError: Double,
    val treeLevelSE: Double = standardError,
    val ci95HalfWidth: Double,
    val treeLevelCi95HalfWidth: Double = ci95HalfWidth,
    val minZ: Double,
    val maxZ: Double,
    val isReliable: Boolean, // true when primaryLeafCount >= RELIABLE_MIN_SAMPLES (10)
    val isHierarchicallyReliable: Boolean = false, // true when trees >= 5, n >= 10, no DA/AS
    val hasDirectionalAsymmetry: Boolean = false,
    val hasAntisymmetry: Boolean = false,
    val hasSizeDependence: Boolean = false,
    val traitDiagnostics: List<TraitDiagnostics> = emptyList(),
    val measurementError: MeasurementErrorReport? = null,
    val faValidationStatus: String = "Не проверено",
    val indicativeRating: EnvironmentalRating?
) {
    companion object {
        const val RELIABLE_MIN_SAMPLES = 10
        const val MIN_TREES_RECOMMENDED = 5
    }
}
