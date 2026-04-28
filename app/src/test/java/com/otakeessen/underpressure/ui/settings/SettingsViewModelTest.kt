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

import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.data.export.TableImportManager

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var measurementRepository: MeasurementRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var importManager: TableImportManager
    private lateinit var viewModel: SettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk()
        measurementRepository = mockk()
        alarmScheduler = mockk(relaxed = true)
        importManager = mockk()
    }

    @Test
    fun `uiState loads settings correctly`() = runTest {
        val settings = AppSettingsEntity(
            slotTimes = listOf("08:00", "13:00", "19:00", "23:00"),
            slotActiveFlags = listOf(true, true, false, false)
        )
        every { settingsRepository.getSettings() } returns flowOf(settings)

        viewModel = SettingsViewModel(settingsRepository, measurementRepository, alarmScheduler, importManager)

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
    fun `updateSlotTime calls repository save and alarm scheduler`() = runTest {
        val settings = AppSettingsEntity()
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.saveSettings(any()) } returns Unit

        viewModel = SettingsViewModel(settingsRepository, measurementRepository, alarmScheduler, importManager)
        viewModel.updateSlotTime(1, "14:30")

        coVerify {
            settingsRepository.saveSettings(match {
                it.slotTimes[1] == "14:30"
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

        viewModel = SettingsViewModel(settingsRepository, measurementRepository, alarmScheduler, importManager)
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

        viewModel = SettingsViewModel(settingsRepository, measurementRepository, alarmScheduler, importManager)
        viewModel.updateSlotActive(0, false)

        coVerify(exactly = 0) {
            settingsRepository.saveSettings(any())
        }
        verify(exactly = 0) {
            alarmScheduler.updateAlarms(any())
        }
    }

    @Test
    fun `loadSettings auto-fixes slot 1 alarm if disabled`() = runTest {
        val settings = AppSettingsEntity(
            slotActiveFlags = listOf(true, false, false, false),
            slotAlarmsEnabled = listOf(false, false, false, false)
        )
        // First emission has bug, second emission (after save) is fixed
        val fixedSettings = settings.copy(slotAlarmsEnabled = listOf(true, false, false, false))
        
        // Use a MutableSharedFlow or similar to simulate repository updates
        val settingsFlow = kotlinx.coroutines.flow.MutableSharedFlow<AppSettingsEntity?>(replay = 1)
        settingsFlow.emit(settings)
        
        every { settingsRepository.getSettings() } returns settingsFlow
        coEvery { settingsRepository.saveSettings(any()) } coAnswers {
            settingsFlow.emit(it.invocation.args[0] as AppSettingsEntity)
        }

        viewModel = SettingsViewModel(settingsRepository, measurementRepository, alarmScheduler, importManager)

        coVerify {
            settingsRepository.saveSettings(match {
                it.slotAlarmsEnabled[0] == true
            })
        }
        verify {
            alarmScheduler.updateAlarms(match {
                it.slotAlarmsEnabled[0] == true
            })
        }
        
        assertTrue(viewModel.uiState.value.slots[0].isAlarmEnabled)
    }
}

