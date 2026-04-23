package com.otakeessen.underpressure.data.repository

import com.otakeessen.underpressure.data.local.dao.AppSettingsDao
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SettingsRepositoryImplTest {

    private lateinit var appSettingsDao: AppSettingsDao
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        appSettingsDao = mockk()
        repository = SettingsRepositoryImpl(appSettingsDao)
    }

    @Test
    fun `saveSettings calls dao upsert`() = runTest {
        val settings = AppSettingsEntity(masterAlarmEnabled = true)
        coEvery { appSettingsDao.upsert(settings) } returns Unit

        repository.saveSettings(settings)

        coVerify(exactly = 1) { appSettingsDao.upsert(settings) }
    }

    @Test
    fun `getSettings calls dao getSettings`() = runTest {
        val settings = AppSettingsEntity(masterAlarmEnabled = true)
        every { appSettingsDao.getSettings() } returns flowOf(settings)

        repository.getSettings().collect {
            assertEquals(settings, it)
        }

        verify(exactly = 1) { appSettingsDao.getSettings() }
    }
}

