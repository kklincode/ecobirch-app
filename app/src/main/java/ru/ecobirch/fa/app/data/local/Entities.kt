package ru.ecobirch.fa.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "territories")
data class TerritoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String,
    val isDemo: Boolean
)

@Entity(
    tableName = "leaf_analyses",
    foreignKeys = [
        ForeignKey(
            entity = TerritoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["territoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["territoryId"])]
)
data class LeafAnalysisEntity(
    @PrimaryKey val id: String,
    val territoryId: String,
    val photoPath: String,
    val photoName: String,
    val pointsJson: String,
    val traitsJson: String,
    val zScore: Double,
    val isTemplate: Boolean,
    val timestamp: String,
    val aspectRatio: Float,
    val axisReferenceLengthMm: Double?,
    val detectionSource: String,
    val exifLatitude: Double?,
    val exifLongitude: Double?
)

data class TerritoryWithLeaves(
    @Embedded val territory: TerritoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "territoryId"
    )
    val leaves: List<LeafAnalysisEntity>
)
