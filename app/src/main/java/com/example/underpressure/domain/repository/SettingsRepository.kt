package com.example.underpressure.domain.repository

import com.example.underpressure.data.local.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface for application settings operations.
 */
interface SettingsRepository {
    /**
     * Saves or updates application settings.
     */
    suspend fun saveSettings(settings: AppSettingsEntity)

    /**
     * Retrieves the current application settings.
     * Returns null if no settings have been saved yet.
     */
    fun getSettings(): Flow<AppSettingsEntity?>
    /**
     * Retrieves the current application settings (one-shot).
     */
    suspend fun getSettingsSync(): AppSettingsEntity?
}
