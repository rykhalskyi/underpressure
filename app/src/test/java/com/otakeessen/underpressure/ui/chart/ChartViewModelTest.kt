package com.otakeessen.underpressure.ui.chart

import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.data.export.ChartExportManager
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
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
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: ChartViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        coEvery { settingsRepository.getSettings() } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows no data when repository is empty`() = runTest(testDispatcher) {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        val state = viewModel.uiState.first { !it.isLoading }
        assertEquals(R.string.error_no_data, state.errorMessageResId)
    }

    @Test
    fun `data is correctly filtered by slot`() = runTest(testDispatcher) {
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
        
        val state = viewModel.uiState.first { it.selectedSlots == setOf(0) }
        assertNotNull("BP LineData should not be null", state.bpLineData)
        assertEquals(1, state.bpLineData?.dataSets?.size)
        assertTrue(state.bpLineData?.dataSets?.get(0)?.label?.contains("07:00") == true)
    }

    @Test
    fun `data is correctly filtered by date range`() = runTest(testDispatcher) {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-11", slotIndex = 0, systolic = 130, diastolic = 85, pulse = 75)
        )
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        // Wait for initial data
        viewModel.uiState.first { !it.isLoading }

        // Filter from 2026-03-11
        val fromDate = LocalDate.parse("2026-03-11")
        viewModel.updateConfiguration(setOf(0, 1, 2, 3), setOf(MeasurementType.SYS), fromDate, null)
        
        val state = viewModel.uiState.first { it.fromDate == fromDate }
        assertNotNull("BP LineData should not be null", state.bpLineData)
        assertEquals(1, state.bpLineData?.dataSets?.get(0)?.entryCount)
    }

    @Test
    fun `pulse data is correctly separated`() = runTest(testDispatcher) {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        // Filter SYS and PULSE
        val targetTypes = setOf(MeasurementType.SYS, MeasurementType.PULSE)
        viewModel.updateConfiguration(setOf(0), targetTypes, null, null)
        
        val state = viewModel.uiState.first { it.selectedTypes == targetTypes }
        assertNotNull("BP LineData should not be null", state.bpLineData)
        assertNotNull("Pulse LineData should not be null", state.pulseLineData)
        assertEquals(1, state.bpLineData?.dataSets?.size)
    }

    @Test
    fun `onShareChart triggers ShareFile event`() = runTest(testDispatcher) {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        val bitmap: android.graphics.Bitmap = mockk()
        val file = File("test.png")
        coEvery { chartExportManager.saveChartToCache(any()) } returns file

        val events = mutableListOf<ChartViewModel.ChartEvent>()
        val job = launch {
            viewModel.events.toList(events)
        }

        viewModel.onShareChart(bitmap)

        assertTrue(events.any { it is ChartViewModel.ChartEvent.ShareFile && it.file == file })
        job.cancel()
    }
}

