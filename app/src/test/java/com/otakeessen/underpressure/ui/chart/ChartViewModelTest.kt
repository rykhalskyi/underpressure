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
    fun `sequential mode uses incrementing indices for X values`() = runTest(testDispatcher) {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75),
            MeasurementEntity(id = 3, date = "2026-03-11", slotIndex = 0, systolic = 125, diastolic = 82, pulse = 72)
        )
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        // Wait for initial data
        viewModel.uiState.first { !it.isLoading }

        // Switch to Sequential Mode
        viewModel.setChartMode(ChartMode.SEQUENTIAL)
        
        val state = viewModel.uiState.first { it.chartMode == ChartMode.SEQUENTIAL }
        assertNotNull("BP LineData should not be null", state.bpLineData)
        
        // Slot 0 has 2 entries (index 0 and 2 because Slot 1 is index 1)
        val slot0Sys = state.bpLineData?.dataSets?.find { it.label?.startsWith("07:00") == true && it.label?.endsWith("SYS") == true }
        assertNotNull(slot0Sys)
        assertEquals(2, slot0Sys?.entryCount)
        assertEquals(0f, slot0Sys?.getEntryForIndex(0)?.x)
        assertEquals(2f, slot0Sys?.getEntryForIndex(1)?.x)
        
        // Check labels map
        val mar = LocalDate.of(2026, 3, 1).format(java.time.format.DateTimeFormatter.ofPattern("MMM"))
        assertEquals("$mar 10\n07:00", state.xLabels[0f])
        assertEquals("$mar 10\n12:00", state.xLabels[1f])
        assertEquals("$mar 11\n07:00", state.xLabels[2f])
    }

    @Test
    fun `date presets correctly filter the data`() = runTest(testDispatcher) {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        val today = LocalDate.now()
        
        // Last 7 Days
        viewModel.setDatePreset(DatePreset.LAST_7_DAYS)
        var state = viewModel.uiState.first { it.selectedDatePreset == DatePreset.LAST_7_DAYS }
        assertEquals(today.minusDays(6), state.fromDate)
        assertEquals(today, state.toDate)
        
        // Last 30 Days
        viewModel.setDatePreset(DatePreset.LAST_30_DAYS)
        state = viewModel.uiState.first { it.selectedDatePreset == DatePreset.LAST_30_DAYS }
        assertEquals(today.minusDays(29), state.fromDate)
        assertEquals(today, state.toDate)
    }

    @Test
    fun `pulse line is solid and has standard width`() = runTest(testDispatcher) {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        coEvery { measurementRepository.getAllMeasurements() } returns flowOf(measurements)
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, chartExportManager)
        
        viewModel.updateConfiguration(setOf(0), setOf(MeasurementType.PULSE), null, null)
        
        val state = viewModel.uiState.first { it.selectedTypes.contains(MeasurementType.PULSE) }
        val pulseDataSet = state.pulseLineData?.dataSets?.get(0) as com.github.mikephil.charting.data.LineDataSet
        
        assertEquals(3f, pulseDataSet.lineWidth)
        assertTrue("Pulse line should be solid (no dash pattern)", pulseDataSet.dashPathEffect == null)
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
