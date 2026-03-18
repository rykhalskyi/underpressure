package com.example.underpressure.ui.chart

import com.example.underpressure.data.export.ChartExportManager
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class ChartViewModelTest {

    private val measurementRepository: MeasurementRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val chartExportManager: ChartExportManager = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: ChartViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        coEvery { settingsRepository.getSettings() } returns flowOf(null)
    }

    @Test
    fun `initial state shows no data when repository is empty`() = runTest {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals("No data available", state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `data is correctly filtered by slot`() = runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75)
        )
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        // Wait for initial data
        viewModel.uiState.first { !it.isLoading }

        // Filter only slot 0
        viewModel.updateConfiguration(setOf(0), setOf(MeasurementType.SYS), null, null)
        
        val state = viewModel.uiState.first { it.selectedSlots.size == 1 }
        assertNotNull("LineData should not be null", state.lineData)
        assertEquals(1, state.lineData?.dataSets?.size)
        assertTrue(state.lineData?.dataSets?.get(0)?.label?.contains("Slot 1") == true)
    }

    @Test
    fun `data is correctly filtered by date range`() = runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-11", slotIndex = 0, systolic = 130, diastolic = 85, pulse = 75)
        )
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        // Wait for initial data
        viewModel.uiState.first { !it.isLoading }

        // Filter from 2026-03-11
        viewModel.updateConfiguration(setOf(0, 1, 2, 3), setOf(MeasurementType.SYS), LocalDate.parse("2026-03-11"), null)
        
        val state = viewModel.uiState.first { it.fromDate != null }
        assertNotNull("LineData should not be null", state.lineData)
        // Check only one entry in dataset
        assertEquals(1, state.lineData?.dataSets?.get(0)?.entryCount)
    }

    @Test
    fun `onShareChart triggers ShareFile event`() = runTest {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        val bitmap: android.graphics.Bitmap = mockk()
        val file = File("test.png")
        coEvery { chartExportManager.saveChartToCache(any()) } returns file
        
        viewModel.onShareChart(bitmap)
        
        val event = viewModel.events.first()
        assertTrue(event is ChartViewModel.ChartEvent.ShareFile)
        assertEquals(file, (event as ChartViewModel.ChartEvent.ShareFile).file)
    }
}
