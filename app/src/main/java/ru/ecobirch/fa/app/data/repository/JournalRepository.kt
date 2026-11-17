package ru.ecobirch.fa.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.ecobirch.fa.app.data.local.JournalDao
import ru.ecobirch.fa.app.data.local.LeafAnalysisEntity
import ru.ecobirch.fa.app.data.local.PhotoStorageManager
import ru.ecobirch.fa.app.data.local.TerritoryEntity
import ru.ecobirch.fa.app.data.local.TerritoryWithLeaves
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.LeafPoints
import ru.ecobirch.fa.app.domain.model.LeafTraits
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.domain.sample.SampleTerritoriesFactory

class JournalRepository(
    private val dao: JournalDao,
    private val photoManager: PhotoStorageManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun observeTerritories(): Flow<List<Territory>> {
        return dao.observeTerritoriesWithLeaves().map { rows ->
            rows.map { it.toDomain() }
        }
    }

    suspend fun saveTerritory(territory: Territory) = withContext(Dispatchers.IO) {
        dao.upsertTerritory(territory.toEntity())
        if (territory.leaves.isNotEmpty()) {
            dao.upsertLeaves(territory.leaves.map { it.toEntity(territory.id) })
        }
    }

    suspend fun addMeasurementToTerritory(
        territoryId: String,
        leaf: LeafAnalysis
    ) = withContext(Dispatchers.IO) {
        dao.upsertLeaf(leaf.toEntity(territoryId))
    }

    suspend fun deleteLeaf(leafId: String) = withContext(Dispatchers.IO) {
        val path = dao.getPhotoPathForLeaf(leafId)
        dao.deleteLeafById(leafId)
        photoManager.deletePhotoIfOwned(path)
    }

    suspend fun deleteTerritory(territoryId: String) = withContext(Dispatchers.IO) {
        val paths = dao.getPhotoPathsForTerritory(territoryId)
        dao.deleteTerritoryById(territoryId)
        paths.forEach { photoManager.deletePhotoIfOwned(it) }
    }

    suspend fun loadDemoTerritories() = withContext(Dispatchers.IO) {
        val demos = SampleTerritoriesFactory.createDemoTerritories()
        dao.upsertTerritories(demos.map { it.toEntity() })
        dao.upsertLeaves(demos.flatMap { t -> t.leaves.map { it.toEntity(t.id) } })
    }

    suspend fun clearDemoTerritories() = withContext(Dispatchers.IO) {
        dao.deleteDemoTerritories()
    }

    suspend fun clearAllUserTerritories() = withContext(Dispatchers.IO) {
        val paths = dao.getAllPhotoPaths()
        dao.deleteAllTerritories()
        paths.forEach { photoManager.deletePhotoIfOwned(it) }
    }

    /**
     * Exports journal to JSON compatible with the EcoBirch format.
     * By default, demo territories (isDemo=true) are excluded so scientific exports remain clean.
     */
    suspend fun exportJournalToJson(includeDemo: Boolean = false): String = withContext(Dispatchers.IO) {
        val all = dao.getAllTerritoriesWithLeaves().map { it.toDomain() }
        val filtered = if (includeDemo) all else all.filterNot { it.isDemo }
        json.encodeToString(filtered)
    }

    /**
     * Imports JSON and merges territories/leaves by ID (never overwriting newer existing entries blindly).
     */
    suspend fun importJournalFromJson(rawJson: String): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val imported = json.decodeFromString<List<Territory>>(rawJson)
            var count = 0
            for (t in imported) {
                val isDemoFlag = t.isDemo || t.id in setOf("t1", "t2", "t3", "demo_t1", "demo_t2", "demo_t3")
                dao.upsertTerritory(t.copy(isDemo = isDemoFlag).toEntity())
                val resolvedLeaves = t.leaves.map { leaf ->
                    val savedPhotoPath = if (leaf.photoUrl.startsWith("data:image")) {
                        photoManager.saveBase64DataUrl(leaf.photoUrl)
                    } else {
                        leaf.photoUrl
                    }
                    leaf.copy(photoUrl = savedPhotoPath).toEntity(t.id)
                }
                if (resolvedLeaves.isNotEmpty()) {
                    dao.upsertLeaves(resolvedLeaves)
                }
                count++
            }
            count
        }
    }

    private fun TerritoryWithLeaves.toDomain(): Territory {
        return Territory(
            id = territory.id,
            name = territory.name,
            description = territory.description,
            latitude = territory.latitude,
            longitude = territory.longitude,
            timestamp = territory.timestamp,
            isDemo = territory.isDemo,
            leaves = leaves
                .sortedByDescending { it.timestamp }
                .map { it.toDomain() }
        )
    }

    private fun Territory.toEntity(): TerritoryEntity {
        return TerritoryEntity(
            id = id,
            name = name,
            description = description,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            isDemo = isDemo
        )
    }

    private fun LeafAnalysis.toEntity(territoryId: String): LeafAnalysisEntity {
        return LeafAnalysisEntity(
            id = id,
            territoryId = territoryId,
            photoPath = photoUrl,
            photoName = photoName,
            pointsJson = json.encodeToString(points),
            traitsJson = json.encodeToString(traits),
            zScore = zScore,
            isTemplate = isTemplate,
            timestamp = timestamp,
            aspectRatio = aspectRatio,
            axisReferenceLengthMm = axisReferenceLengthMm,
            detectionSource = detectionSource.name,
            exifLatitude = exifLatitude,
            exifLongitude = exifLongitude,
            treeId = treeId,
            treeLabel = treeLabel,
            replicateOfLeafId = replicateOfLeafId,
            isBlindMeasurement = isBlindMeasurement,
            leafIntegrityVerified = leafIntegrityVerified
        )
    }

    private fun LeafAnalysisEntity.toDomain(): LeafAnalysis {
        val decodedPoints = json.decodeFromString<LeafPoints>(pointsJson)
        val decodedTraits = json.decodeFromString<LeafTraits>(traitsJson)
        val source = runCatching { DetectionSource.valueOf(detectionSource) }
            .getOrDefault(DetectionSource.MANUAL_DEFAULT)
        return LeafAnalysis(
            id = id,
            photoUrl = photoPath,
            photoName = photoName,
            points = decodedPoints,
            isTemplate = isTemplate,
            traits = decodedTraits,
            zScore = zScore,
            timestamp = timestamp,
            aspectRatio = aspectRatio,
            axisReferenceLengthMm = axisReferenceLengthMm,
            detectionSource = source,
            exifLatitude = exifLatitude,
            exifLongitude = exifLongitude,
            treeId = treeId,
            treeLabel = treeLabel,
            replicateOfLeafId = replicateOfLeafId,
            isBlindMeasurement = isBlindMeasurement,
            leafIntegrityVerified = leafIntegrityVerified
        )
    }
}
