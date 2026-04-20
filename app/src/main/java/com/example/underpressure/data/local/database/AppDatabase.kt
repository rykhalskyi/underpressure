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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "under_pressure_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
