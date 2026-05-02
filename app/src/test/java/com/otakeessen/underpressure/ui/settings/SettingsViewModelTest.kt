package com.otakeessen.underpressure.ui.settings

import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var importManager: com.otakeessen.underpressure.data.export.TableImportManager
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk()
        alarmScheduler = mockk(relaxed = true)
        importManager = mockk(relaxed = true)
    }

    @Test
    fun `uiState loads settings correctly`() = runTest {
        val settings = AppSettingsEntity(
            slotTimes = listOf("08:00", "13:00", "19:00", "23:00"),
            slotActiveFlags = listOf(true, true, false, false)
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(4, state.slots.size)
        assertEquals("08:00", state.slots[0].time)
        assertTrue(state.slots[0].isActive)
        assertFalse(state.slots[0].isToggleable)
        
        assertEquals("19:00", state.slots[2].time)
        assertFalse(state.slots[2].isActive)
        assertTrue(state.slots[2].isToggleable)
    }

    @Test
    fun `updateSlotTime calls repository save and marks slot as modified`() = runTest {
        val settings = AppSettingsEntity(
            slotModifiedFlags = listOf(false, false, false, false)
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.saveSettings(any()) } returns Unit

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)
        viewModel.updateSlotTime(1, "14:30")

        coVerify {
            settingsRepository.saveSettings(match {
                it.slotTimes[1] == "14:30" && it.slotModifiedFlags[1] == true
            })
        }
        verify {
            alarmScheduler.updateAlarms(any())
        }
    }

    @Test
    fun `updateSlotActive calls repository save and alarm scheduler for slots 2-4`() = runTest {
        val settings = AppSettingsEntity()
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.saveSettings(any()) } returns Unit

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)
        viewModel.updateSlotActive(1, true)

        coVerify {
            settingsRepository.saveSettings(match {
                it.slotActiveFlags[1] == true
            })
        }
        verify {
            alarmScheduler.updateAlarms(any())
        }
    }

    @Test
    fun `updateSlotActive does nothing for slot 1`() = runTest {
        val settings = AppSettingsEntity(slotActiveFlags = listOf(true, false, false, false))
        every { settingsRepository.getSettings() } returns flowOf(settings)

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)
        viewModel.updateSlotActive(0, false)

        coVerify(exactly = 0) {
            settingsRepository.saveSettings(any())
        }
        verify(exactly = 0) {
            alarmScheduler.updateAlarms(any())
        }
    }

    @Test
    fun `updateSlotTime fails if difference from neighbor is less than 30 minutes`() = runTest {
        val settings = AppSettingsEntity(
            slotTimes = listOf("07:00", "07:45", "18:00", "22:00"),
            slotActiveFlags = listOf(true, true, false, false)
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)
        
        // Try to set slot 2 (index 1) to 07:15, which is only 15 mins from slot 1 (07:00)
        viewModel.updateSlotTime(1, "07:15")

        coVerify(exactly = 0) {
            settingsRepository.saveSettings(any())
        }
        
        val state = viewModel.uiState.value
        assertTrue(state.error?.startsWith("hint_cannot_create_slot|") == true)
        assertTrue(state.error?.contains("07:00") == true)
    }

    @Test
    fun `updateSlotTime succeeds if difference from neighbor is exactly 30 minutes`() = runTest {
        val settings = AppSettingsEntity(
            slotTimes = listOf("07:00", "08:00", "18:00", "22:00"),
            slotActiveFlags = listOf(true, true, false, false)
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.saveSettings(any()) } returns Unit

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)
        
        // Try to set slot 2 to 07:30
        viewModel.updateSlotTime(1, "07:30")

        coVerify(exactly = 1) {
            settingsRepository.saveSettings(match { it.slotTimes[1] == "07:30" })
        }
    }

    @Test
    fun `updateSlotTime validates wrap-around difference`() = runTest {
        val settings = AppSettingsEntity(
            slotTimes = listOf("23:45", "08:00", "18:00", "22:00"),
            slotActiveFlags = listOf(true, false, false, true)
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)
        
        // Try to set slot 4 (index 3) to 23:55, which is 10 mins from slot 1 (23:45)
        viewModel.updateSlotTime(3, "23:55")

        coVerify(exactly = 0) {
            settingsRepository.saveSettings(any())
        }
        
        val state = viewModel.uiState.value
        assertTrue(state.error?.startsWith("hint_cannot_create_slot|") == true)
        assertTrue(state.error?.contains("23:45") == true)
    }

    @Test
    fun `updateSlotTime ignores inactive slots during validation`() = runTest {
        val settings = AppSettingsEntity(
            slotTimes = listOf("07:00", "07:15", "18:00", "22:00"),
            slotActiveFlags = listOf(true, false, false, false) // Slot 2 (index 1) is inactive
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.saveSettings(any()) } returns Unit

        viewModel = SettingsViewModel(settingsRepository, alarmScheduler, importManager)
        
        // Try to set slot 1 (index 0) to 07:10. 
        // Even though slot 2 is 07:15, it's inactive, so it should be ignored.
        viewModel.updateSlotTime(0, "07:10")

        coVerify(exactly = 1) {
            settingsRepository.saveSettings(match { it.slotTimes[0] == "07:10" })
        }
    }
}

