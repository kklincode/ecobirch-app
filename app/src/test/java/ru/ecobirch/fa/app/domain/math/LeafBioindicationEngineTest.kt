package ru.ecobirch.fa.app.domain.math

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.LeafPoint
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.domain.sample.SampleTerritoriesFactory

class LeafBioindicationEngineTest {

    @Test
    fun perfectlySymmetricLeafHasZeroZScore() {
        val symmetric = LeafPoints(
            base = LeafPoint(50f, 90f),
            tip = LeafPoint(50f, 10f),
            leftVein1Base = LeafPoint(50f, 70f),
            leftVein1End = LeafPoint(20f, 50f),
            leftVein2Base = LeafPoint(50f, 50f),
            leftVein2End = LeafPoint(25f, 30f),
            leftWidthMax = LeafPoint(15f, 55f),
            rightVein1Base = LeafPoint(50f, 70f),
            rightVein1End = LeafPoint(80f, 50f),
            rightVein2Base = LeafPoint(50f, 50f),
            rightVein2End = LeafPoint(75f, 30f),
            rightWidthMax = LeafPoint(85f, 55f)
        )
        val traits = LeafBioindicationEngine.calculateLeafTraits(symmetric, aspectRatio = 0.8f)
        val z = LeafBioindicationEngine.calculateZScore(traits)
        assertEquals(0.0, z, 1e-6)
        assertEquals(1, LeafBioindicationEngine.getEnvironmentalRating(z).score)
    }

    @Test
    fun zakharovScaleBoundariesMatchMethodology() {
        assertEquals(1, LeafBioindicationEngine.getEnvironmentalRating(0.000).score)
        assertEquals(1, LeafBioindicationEngine.getEnvironmentalRating(0.0399).score)
        assertEquals(2, LeafBioindicationEngine.getEnvironmentalRating(0.040).score)
        assertEquals(2, LeafBioindicationEngine.getEnvironmentalRating(0.044).score)
        assertEquals(3, LeafBioindicationEngine.getEnvironmentalRating(0.045).score)
        assertEquals(3, LeafBioindicationEngine.getEnvironmentalRating(0.049).score)
        assertEquals(4, LeafBioindicationEngine.getEnvironmentalRating(0.050).score)
        assertEquals(4, LeafBioindicationEngine.getEnvironmentalRating(0.054).score)
        assertEquals(5, LeafBioindicationEngine.getEnvironmentalRating(0.0541).score)
        assertEquals(5, LeafBioindicationEngine.getEnvironmentalRating(0.080).score)
    }

    @Test
    fun degenerateCoincidentPointsDoNotProduceNaN() {
        val collapsed = LeafPoint(50f, 50f)
        val pts = LeafPoints(
            base = collapsed,
            tip = collapsed,
            leftVein1Base = collapsed,
            leftVein1End = collapsed,
            leftVein2Base = collapsed,
            leftVein2End = collapsed,
            leftWidthMax = collapsed,
            rightVein1Base = collapsed,
            rightVein1End = collapsed,
            rightVein2Base = collapsed,
            rightVein2End = collapsed,
            rightWidthMax = collapsed
        )
        val traits = LeafBioindicationEngine.calculateLeafTraits(pts)
        val z = LeafBioindicationEngine.calculateZScore(traits)
        assertFalse(z.isNaN())
        assertFalse(z.isInfinite())
        assertEquals(0.0, z, 1e-9)
    }

    @Test
    fun zScoreIsInvariantToPhysicalMmCalibrationScale() {
        val pts = SampleTerritoriesFactory.moderateTemplatePoints
        val uncalibrated = LeafBioindicationEngine.calculateLeafTraits(pts, aspectRatio = 0.8f, axisReferenceLengthMm = null)
        val calibrated = LeafBioindicationEngine.calculateLeafTraits(pts, aspectRatio = 0.8f, axisReferenceLengthMm = 62.5)
        assertEquals(
            LeafBioindicationEngine.calculateZScore(uncalibrated),
            LeafBioindicationEngine.calculateZScore(calibrated),
            1e-9
        )
    }

    @Test
    fun trait5AngleSymmetryRespectsTiltedLeafMidrib() {
        // Leaf tilted 90 degrees horizontally: base=(10,50), tip=(90,50)
        // Left (above midrib in screen coords) and Right (below midrib) are symmetric around y=50
        val tilted = LeafPoints(
            base = LeafPoint(10f, 50f),
            tip = LeafPoint(90f, 50f),
            leftVein1Base = LeafPoint(30f, 50f),
            leftVein1End = LeafPoint(50f, 20f),
            leftVein2Base = LeafPoint(50f, 50f),
            leftVein2End = LeafPoint(70f, 30f),
            leftWidthMax = LeafPoint(45f, 15f),
            rightVein1Base = LeafPoint(30f, 50f),
            rightVein1End = LeafPoint(50f, 80f),
            rightVein2Base = LeafPoint(50f, 50f),
            rightVein2End = LeafPoint(70f, 70f),
            rightWidthMax = LeafPoint(45f, 85f)
        )
        val traits = LeafBioindicationEngine.calculateLeafTraits(tilted, aspectRatio = 1.0f)
        assertEquals(45.0, traits.t5Angle.xLeft, 1e-4)
        assertEquals(45.0, traits.t5Angle.xRight, 1e-4)
        assertEquals(0.0, traits.t5Angle.yDiff, 1e-9)
    }

    @Test
    fun territoryStatisticsComputesSEAndReliabilityThreshold() {
        val pts = SampleTerritoriesFactory.healthyTemplatePoints
        val traits = LeafBioindicationEngine.calculateLeafTraits(pts)
        val sampleLeaf = LeafAnalysis(
            id = "l1",
            photoName = "test",
            points = pts,
            traits = traits,
            zScore = 0.030,
            timestamp = "2026-11-16T00:00:00Z",
            detectionSource = DetectionSource.TEMPLATE
        )
        val smallList = List(5) { idx -> sampleLeaf.copy(id = "l$idx", zScore = 0.030 + idx * 0.002) }
        val smallStats = LeafBioindicationEngine.calculateTerritoryStatistics(smallList)
        assertEquals(5, smallStats.sampleCount)
        assertEquals(5, smallStats.primaryLeafCount)
        assertFalse(smallStats.isReliable)
        assertFalse(smallStats.isHierarchicallyReliable)
        assertTrue(smallStats.standardError > 0.0)
        assertTrue(smallStats.ci95HalfWidth > smallStats.standardError)

        val singleTreeList = List(10) { idx -> sampleLeaf.copy(id = "r$idx", treeId = "tree_1", zScore = 0.035) }
        val singleTreeStats = LeafBioindicationEngine.calculateTerritoryStatistics(singleTreeList)
        assertTrue(singleTreeStats.isReliable)
        assertEquals(1, singleTreeStats.treeCount)
        assertFalse(singleTreeStats.isHierarchicallyReliable)
        assertEquals(0.035, singleTreeStats.meanFA, 1e-9)

        val multiTreeList = List(10) { idx ->
            sampleLeaf.copy(
                id = "m$idx",
                treeId = "tree_${(idx % 5) + 1}",
                treeLabel = "Дерево №${(idx % 5) + 1}",
                zScore = 0.035
            )
        }
        val multiTreeStats = LeafBioindicationEngine.calculateTerritoryStatistics(multiTreeList)
        assertEquals(5, multiTreeStats.treeCount)
        assertTrue(multiTreeStats.isHierarchicallyReliable)
        assertEquals(0.035, multiTreeStats.treeMeanFA, 1e-9)
    }

    @Test
    fun demoTerritoriesMatchDistinctEnvironmentalScoresAndHierarchy() {
        val demos = SampleTerritoriesFactory.createDemoTerritories()
        assertEquals(3, demos.size)
        val s1 = LeafBioindicationEngine.calculateTerritoryStatistics(demos[0].leaves)
        val s2 = LeafBioindicationEngine.calculateTerritoryStatistics(demos[1].leaves)
        val s3 = LeafBioindicationEngine.calculateTerritoryStatistics(demos[2].leaves)
        assertTrue(s1.isReliable && s2.isReliable && s3.isReliable)
        assertTrue(s1.isHierarchicallyReliable && s2.isHierarchicallyReliable && s3.isHierarchicallyReliable)
        assertEquals(6, s1.treeCount)
        assertEquals(12, s1.primaryLeafCount)
        assertEquals(2, s1.replicateCount)
        assertTrue((s1.measurementError?.pairedLeafCount ?: 0) == 2)
        assertEquals(1, s1.indicativeRating?.score)
        assertEquals(3, s2.indicativeRating?.score)
        assertEquals(5, s3.indicativeRating?.score)
    }

    @Test
    fun signedAsymmetryDetectsDirectionalAsymmetryAcrossLeaves() {
        val pts = SampleTerritoriesFactory.healthyTemplatePoints.copy(
            leftWidthMax = LeafPoint(10f, 52f),
            rightWidthMax = LeafPoint(80f, 52f) // systematically wider left half on every leaf
        )
        val traits = LeafBioindicationEngine.calculateLeafTraits(pts, aspectRatio = 1.0f)
        assertTrue(traits.t1Width.signedRelative > 0.0)
        assertEquals(traits.t1Width.yDiff, kotlin.math.abs(traits.t1Width.signedRelative), 1e-9)

        val biasedLeaves = List(12) { idx ->
            val shiftedPts = pts.copy(
                leftWidthMax = LeafPoint(10f - (idx % 3) * 0.5f, 52f)
            )
            val tr = LeafBioindicationEngine.calculateLeafTraits(shiftedPts, aspectRatio = 1.0f)
            LeafAnalysis(
                id = "da_$idx",
                treeId = "tree_${(idx % 6) + 1}",
                photoName = "DA $idx",
                points = shiftedPts,
                traits = tr,
                zScore = LeafBioindicationEngine.calculateZScore(tr),
                timestamp = "2026-11-16T00:00:00Z"
            )
        }
        val stats = LeafBioindicationEngine.calculateTerritoryStatistics(biasedLeaves)
        assertTrue(stats.hasDirectionalAsymmetry)
        assertTrue(stats.traitDiagnostics[0].hasDirectionalAsymmetry)
        assertTrue(stats.faValidationStatus.contains("DA"))
    }

    @Test
    fun measurementErrorReportExcludesReplicatesFromPrimaryMeanAndMeasuresVariance() {
        val pts = SampleTerritoriesFactory.moderateTemplatePoints
        val traits = LeafBioindicationEngine.calculateLeafTraits(pts)
        val baseZ = LeafBioindicationEngine.calculateZScore(traits)

        val primaries = (0 until 6).map { idx ->
            LeafAnalysis(
                id = "p_$idx",
                treeId = "tree_${idx + 1}",
                photoName = "P$idx",
                points = pts,
                traits = traits,
                zScore = baseZ + (idx - 2.5) * 0.008,
                timestamp = "2026-11-16T00:00:00Z"
            )
        }
        val replicates = listOf(
            primaries[0].copy(id = "rep_0", replicateOfLeafId = "p_0", isBlindMeasurement = true, zScore = primaries[0].zScore + 0.001),
            primaries[1].copy(id = "rep_1", replicateOfLeafId = "p_1", isBlindMeasurement = true, zScore = primaries[1].zScore - 0.001)
        )
        val stats = LeafBioindicationEngine.calculateTerritoryStatistics(primaries + replicates)
        assertEquals(6, stats.primaryLeafCount)
        assertEquals(2, stats.replicateCount)
        assertEquals(baseZ, stats.meanFA, 1e-6)
        val me = stats.measurementError
        assertTrue(me != null)
        assertEquals(2, me!!.pairedLeafCount)
        assertEquals(0.001, me.meanAbsoluteDiffZ, 1e-6)
        assertTrue(me.measurementErrorPercent in 0.0..100.0)
    }

    @Test
    fun orthogonalProjectionDropsPerpendicularOntoTiltedMidrib() {
        val base = LeafPoint(20f, 80f)
        val tip = LeafPoint(80f, 20f) // 45-degree diagonal midrib
        val side = LeafPoint(20f, 20f)
        val proj = LeafBioindicationEngine.projectPointOntoLine(side, base, tip, aspectRatio = 1.0f)
        assertEquals(50.0f, proj.x, 1e-3f)
        assertEquals(50.0f, proj.y, 1e-3f)
    }

    @Test
    fun territoryJournalJsonRoundtripPreservesMetrics() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val demos = SampleTerritoriesFactory.createDemoTerritories()
        val encoded = json.encodeToString(demos)
        val decoded = json.decodeFromString<List<Territory>>(encoded)
        assertEquals(demos.size, decoded.size)
        assertEquals(demos[0].leaves.size, decoded[0].leaves.size)
        assertEquals(demos[1].leaves[0].zScore, decoded[1].leaves[0].zScore, 1e-9)
    }
}
