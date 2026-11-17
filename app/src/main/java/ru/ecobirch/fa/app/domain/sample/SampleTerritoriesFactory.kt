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
 * Calibration templates & isolated demo territories demonstrating hierarchical sampling
 * (multiple trees + paired blind replicates for ME%) aligned with true Zakharov thresholds.
 */
object SampleTerritoriesFactory {

    // Low asymmetry (~0.020): highly symmetrical control leaf
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

    // Moderate asymmetry (~0.047)
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

    // High asymmetry (~0.085)
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

        fun mirrorLeftRight(p: LeafPoints): LeafPoints = LeafPoints(
            base = p.base.copy(x = 100f - p.base.x),
            tip = p.tip.copy(x = 100f - p.tip.x),
            leftVein1Base = p.rightVein1Base.copy(x = 100f - p.rightVein1Base.x),
            leftVein1End = p.rightVein1End.copy(x = 100f - p.rightVein1End.x),
            leftVein2Base = p.rightVein2Base.copy(x = 100f - p.rightVein2Base.x),
            leftVein2End = p.rightVein2End.copy(x = 100f - p.rightVein2End.x),
            leftWidthMax = p.rightWidthMax.copy(x = 100f - p.rightWidthMax.x),
            rightVein1Base = p.leftVein1Base.copy(x = 100f - p.leftVein1Base.x),
            rightVein1End = p.leftVein1End.copy(x = 100f - p.leftVein1End.x),
            rightVein2Base = p.leftVein2Base.copy(x = 100f - p.leftVein2Base.x),
            rightVein2End = p.leftVein2End.copy(x = 100f - p.leftVein2End.x),
            rightWidthMax = p.leftWidthMax.copy(x = 100f - p.leftWidthMax.x)
        )

        // Blend between symmetric average of template and template itself by factor k (0.2..1.95)
        // and mirror half the leaves so population signed mean = 0 (unimodal true FA, no DA/AS).
        fun scaleAsymmetry(base: LeafPoints, factor: Float, mirror: Boolean): LeafPoints {
            val m = mirrorLeftRight(base)
            fun lerpPt(a: LeafPoint, b: LeafPoint): LeafPoint {
                val midX = (a.x + b.x) * 0.5f
                val midY = (a.y + b.y) * 0.5f
                return LeafPoint(
                    x = (midX + (a.x - midX) * factor).coerceIn(2f, 98f),
                    y = (midY + (a.y - midY) * factor).coerceIn(2f, 98f)
                )
            }
            val scaled = LeafPoints(
                base = lerpPt(base.base, m.base),
                tip = lerpPt(base.tip, m.tip),
                leftVein1Base = lerpPt(base.leftVein1Base, m.leftVein1Base),
                leftVein1End = lerpPt(base.leftVein1End, m.leftVein1End),
                leftVein2Base = lerpPt(base.leftVein2Base, m.leftVein2Base),
                leftVein2End = lerpPt(base.leftVein2End, m.leftVein2End),
                leftWidthMax = lerpPt(base.leftWidthMax, m.leftWidthMax),
                rightVein1Base = lerpPt(base.rightVein1Base, m.rightVein1Base),
                rightVein1End = lerpPt(base.rightVein1End, m.rightVein1End),
                rightVein2Base = lerpPt(base.rightVein2Base, m.rightVein2Base),
                rightVein2End = lerpPt(base.rightVein2End, m.rightVein2End),
                rightWidthMax = lerpPt(base.rightWidthMax, m.rightWidthMax)
            )
            return if (mirror) mirrorLeftRight(scaled) else scaled
        }

        fun perturb(base: LeafPoints, seed: Double, amp: Float = 0.2f): LeafPoints = base.copy(
            leftWidthMax = base.leftWidthMax.copy(
                x = (base.leftWidthMax.x + sin(seed).toFloat() * amp).coerceIn(5f, 45f)
            ),
            rightWidthMax = base.rightWidthMax.copy(
                x = (base.rightWidthMax.x + cos(seed).toFloat() * amp).coerceIn(55f, 95f)
            )
        )

        fun buildLeaf(
            id: String,
            name: String,
            pts: LeafPoints,
            timeMs: Long,
            treeNum: Int,
            replicateOf: String? = null,
            blind: Boolean = false
        ): LeafAnalysis {
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
                detectionSource = DetectionSource.TEMPLATE,
                treeId = "tree_$treeNum",
                treeLabel = "Дерево №$treeNum",
                replicateOfLeafId = replicateOf,
                isBlindMeasurement = blind,
                leafIntegrityVerified = true
            )
        }

        // Unimodal symmetric scale factors around 1.0 (mean = 1.0 -> preserves target Z-score & keeps kurtosis unimodal)
        val treeScales = listOf(0.20f, 0.35f, 0.55f, 0.85f, 1.35f, 2.70f)

        fun buildHierarchicalDemoLeaves(prefix: String, tag: String, basePts: LeafPoints): List<LeafAnalysis> {
            val primary = treeScales.flatMapIndexed { idx, factor ->
                val treeNum = idx + 1
                val dayOffset = 5 - idx
                listOf(
                    buildLeaf(
                        id = "${prefix}_${dayOffset}_a",
                        name = "$tag-${treeNum}A (Демо)",
                        pts = scaleAsymmetry(basePts, factor, mirror = false),
                        timeMs = nowMillis - dayOffset * dayMs - 7_200_000L,
                        treeNum = treeNum
                    ),
                    buildLeaf(
                        id = "${prefix}_${dayOffset}_b",
                        name = "$tag-${treeNum}B (Демо)",
                        pts = scaleAsymmetry(basePts, factor, mirror = true),
                        timeMs = nowMillis - dayOffset * dayMs - 14_400_000L,
                        treeNum = treeNum
                    )
                )
            }
            // Add 2 blind duplicate measurements (ME calibration) referencing trees 1 and 5
            val rep1 = buildLeaf(
                id = "${prefix}_rep1",
                name = "$tag-1A · Повтор (ME)",
                pts = perturb(primary[0].points, 0.35, amp = 0.15f),
                timeMs = nowMillis - 3_600_000L,
                treeNum = 1,
                replicateOf = primary[0].id,
                blind = true
            )
            val rep2 = buildLeaf(
                id = "${prefix}_rep2",
                name = "$tag-5A · Повтор (ME)",
                pts = perturb(primary[8].points, 0.85, amp = 0.15f),
                timeMs = nowMillis - 1_800_000L,
                treeNum = 5,
                replicateOf = primary[8].id,
                blind = true
            )
            return primary + listOf(rep1, rep2)
        }

        return listOf(
            Territory(
                id = "demo_t1",
                name = "Ботанический сад (Демо · Контроль)",
                description = "Демо-участок: 6 деревьев × 2 листа + 2 слепых повтора (ME). Низкая относительная асимметрия.",
                latitude = 55.8384,
                longitude = 37.6063,
                leaves = buildHierarchicalDemoLeaves("demo_t1", "БС", healthyTemplatePoints),
                timestamp = formatIso(nowMillis - 5 * dayMs),
                isDemo = true
            ),
            Territory(
                id = "demo_t2",
                name = "Городской сквер (Демо · Умеренно)",
                description = "Демо-участок: 6 деревьев × 2 листа + 2 слепых повтора (ME). Умеренная асимметрия.",
                latitude = 55.7558,
                longitude = 37.6173,
                leaves = buildHierarchicalDemoLeaves("demo_t2", "ГС", moderateTemplatePoints),
                timestamp = formatIso(nowMillis - 5 * dayMs),
                isDemo = true
            ),
            Territory(
                id = "demo_t3",
                name = "Промзона / Трасса (Демо · Высокий Z)",
                description = "Демо-участок: 6 деревьев × 2 листа + 2 слепых повтора (ME). Высокий индекс асимметрии.",
                latitude = 55.7105,
                longitude = 37.6650,
                leaves = buildHierarchicalDemoLeaves("demo_t3", "ПР", stressedTemplatePoints),
                timestamp = formatIso(nowMillis - 5 * dayMs),
                isDemo = true
            )
        )
    }

    fun formatIso(millis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(millis))
    }
}
