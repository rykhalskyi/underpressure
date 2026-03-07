package com.example.underpressure.ui.table

import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
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

    private lateinit var repository: MeasurementRepository
    private lateinit var viewModel: MeasurementTableViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @Test
    fun `uiState initially emits loading`() = runTest {
        every { repository.getAllMeasurements() } returns flowOf(emptyList())
        viewModel = MeasurementTableViewModel(repository)
        
        // The stateIn initial value should be loading
        assertEquals(TableUiState(isLoading = true), viewModel.uiState.value)
    }

    @Test
    fun `uiState emits items when repository has data`() = runTest {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val measurements = listOf(
            MeasurementEntity(id = 1, date = today, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70, updatedAt = 1000L),
            MeasurementEntity(id = 2, date = "2024-01-01", slotIndex = 0, systolic = 130, diastolic = 90, pulse = 75, updatedAt = 500L)
        )
        every { repository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = MeasurementTableViewModel(repository)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(2, state.items.size)
        assertTrue(state.items.first { it.date == today }.isToday)
        assertFalse(state.items.first { it.date == "2024-01-01" }.isToday)
    }

    @Test
    fun `uiState takes latest measurement for each day`() = runTest {
        val date = "2024-01-01"
        val measurements = listOf(
            MeasurementEntity(id = 1, date = date, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70, updatedAt = 1000L),
            MeasurementEntity(id = 2, date = date, slotIndex = 1, systolic = 140, diastolic = 95, pulse = 80, updatedAt = 2000L)
        )
        every { repository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = MeasurementTableViewModel(repository)
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(1, state.items.size)
        assertEquals(140, state.items[0].systolic)
        assertEquals(95, state.items[0].diastolic)
        assertEquals(80, state.items[0].pulse)
    }

    @Test
    fun `uiState handles empty repository data`() = runTest {
        every { repository.getAllMeasurements() } returns flowOf(emptyList())
        
        viewModel = MeasurementTableViewModel(repository)
        val state = viewModel.uiState.first { !it.isLoading }

        assertTrue(state.items.isEmpty())
        assertFalse(state.isLoading)
    }
}
