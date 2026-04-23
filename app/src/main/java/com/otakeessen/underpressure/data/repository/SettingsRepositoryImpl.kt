package com.otakeessen.underpressure.data.repository

import com.otakeessen.underpressure.data.local.dao.AppSettingsDao
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.domain.repository.SettingsRepository
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

