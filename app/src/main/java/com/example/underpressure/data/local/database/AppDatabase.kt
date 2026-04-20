package com.example.underpressure.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.underpressure.data.local.converters.Converters
import com.example.underpressure.data.local.dao.AppSettingsDao
import com.example.underpressure.data.local.dao.MeasurementDao
import com.example.underpressure.data.local.dao.MeasurementEntryDao
import com.example.underpressure.data.local.dao.MeasurementListDao
import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.data.local.entities.MeasurementEntryEntity
import com.example.underpressure.data.local.entities.MeasurementListEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main database class for the application.
 */
@Database(
    entities = [
        MeasurementEntity::class,
        AppSettingsEntity::class,
        MeasurementListEntity::class,
        MeasurementEntryEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun measurementListDao(): MeasurementListDao
    abstract fun measurementEntryDao(): MeasurementEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create measurement_lists table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `measurement_lists` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`type` TEXT NOT NULL, " +
                            "`active` INTEGER NOT NULL DEFAULT 1)"
                )
                // Create measurement_entries table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `measurement_entries` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`date` TEXT NOT NULL, " +
                            "`slotIndex` INTEGER NOT NULL, " +
                            "`listId` INTEGER NOT NULL, " +
                            "`value` TEXT NOT NULL, " +
                            "`updatedAt` INTEGER NOT NULL, " +
                            "FOREIGN KEY(`listId`) REFERENCES `measurement_lists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                // Add index for performance/foreign key
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_measurement_entries_listId` ON `measurement_entries` (`listId`)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "under_pressure_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

