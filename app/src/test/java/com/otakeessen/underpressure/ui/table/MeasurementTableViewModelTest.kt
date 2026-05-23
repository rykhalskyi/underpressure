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
    private lateinit var trackerRepository: com.otakeessen.underpressure.domain.repository.TrackerRepository
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
        trackerRepository = mockk(relaxed = true)
        alarmScheduler = mockk(relaxed = true)
        
        // Default mocks
        coEvery { settingsRepository.getSettingsSync() } returns null
        every { trackerRepository.getActiveTrackerDefinitions() } returns flowOf(emptyList())
        every { trackerRepository.getAllTrackerValues() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState initially emits loading`() = runTest {
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(null)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
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
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.isFabEnabled)
        assertEquals(0, state.fabTargetSlotIndex)
        assertFalse(state.isGuidanceRequired)
        assertNull(state.fabHint)
    }

    @Test
    fun `FAB shows hint when within 15 min and slot has existing reading`() = runTest {
        // Current time is 12:00. Slot is at 12:10 (within +15 min), with existing data.
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:10"),
            slotActiveFlags = listOf(true)
        )
        val measurements = listOf(
            MeasurementEntity(id = 1, date = today, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }

        assertFalse(state.isFabEnabled)
        assertNull(state.fabTargetSlotIndex)
        assertFalse(state.isGuidanceRequired)
        assertEquals("edit_slot|1", state.fabHint)
    }

    @Test
    fun `FAB opens direct anytime when more than 30 min BEFORE slot`() = runTest {
        // Current time is 12:00. Slot is at 12:35 (35 min in future, >30 min BEFORE).
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:35"),
            slotActiveFlags = listOf(true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.isFabEnabled)
        assertEquals(-1, state.fabTargetSlotIndex)
        assertFalse(state.isGuidanceRequired)
    }

    @Test
    fun `FAB opens direct anytime when more than 15 min AFTER slot`() = runTest {
        // Current time is 12:20. Slot is at 12:00 (20 min past, >15 min AFTER).
        val clock1220 = Clock.fixed(Instant.parse("2023-10-27T12:20:00Z"), ZoneId.of("UTC"))
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:00", "18:00", "22:00"),
            slotActiveFlags = listOf(true, false, false, false)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, clock1220, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.isFabEnabled)
        assertEquals(-1, state.fabTargetSlotIndex)
        assertFalse(state.isGuidanceRequired)
    }

    @Test
    fun `FAB shows anytime confirmation when between 15-30 min BEFORE slot`() = runTest {
        // Current time is 12:00. Slot is at 12:20 (20 min in the future, within 15-30 min BEFORE).
        val settings = AppSettingsEntity(
            slotTimes = listOf("12:20"),
            slotActiveFlags = listOf(true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.isFabEnabled)
        assertEquals(0, state.fabTargetSlotIndex)
        assertTrue(state.isGuidanceRequired)
    }

    @Test
    fun `FAB opens anytime dialog when over 30 min from nearest slot`() = runTest {
        // Current time is 14:00. Closest slot is 12:00 (120 min diff).
        val clock1400 = Clock.fixed(Instant.parse("2023-10-27T14:00:00Z"), ZoneId.of("UTC"))
        val settings = AppSettingsEntity(
            slotTimes = listOf("08:00", "12:00", "18:00", "22:00"),
            slotActiveFlags = listOf(true, true, true, true)
        )
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, clock1400, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertTrue(state.isFabEnabled)
        assertEquals(-1, state.fabTargetSlotIndex)
        assertFalse(state.isGuidanceRequired)
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
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
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
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
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
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
        val state = viewModel.uiState.first { !it.isLoading }
        val todayRow = state.items.find { it.date == today }
        
        assertTrue("Future filled slot SHOULD be clickable for editing", todayRow?.clickableSlots?.contains(0) == true)
    }

    @Test
    fun `toggleViewMode saves setting to repository`() = runTest {
        val settings = AppSettingsEntity(tableIsAllView = false)
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { settingsRepository.saveSettings(any()) } returns Unit

        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        viewModel.toggleViewMode()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.saveSettings(match { it.tableIsAllView }) }
    }

    @Test
    fun `toggleSummaryVisibility saves setting to repository`() = runTest {
        val settings = AppSettingsEntity(tableIsSummaryVisible = true)
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { settingsRepository.saveSettings(any()) } returns Unit

        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        viewModel.toggleSummaryVisibility()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { settingsRepository.saveSettings(match { !it.tableIsSummaryVisible }) }
    }

    @Test
    fun `onSaveMeasurement auto-switches to All view when first anytime reading is added`() = runTest {
        val settings = AppSettingsEntity(tableIsAllView = false, slotTimes = listOf("01:00"))
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(settings)
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { settingsRepository.saveSettings(any()) } returns Unit
        coEvery { measurementRepository.saveMeasurement(any()) } returns 1L

        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
        // Wait for UI state to reflect farSettings
        viewModel.uiState.first { !it.isLoading }

        // Open dialog in anytime mode
        viewModel.onFabClicked()
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertTrue("Dialog should be in flexible mode", viewModel.uiState.value.dialogState.isFlexibleMode)
        
        viewModel.onSaveMeasurement("120/80")
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { settingsRepository.saveSettings(match { it.tableIsAllView }) }
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
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, fixedClock, alarmScheduler)
        
        // Wait for uiState to be ready
        viewModel.uiState.first { !it.isLoading }
        
        viewModel.onCellClicked(today, 0)
        
        // Advance time for co-routines
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertFalse("Dialog should remain closed for future empty slot", viewModel.uiState.value.dialogState.isOpen)
    }
}
