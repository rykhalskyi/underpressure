package com.example.underpressure.data.repository

import com.example.underpressure.data.local.dao.AppSettingsDao
import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

/**
 * Room-based implementation of [SettingsRepository].
 *
 * @property appSettingsDao Data Access Object for application settings.
 */
class SettingsRepositoryImpl(
    private val appSettingsDao: AppSettingsDao
) : SettingsRepository {

    override suspend fun saveSettings(settings: AppSettingsEntity) {
        appSettingsDao.upsert(settings)
    }

    override fun getSettings(): Flow<AppSettingsEntity?> {
        return appSettingsDao.getSettings()
    }

    override suspend fun getSettingsSync(): AppSettingsEntity? {
        return appSettingsDao.getSettingsSync()
    }
}
