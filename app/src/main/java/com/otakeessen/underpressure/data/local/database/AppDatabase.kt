package com.otakeessen.underpressure.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.otakeessen.underpressure.data.local.converters.Converters
import com.otakeessen.underpressure.data.local.dao.AppSettingsDao
import com.otakeessen.underpressure.data.local.dao.MeasurementDao
import com.otakeessen.underpressure.data.local.dao.TrackerDao
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.data.local.entities.TrackerDefinitionEntity
import com.otakeessen.underpressure.data.local.entities.TrackerValueEntity

/**
 * Main database class for the application.
 */
@Database(
    entities = [
        MeasurementEntity::class,
        AppSettingsEntity::class,
        TrackerDefinitionEntity::class,
        TrackerValueEntity::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun measurementDao(): MeasurementDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // No changes in schema between 1 and 2
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN lastOnboardedVersion TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN slotModifiedFlags TEXT NOT NULL DEFAULT 'false,false,false,false'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // SQLite doesn't support DROP COLUMN before 3.35.0 (API 34).
                // Standard Room pattern: create new table, copy data, drop old table, rename.
                
                // 1. Create the new table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `app_settings_new` (
                        `id` INTEGER NOT NULL, 
                        `masterAlarmEnabled` INTEGER NOT NULL, 
                        `slotTimes` TEXT NOT NULL, 
                        `slotActiveFlags` TEXT NOT NULL, 
                        `slotModifiedFlags` TEXT NOT NULL, 
                        `lastOnboardedVersion` TEXT, 
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // 2. Copy the data (omitting slotAlarmsEnabled)
                db.execSQL("""
                    INSERT INTO `app_settings_new` (id, masterAlarmEnabled, slotTimes, slotActiveFlags, slotModifiedFlags, lastOnboardedVersion)
                    SELECT id, masterAlarmEnabled, slotTimes, slotActiveFlags, slotModifiedFlags, lastOnboardedVersion FROM app_settings
                """.trimIndent())

                // 3. Drop the old table
                db.execSQL("DROP TABLE app_settings")

                // 4. Rename the new table
                db.execSQL("ALTER TABLE app_settings_new RENAME TO app_settings")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN bpGuidelines TEXT NOT NULL DEFAULT 'ESC_ESH'")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE measurements ADD COLUMN isFlexible INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE measurements ADD COLUMN timestamp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE measurements SET timestamp = createdAt")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN chartSelectedSlots TEXT NOT NULL DEFAULT '0,1,2,3,-1'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN chartSelectedTypes TEXT NOT NULL DEFAULT 'SYS,DIA,PULSE'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN chartShowRiskZones INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN chartShowRollingAverage INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN chartShowInteractiveLegend INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN chartMode TEXT NOT NULL DEFAULT 'TREND_BY_SLOT'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN chartDatePreset TEXT NOT NULL DEFAULT 'ALL_TIME'")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN tableIsAllView INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE app_settings ADD COLUMN tableIsSummaryVisible INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tracker_definitions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `unit` TEXT, 
                        `isActive` INTEGER NOT NULL, 
                        `showOnChart` INTEGER NOT NULL, 
                        `useSecondaryAxis` INTEGER NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tracker_values` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `measurementId` INTEGER NOT NULL, 
                        `trackerId` INTEGER NOT NULL, 
                        `floatValue` REAL, 
                        `booleanValue` INTEGER, 
                        `stringValue` TEXT, 
                        `timestamp` INTEGER NOT NULL, 
                        FOREIGN KEY(`measurementId`) REFERENCES `measurements`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, 
                        FOREIGN KEY(`trackerId`) REFERENCES `tracker_definitions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE 
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_values_measurementId` ON `tracker_values` (`measurementId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tracker_values_trackerId` ON `tracker_values` (`trackerId`)")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tracker_definitions ADD COLUMN min REAL")
                db.execSQL("ALTER TABLE tracker_definitions ADD COLUMN max REAL")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new table without useSecondaryAxis
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tracker_definitions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
                        `name` TEXT NOT NULL, 
                        `type` TEXT NOT NULL, 
                        `unit` TEXT, 
                        `isActive` INTEGER NOT NULL, 
                        `showOnChart` INTEGER NOT NULL, 
                        `min` REAL, 
                        `max` REAL
                    )
                """.trimIndent())

                // 2. Copy the data
                db.execSQL("""
                    INSERT INTO `tracker_definitions_new` (id, name, type, unit, isActive, showOnChart, min, max)
                    SELECT id, name, type, unit, isActive, showOnChart, min, max FROM tracker_definitions
                """.trimIndent())

                // 3. Drop the old table
                db.execSQL("DROP TABLE tracker_definitions")

                // 4. Rename the new table
                db.execSQL("ALTER TABLE tracker_definitions_new RENAME TO tracker_definitions")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "under_pressure_database"
                )
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, 
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, 
                    MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10,
                    MIGRATION_10_11
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

