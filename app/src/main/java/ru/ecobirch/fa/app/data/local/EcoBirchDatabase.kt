package ru.ecobirch.fa.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [TerritoryEntity::class, LeafAnalysisEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EcoBirchDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao

    companion object {
        @Volatile
        private var INSTANCE: EcoBirchDatabase? = null

        fun getInstance(context: Context): EcoBirchDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    EcoBirchDatabase::class.java,
                    "ecobirch_fa_journal.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
