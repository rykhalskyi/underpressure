package com.example.underpressure.ui.table

import com.example.underpressure.alarm.AlarmScheduler
import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementTableViewModelTest {

    private lateinit var measurementRepository: MeasurementRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var alarmScheduler: AlarmScheduler
    private lateinit var viewModel: MeasurementTableViewModel
    private val testDispatcher = StandardTestDispatcher()
    
    // Fixed clock for testing: 2023-10-27 at 12:00:00
    private val fixedClock = Clock.fixed(Instant.parse("2023-10-27T12:00:00Z"), ZoneId.of("UTC"))
    private val today = LocalDate.now(fixedClock).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        measurementRepository = mockk()
        settingsRepository = mockk()
        alarmScheduler = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState initially emits loading`() = runTest {
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(null)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        assertEquals(TableUiState(isLoading = true), viewModel.uiState.value)
    }

    @Test
    fun `FAB is enabled when within 15 mins of an empty slot`() = runTest {
        // Current time is 12:00. Slot is at 12:10 (within +15 min)
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:10"),
            slotActiveFlags = listOf(true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        advanceTimeBy(1) // Trigger initial flow emissions
        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.isFabEnabled)
        assertEquals(0, state.fabTargetSlotIndex)
    }

    @Test
    fun `FAB is disabled when outside 15-min window`() = runTest {
        // Current time is 12:00. Slot is at 12:20 (outside +15 min)
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:20"),
            slotActiveFlags = listOf(true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        advanceTimeBy(1)
        val state = viewModel.uiState.first { !it.isLoading }

        assertFalse(state.isFabEnabled)
        assertNull(state.fabTargetSlotIndex)
    }

    @Test
    fun `FAB is disabled when slot is already filled for today`() = runTest {
        // Current time is 12:00. Slot is at 12:10.
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:10"),
            slotActiveFlags = listOf(true)
        )
        val measurements = listOf(
            MeasurementEntity(id = 1, date = today, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        advanceTimeBy(1)
        val state = viewModel.uiState.first { !it.isLoading }

        assertFalse(state.isFabEnabled)
    }

    @Test
    fun `FAB selects closest in the past slot when multiple overlap`() = runTest {
        // Current time is 12:00. 
        // Slot 0: 11:55 (5 mins in past)
        // Slot 1: 12:05 (5 mins in future)
        val settings = AppSettingsEntity(
            slotTimes = listOf("11:55", "12:05"),
            slotActiveFlags = listOf(true, true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        advanceTimeBy(1)
        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.isFabEnabled)
        assertEquals(0, state.fabTargetSlotIndex) // Should prefer 11:55 because it's in the past
    }

    @Test
    fun `uiState emits items with multiple slots`() = runTest {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val measurements = listOf(
            MeasurementEntity(id = 1, date = todayStr, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = todayStr, slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75)
        )
        // Only first 2 slots active
        val settings = AppSettingsEntity(
            slotTimes = listOf("08:00", "20:00", "22:00", "00:00"),
            slotActiveFlags = listOf(true, true, false, false)
        )
        
        every { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, alarmScheduler = alarmScheduler)
        
        advanceTimeBy(1)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1, state.items.size)
        assertEquals(2, state.items[0].slots.size)
        assertEquals(120, state.items[0].slots[0]?.systolic)
        assertEquals(130, state.items[0].slots[1]?.systolic)
        assertEquals(listOf("08:00", "20:00"), state.slotHeaders)
    }

    @Test
    fun `uiState handles missing settings`() = runTest {
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(null)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, alarmScheduler = alarmScheduler)
        
        advanceTimeBy(1)
        val state = viewModel.uiState.first { !it.isLoading }

        // Should have 1 default active slot header ("07:00")
        assertEquals(listOf("07:00"), state.slotHeaders)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun `onCellClicked opens dialog for today's date`() = runTest {
        val settings = AppSettingsEntity(
            slotActiveFlags = listOf(true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { measurementRepository.getMeasurementsByDateSync(today) } returns emptyList()

        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        viewModel.onCellClicked(today, 0)
        
        val state = viewModel.uiState.first { it.dialogState.isOpen }
        assertTrue(state.dialogState.isOpen)
        assertEquals(today, state.dialogState.date)
    }

    @Test
    fun `onCellClicked does not open dialog for past date`() = runTest {
        val pastDate = "2023-10-26"
        val settings = AppSettingsEntity(
            slotActiveFlags = listOf(true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.getSettingsSync() } returns settings

        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        // Wait for initial state
        viewModel.uiState.first { !it.isLoading }
        
        viewModel.onCellClicked(pastDate, 0)
        
        // Give it some time to potentially process (though it shouldn't)
        testDispatcher.scheduler.runCurrent()
        val state = viewModel.uiState.value
        assertFalse(state.dialogState.isOpen)
    }
}
