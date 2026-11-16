package ru.ecobirch.fa.app.domain.sample

import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.domain.model.Territory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

/**
 * Calibration templates & isolated demo territories calibrated to the true Zakharov scale
 * (< 0.040 / 0.040–0.044 / 0.045–0.049 / 0.050–0.054 / > 0.054).
 */
object SampleTerritoriesFactory {

    // Score I (~0.020): highly symmetrical control leaf
    val healthyTemplatePoints = LeafPoints(
        base = LeafPoint(50f, 90f),
        tip = LeafPoint(50f, 15f),
        leftVein1Base = LeafPoint(50f, 70f),
        leftVein1End = LeafPoint(22f, 52f),
        leftVein2Base = LeafPoint(50f, 50f),
        leftVein2End = LeafPoint(26f, 32f),
        leftWidthMax = LeafPoint(18f, 55f),
        rightVein1Base = LeafPoint(50f, 69.5f),
        rightVein1End = LeafPoint(77.5f, 51.8f),
        rightVein2Base = LeafPoint(50f, 50.2f),
        rightVein2End = LeafPoint(73.6f, 32.3f),
        rightWidthMax = LeafPoint(81.5f, 55.2f)
    )

    // Score III (~0.047): moderate urban stress
    val moderateTemplatePoints = LeafPoints(
        base = LeafPoint(50f, 89f),
        tip = LeafPoint(50f, 16f),
        leftVein1Base = LeafPoint(50f, 68f),
        leftVein1End = LeafPoint(20f, 51f),
        leftVein2Base = LeafPoint(50f, 49f),
        leftVein2End = LeafPoint(23f, 30f),
        leftWidthMax = LeafPoint(15f, 54f),
        rightVein1Base = LeafPoint(50f, 71f),
        rightVein1End = LeafPoint(76f, 55f),
        rightVein2Base = LeafPoint(50f, 50f),
        rightVein2End = LeafPoint(73f, 33f),
        rightWidthMax = LeafPoint(80f, 57f)
    )

    // Score V (~0.085): high traffic / industrial stress
    val stressedTemplatePoints = LeafPoints(
        base = LeafPoint(50f, 90f),
        tip = LeafPoint(52f, 12f),
        leftVein1Base = LeafPoint(50f, 74f),
        leftVein1End = LeafPoint(14f, 58f),
        leftVein2Base = LeafPoint(50f, 54f),
        leftVein2End = LeafPoint(18f, 38f),
        leftWidthMax = LeafPoint(10f, 62f),
        rightVein1Base = LeafPoint(50f, 64f),
        rightVein1End = LeafPoint(76f, 46f),
        rightVein2Base = LeafPoint(50f, 44f),
        rightVein2End = LeafPoint(72f, 26f),
        rightWidthMax = LeafPoint(86f, 48f)
    )

    fun createDemoTerritories(nowMillis: Long = System.currentTimeMillis()): List<Territory> {
        val dayMs = 86_400_000L
        val days = (6 downTo 0).toList()

        fun perturb(base: LeafPoints, seed: Double): LeafPoints = base.copy(
            leftWidthMax = base.leftWidthMax.copy(
                x = (base.leftWidthMax.x + sin(seed).toFloat() * 0.9f).coerceIn(8f, 40f)
            ),
            rightWidthMax = base.rightWidthMax.copy(
                x = (base.rightWidthMax.x + cos(seed).toFloat() * 0.9f).coerceIn(60f, 92f)
            ),
            leftVein2End = base.leftVein2End.copy(
                y = base.leftVein2End.y + sin(seed * 1.7).toFloat() * 0.8f
            ),
            rightVein2End = base.rightVein2End.copy(
                y = base.rightVein2End.y + cos(seed * 1.7).toFloat() * 0.8f
            )
        )

        fun buildLeaf(id: String, name: String, pts: LeafPoints, timeMs: Long): LeafAnalysis {
            val traits = LeafBioindicationEngine.calculateLeafTraits(pts, aspectRatio = 0.8f)
            val z = LeafBioindicationEngine.calculateZScore(traits)
            return LeafAnalysis(
                id = id,
                photoUrl = "template",
                photoName = name,
                points = pts,
                isTemplate = true,
                traits = traits,
                zScore = z,
                timestamp = formatIso(timeMs),
                aspectRatio = 0.8f,
                detectionSource = DetectionSource.TEMPLATE
            )
        }

        // 10+ samples per territory so demo territories illustrate statistically reliable sampling (n=14)
        val t1Leaves = days.flatMapIndexed { idx, d ->
            listOf(
                buildLeaf("demo_t1_${d}_a", "БС-${d + 1}A (Демо)", perturb(healthyTemplatePoints, idx * 1.1), nowMillis - d * dayMs - 7_200_000L),
                buildLeaf("demo_t1_${d}_b", "БС-${d + 1}B (Демо)", perturb(healthyTemplatePoints, (idx + 1) * 0.7), nowMillis - d * dayMs - 14_400_000L)
            )
        }

        val t2Leaves = days.flatMapIndexed { idx, d ->
            listOf(
                buildLeaf("demo_t2_${d}_a", "ГС-${d + 1}A (Демо)", perturb(moderateTemplatePoints, idx * 0.9), nowMillis - d * dayMs - 10_800_000L),
                buildLeaf("demo_t2_${d}_b", "ГС-${d + 1}B (Демо)", perturb(moderateTemplatePoints, (idx + 1) * 1.3), nowMillis - d * dayMs - 21_600_000L)
            )
        }

        val t3Leaves = days.flatMapIndexed { idx, d ->
            listOf(
                buildLeaf("demo_t3_${d}_a", "ПР-${d + 1}A (Демо)", perturb(stressedTemplatePoints, idx * 1.4), nowMillis - d * dayMs - 3_600_000L),
                buildLeaf("demo_t3_${d}_b", "ПР-${d + 1}B (Демо)", perturb(stressedTemplatePoints, (idx + 1) * 0.8), nowMillis - d * dayMs - 18_000_000L)
            )
        }

        return listOf(
            Territory(
                id = "demo_t1",
                name = "Ботанический сад (Демо · Контроль)",
                description = "Условно чистая лесопарковая зона вдали от магистралей. Демонстрационная выборка (n=14).",
                latitude = 55.8384,
                longitude = 37.6063,
                leaves = t1Leaves,
                timestamp = formatIso(nowMillis - 6 * dayMs),
                isDemo = true
            ),
            Territory(
                id = "demo_t2",
                name = "Городской сквер (Демо · Умеренно)",
                description = "Центральная рекреационная зона со средней транспортной нагрузкой. Демонстрационная выборка (n=14).",
                latitude = 55.7558,
                longitude = 37.6173,
                leaves = t2Leaves,
                timestamp = formatIso(nowMillis - 6 * dayMs),
                isDemo = true
            ),
            Territory(
                id = "demo_t3",
                name = "Крупная магистраль (Демо · Стресс)",
                description = "Придорожная полоса интенсивного трафика. Демонстрационная выборка (n=14).",
                latitude = 55.7745,
                longitude = 37.5852,
                leaves = t3Leaves,
                timestamp = formatIso(nowMillis - 6 * dayMs),
                isDemo = true
            )
        )
    }

    fun formatIso(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(millis))
    }
}
