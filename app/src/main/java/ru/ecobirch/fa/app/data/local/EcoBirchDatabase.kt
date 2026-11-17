package ru.ecobirch.fa.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TerritoryEntity::class, LeafAnalysisEntity::class],
    version = 2,
    exportSchema = false
)
abstract class EcoBirchDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: EcoBirchDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE leaf_analyses ADD COLUMN treeId TEXT NOT NULL DEFAULT 'tree_1'")
                db.execSQL("ALTER TABLE leaf_analyses ADD COLUMN treeLabel TEXT NOT NULL DEFAULT 'Дерево №1'")
                db.execSQL("ALTER TABLE leaf_analyses ADD COLUMN replicateOfLeafId TEXT")
                db.execSQL("ALTER TABLE leaf_analyses ADD COLUMN isBlindMeasurement INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE leaf_analyses ADD COLUMN leafIntegrityVerified INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getInstance(context: Context): EcoBirchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EcoBirchDatabase::class.java,
                    "ecobirch_fa_journal.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }

        @androidx.annotation.VisibleForTesting
        fun resetInstanceForTesting() {
            synchronized(this) {
                runCatching { INSTANCE?.close() }
                INSTANCE = null
            }
        }
    }
}
