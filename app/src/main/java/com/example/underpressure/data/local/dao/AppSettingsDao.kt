package com.example.underpressure.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.underpressure.data.local.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for application settings.
 */
@Dao
interface AppSettingsDao {
    @Upsert
    suspend fun upsert(settings: AppSettingsEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettingsEntity?>
    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsSync(): AppSettingsEntity?
}
