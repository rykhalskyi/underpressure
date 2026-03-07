package com.example.underpressure.ui.table

import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementTableViewModelTest {

    private lateinit var measurementRepository: MeasurementRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: MeasurementTableViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        measurementRepository = mockk()
        settingsRepository = mockk()
    }

    @Test
    fun `uiState initially emits loading`() = runTest {
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(null)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository)
        
        assertEquals(TableUiState(isLoading = true), viewModel.uiState.value)
    }

    @Test
    fun `uiState emits items with multiple slots`() = runTest {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val measurements = listOf(
            MeasurementEntity(id = 1, date = today, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = today, slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75)
        )
        // Only first 2 slots active
        val settings = AppSettingsEntity(
            slotTimes = listOf("08:00", "20:00", "22:00", "00:00"),
            slotActiveFlags = listOf(true, true, false, false)
        )
        
        every { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        every { settingsRepository.getSettings() } returns flowOf(settings)
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository)
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
        
        viewModel = MeasurementTableViewModel(measurementRepository, settingsRepository)
        val state = viewModel.uiState.first { !it.isLoading }

        // Should have 1 default active slot header ("07:00")
        assertEquals(listOf("07:00"), state.slotHeaders)
        assertTrue(state.items.isEmpty())
    }
}
