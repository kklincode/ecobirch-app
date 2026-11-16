package ru.ecobirch.fa.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {

    @Transaction
    @Query("SELECT * FROM territories ORDER BY isDemo ASC, timestamp DESC")
    fun observeTerritoriesWithLeaves(): Flow<List<TerritoryWithLeaves>>

    @Transaction
    @Query("SELECT * FROM territories ORDER BY isDemo ASC, timestamp DESC")
    suspend fun getAllTerritoriesWithLeaves(): List<TerritoryWithLeaves>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTerritory(territory: TerritoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTerritories(territories: List<TerritoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaf(leaf: LeafAnalysisEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLeaves(leaves: List<LeafAnalysisEntity>)

    @Query("DELETE FROM leaf_analyses WHERE id = :leafId")
    suspend fun deleteLeafById(leafId: String)

    @Query("DELETE FROM territories WHERE id = :territoryId")
    suspend fun deleteTerritoryById(territoryId: String)

    @Query("DELETE FROM territories WHERE isDemo = 1")
    suspend fun deleteDemoTerritories()

    @Query("DELETE FROM territories")
    suspend fun deleteAllTerritories()

    @Query("SELECT photoPath FROM leaf_analyses WHERE territoryId = :territoryId")
    suspend fun getPhotoPathsForTerritory(territoryId: String): List<String>

    @Query("SELECT photoPath FROM leaf_analyses WHERE id = :leafId")
    suspend fun getPhotoPathForLeaf(leafId: String): String?

    @Query("SELECT photoPath FROM leaf_analyses")
    suspend fun getAllPhotoPaths(): List<String>
}
