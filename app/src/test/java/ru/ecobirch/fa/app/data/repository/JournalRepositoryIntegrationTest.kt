package ru.ecobirch.fa.app.data.repository

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.ecobirch.fa.app.EcoBirchApplication
import ru.ecobirch.fa.app.data.local.EcoBirchDatabase
import ru.ecobirch.fa.app.data.local.PhotoStorageManager
import ru.ecobirch.fa.app.domain.math.LeafBioindicationEngine
import ru.ecobirch.fa.app.domain.model.DetectionSource
import ru.ecobirch.fa.app.domain.model.LeafAnalysis
import ru.ecobirch.fa.app.domain.model.Territory
import ru.ecobirch.fa.app.domain.sample.SampleTerritoriesFactory
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = EcoBirchApplication::class)
class JournalRepositoryIntegrationTest {

    private lateinit var db: EcoBirchDatabase
    private lateinit var repo: JournalRepository

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<EcoBirchApplication>()
        db = Room.inMemoryDatabaseBuilder(ctx, EcoBirchDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = JournalRepository(db.journalDao(), PhotoStorageManager(ctx))
    }

    @After
    fun tearDown() {
        db.close()
        EcoBirchDatabase.resetInstanceForTesting()
    }

    @Test
    fun demoIsolationProtectsUserTerritoriesAndCleanExports() = runTest {
        val pts = SampleTerritoriesFactory.healthyTemplatePoints
        val traits = LeafBioindicationEngine.calculateLeafTraits(pts)
        val userLeaf = LeafAnalysis(
            id = "u_leaf_1",
            photoName = "Полевой лист #1",
            points = pts,
            traits = traits,
            zScore = LeafBioindicationEngine.calculateZScore(traits),
            timestamp = "2026-11-16T10:00:00Z",
            detectionSource = DetectionSource.LOCAL_CONTOUR
        )
        val userTerritory = Territory(
            id = "user_park",
            name = "Парк Покровское-Стрешнево",
            latitude = 55.812,
            longitude = 37.475,
            leaves = listOf(userLeaf),
            timestamp = "2026-11-16T10:00:00Z",
            isDemo = false
        )

        repo.saveTerritory(userTerritory)
        repo.loadDemoTerritories()

        val all = repo.observeTerritories().first()
        assertEquals(4, all.size) // 1 real + 3 demo

        // Default export must exclude demo territories
        val cleanExport = repo.exportJournalToJson(includeDemo = false)
        assertTrue(cleanExport.contains("Парк Покровское-Стрешнево"))
        assertFalse(cleanExport.contains("Ботанический сад (Демо · Контроль)"))

        // Explicit demo export includes all 4
        val fullExport = repo.exportJournalToJson(includeDemo = true)
        assertTrue(fullExport.contains("Ботанический сад (Демо · Контроль)"))

        // Clearing demo leaves user territory untouched
        repo.clearDemoTerritories()
        val remaining = repo.observeTerritories().first()
        assertEquals(1, remaining.size)
        assertEquals("user_park", remaining[0].id)
        assertEquals(1, remaining[0].leaves.size)
    }

    @Test
    fun importJsonRoundtripsAndRejectsCorruptedPayloadsSafely() = runTest {
        val demos = SampleTerritoriesFactory.createDemoTerritories()
        repo.saveTerritory(demos.first().copy(id = "custom_imp", isDemo = false, name = "Импортный участок"))
        val exported = repo.exportJournalToJson(includeDemo = false)

        repo.clearAllUserTerritories()
        assertTrue(repo.observeTerritories().first().isEmpty())

        val ok = repo.importJournalFromJson(exported)
        assertTrue(ok.isSuccess)
        assertEquals(1, ok.getOrNull())
        val restored = repo.observeTerritories().first()
        assertEquals("Импортный участок", restored[0].name)
        assertEquals(14, restored[0].leaves.size)
        assertTrue(restored[0].leaves.any { it.treeId == "tree_1" })
        assertTrue(restored[0].leaves.any { it.treeId == "tree_6" })
        assertTrue(restored[0].leaves.any { it.replicateOfLeafId != null && it.isBlindMeasurement })

        val bad = repo.importJournalFromJson("{ not valid json ]")
        assertTrue(bad.isFailure)
    }

    @Test
    fun roomMigration1To2PreservesV1RowsAndPopulatesScientificDefaults() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<EcoBirchApplication>()
        val dbFile = File(ctx.cacheDir, "migration_v1_test_${System.nanoTime()}.db")
        if (dbFile.exists()) dbFile.delete()

        val v1Db = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS territories (
                id TEXT NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                timestamp TEXT NOT NULL,
                isDemo INTEGER NOT NULL
            )
            """.trimIndent()
        )
        v1Db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS leaf_analyses (
                id TEXT NOT NULL PRIMARY KEY,
                territoryId TEXT NOT NULL,
                photoPath TEXT NOT NULL,
                photoName TEXT NOT NULL,
                pointsJson TEXT NOT NULL,
                traitsJson TEXT NOT NULL,
                zScore REAL NOT NULL,
                isTemplate INTEGER NOT NULL,
                timestamp TEXT NOT NULL,
                aspectRatio REAL NOT NULL,
                axisReferenceLengthMm REAL,
                detectionSource TEXT NOT NULL,
                exifLatitude REAL,
                exifLongitude REAL,
                FOREIGN KEY(territoryId) REFERENCES territories(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        v1Db.execSQL("CREATE INDEX IF NOT EXISTS index_leaf_analyses_territoryId ON leaf_analyses(territoryId)")

        val pts = SampleTerritoriesFactory.healthyTemplatePoints
        val traits = LeafBioindicationEngine.calculateLeafTraits(pts)
        val json = kotlinx.serialization.json.Json { encodeDefaults = true }
        val ptsJson = json.encodeToString(ru.ecobirch.fa.app.domain.model.LeafPoints.serializer(), pts)
        val traitsJson = json.encodeToString(ru.ecobirch.fa.app.domain.model.LeafTraits.serializer(), traits)

        v1Db.execSQL(
            "INSERT INTO territories (id, name, description, latitude, longitude, timestamp, isDemo) VALUES (?, ?, ?, ?, ?, ?, ?)",
            arrayOf("v1_terr", "Старый участок v1", "Описание v1", 55.75, 37.61, "2026-10-01T10:00:00Z", 0)
        )
        v1Db.execSQL(
            "INSERT INTO leaf_analyses (id, territoryId, photoPath, photoName, pointsJson, traitsJson, zScore, isTemplate, timestamp, aspectRatio, axisReferenceLengthMm, detectionSource, exifLatitude, exifLongitude) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf("v1_leaf", "v1_terr", "", "Лист v1", ptsJson, traitsJson, 0.032, 0, "2026-10-01T10:05:00Z", 0.8f, null, "MANUAL_DEFAULT", null, null)
        )
        v1Db.execSQL("PRAGMA user_version = 1")
        v1Db.close()

        val migratedDb = Room.databaseBuilder(ctx, EcoBirchDatabase::class.java, dbFile.absolutePath)
            .addMigrations(EcoBirchDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val migratedRepo = JournalRepository(migratedDb.journalDao(), PhotoStorageManager(ctx))
            val list = migratedRepo.observeTerritories().first()
            assertEquals(1, list.size)
            assertEquals("Старый участок v1", list[0].name)
            assertEquals(1, list[0].leaves.size)
            val leaf = list[0].leaves[0]
            assertEquals("v1_leaf", leaf.id)
            assertEquals("tree_1", leaf.treeId)
            assertEquals("Дерево №1", leaf.treeLabel)
            assertNull(leaf.replicateOfLeafId)
            assertFalse(leaf.isBlindMeasurement)
            assertTrue(leaf.leafIntegrityVerified)
        } finally {
            migratedDb.close()
            dbFile.delete()
        }
    }
}
