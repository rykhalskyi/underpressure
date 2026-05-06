package com.otakeessen.underpressure.ui.table

import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
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
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

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

    private fun getExpectedMonthName(month: Month): String {
        return month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

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
    fun `FAB is enabled for empty slot within 15-min window`() = runTest {
        // Current time is 12:00. Slot is at 12:10 (within +15 min)
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:10"),
            slotActiveFlags = listOf(true),
            slotModifiedFlags = listOf(true) // Already modified doesn't matter if empty
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.isFabEnabled)
        assertEquals(0, state.fabTargetSlotIndex)
        assertFalse(state.isGuidanceRequired)
        assertNull(state.fabHint)
    }

    @Test
    fun `FAB is disabled and shows edit hint when slot in window is already filled`() = runTest {
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
        
        val state = viewModel.uiState.first { !it.isLoading }

        assertFalse(state.isFabEnabled)
        assertEquals("edit_slot|1", state.fabHint)
    }

    @Test
    fun `Example 1 - suggested time 30 mins from neighbor before`() = runTest {
        // it is 10.10. there's a slot 1 09.50. Suggest slot 2 with time 10.20.
        val clock1010 = Clock.fixed(Instant.parse("2023-10-27T10:10:00Z"), ZoneId.of("UTC"))
        val settings = AppSettingsEntity(
            slotTimes = listOf("09:50", "15:00", "18:00", "22:00"),
            slotActiveFlags = listOf(true, false, false, false),
            slotModifiedFlags = listOf(true, false, false, false)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, clock1010, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.isFabEnabled)
        assertTrue(state.isGuidanceRequired)
        assertEquals("10:20", state.dialogState.suggestedSlotTime)
    }

    @Test
    fun `Example 2 - suggested time 30 mins from neighbor after`() = runTest {
        // it is 10.10 there's a slot 10.30. Suggest time 10.00
        val clock1010 = Clock.fixed(Instant.parse("2023-10-27T10:10:00Z"), ZoneId.of("UTC"))
        val settings = AppSettingsEntity(
            slotTimes = listOf("10:30", "15:00", "18:00", "22:00"),
            slotActiveFlags = listOf(true, false, false, false),
            slotModifiedFlags = listOf(true, false, false, false)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, clock1010, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals("10:00", state.dialogState.suggestedSlotTime)
    }

    @Test
    fun `Example 3 - conflict hint when squeezed between slots`() = runTest {
        // it is 10.10 There're both slots 09.50 and 10.30
        val clock1010 = Clock.fixed(Instant.parse("2023-10-27T10:10:00Z"), ZoneId.of("UTC"))
        val settings = AppSettingsEntity(
            slotTimes = listOf("09:50", "10:30", "18:00", "22:00"),
            slotActiveFlags = listOf(true, true, false, false),
            slotModifiedFlags = listOf(true, true, false, false)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, clock1010, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isFabEnabled)
        assertEquals("cannot_create|10:30", state.fabHint)
    }

    @Test
    fun `Rule 4 - all slots modified and outside window shows standard hint`() = runTest {
        // Current time is 14:00. Closest slot is 12:00 (too far).
        val clock1400 = Clock.fixed(Instant.parse("2023-10-27T14:00:00Z"), ZoneId.of("UTC"))
        val settings = AppSettingsEntity(
            slotTimes = listOf("08:00", "12:00", "18:00", "22:00"),
            slotActiveFlags = listOf(true, true, true, true),
            slotModifiedFlags = listOf(true, true, true, true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, clock1400, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertFalse(state.isFabEnabled)
        assertEquals("all_modified", state.fabHint)
    }

    @Test
    fun `past slot is clickable even if empty`() = runTest {
        // Current time is 12:00. Slot is at 08:00 (past)
        val settings = AppSettingsEntity(
            slotTimes = listOf("08:00"),
            slotActiveFlags = listOf(true)
        )
        // We need at least one measurement for the date to appear in the table
        val measurements = listOf(
            MeasurementEntity(id = 1, date = today, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        val todayRow = state.items.find { it.date == today }
        
        assertTrue("Past slot should be clickable", todayRow?.clickableSlots?.contains(0) == true)
    }

    @Test
    fun `future empty slot is not clickable`() = runTest {
        // Current time is 12:00. Slot is at 20:00 (future, outside 15-min window)
        val settings = AppSettingsEntity(
            slotTimes = listOf("20:00"),
            slotActiveFlags = listOf(true)
        )
        // Ensure today row exists by adding a measurement for a DIFFERENT slot
        val measurements = listOf(
            MeasurementEntity(id = 1, date = today, slotIndex = 1, systolic = 120, diastolic = 80, pulse = 70)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        val todayRow = state.items.find { it.date == today }
        
        assertFalse("Future empty slot should NOT be clickable", todayRow?.clickableSlots?.contains(0) == true)
    }

    @Test
    fun `future filled slot IS clickable (for editing)`() = runTest {
        // Current time is 12:00. Slot is at 20:00, but ALREADY has data.
        val settings = AppSettingsEntity(
            slotTimes = listOf("20:00"),
            slotActiveFlags = listOf(true)
        )
        val measurements = listOf(
            MeasurementEntity(id = 1, date = today, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        val todayRow = state.items.find { it.date == today }
        
        assertTrue("Future filled slot SHOULD be clickable for editing", todayRow?.clickableSlots?.contains(0) == true)
    }

    @Test
    fun `onCellClicked does not open dialog for future empty slot`() = runTest {
        // Current time is 12:00. Slot is at 20:00
        val settings = AppSettingsEntity(
            slotTimes = listOf("20:00"),
            slotActiveFlags = listOf(true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { measurementRepository.getMeasurementsByDateSync(today) } returns emptyList()
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, fixedClock, alarmScheduler)
        
        // Wait for uiState to be ready
        viewModel.uiState.first { !it.isLoading }
        
        viewModel.onCellClicked(today, 0)
        
        // Advance time for co-routines
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse("Dialog should remain closed for future empty slot", viewModel.uiState.value.dialogState.isOpen)
    }
}
