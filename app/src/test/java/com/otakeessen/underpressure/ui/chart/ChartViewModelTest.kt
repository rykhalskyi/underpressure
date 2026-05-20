package com.otakeessen.underpressure.ui.chart

import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.data.export.ChartExportManager
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
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
    private val trackerRepository: TrackerRepository = mockk()
    private val chartExportManager: ChartExportManager = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: ChartViewModel

    private val measurementsFlow = MutableStateFlow<List<MeasurementEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow<AppSettingsEntity?>(null)
    private val trackersDefinitionsFlow = MutableStateFlow<List<TrackerDefinition>>(emptyList())
    private val trackerValuesFlow = MutableStateFlow<List<TrackerValue>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { measurementRepository.getAllMeasurements() } returns measurementsFlow
        every { settingsRepository.getSettings() } returns settingsFlow
        every { trackerRepository.getActiveTrackerDefinitions() } returns trackersDefinitionsFlow
        every { trackerRepository.getAllTrackerValues() } returns trackerValuesFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows no data when repository is empty`() = runTest {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        
        val state = viewModel.uiState.filter { !it.isLoading }.first()
        assertEquals(R.string.error_no_data, state.errorMessageResId)
        assertEquals(DatePreset.ALL_TIME, state.selectedDatePreset)
        assertTrue(state.selectedTypes.contains(MeasurementType.PULSE))
    }

    @Test
    fun `data is correctly filtered by slot`() = runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75)
        )
        measurementsFlow.value = measurements
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        
        // Wait for initial data
        viewModel.uiState.filter { !it.isLoading }.first()

        // Filter only slot 0 by toggling others off (default is all on)
        viewModel.toggleSlot(1)
        viewModel.toggleSlot(2)
        viewModel.toggleSlot(3)
        
        // Filter only SYS by toggling DIA and PULSE off
        viewModel.toggleType(MeasurementType.DIA)
        viewModel.toggleType(MeasurementType.PULSE)
        
        val state = viewModel.uiState.filter { 
            it.selectedSlots == setOf(0) && 
            !it.selectedTypes.contains(MeasurementType.DIA) &&
            !it.selectedTypes.contains(MeasurementType.PULSE)
        }.first()
        
        assertNotNull("SYS LineData should not be null", state.sysLineData)
        assertEquals(1, state.sysLineData?.dataSets?.size)
        assertTrue(state.sysLineData?.dataSets?.get(0)?.label?.contains("07:00") == true)
    }

    @Test
    fun `sequential mode uses continuous indices and combined plots`() = runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75),
            MeasurementEntity(id = 3, date = "2026-03-11", slotIndex = 0, systolic = 125, diastolic = 82, pulse = 72)
        )
        measurementsFlow.value = measurements
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        
        // Wait for initial data
        viewModel.uiState.filter { !it.isLoading }.first()

        // Switch to Sequential Mode
        viewModel.setChartMode(ChartMode.CHRONOLOGICAL)
        
        val state = viewModel.uiState.filter { it.chartMode == ChartMode.CHRONOLOGICAL }.first()
        assertNotNull("SYS LineData should not be null", state.sysLineData)
        
        // Should have data for Systolic
        val sysDataSet = state.sysLineData?.dataSets?.find { it.label == "Systolic" }
        assertNotNull(sysDataSet)
        assertEquals(3, sysDataSet?.entryCount)
        assertEquals(0f, sysDataSet?.getEntryForIndex(0)?.x)
        assertEquals(1f, sysDataSet?.getEntryForIndex(1)?.x)
        assertEquals(2f, sysDataSet?.getEntryForIndex(2)?.x)
        
        // Check labels map (sequential mode uses dd.MM format)
        assertEquals("10.03", state.xLabels[0f])
        assertEquals("10.03", state.xLabels[1f])
        assertEquals("11.03", state.xLabels[2f])
    }

    @Test
    fun `sequential mode filters by slot before indexing`() = runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75),
            MeasurementEntity(id = 3, date = "2026-03-11", slotIndex = 0, systolic = 125, diastolic = 82, pulse = 72)
        )
        measurementsFlow.value = measurements
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        viewModel.uiState.filter { !it.isLoading }.first()

        viewModel.setChartMode(ChartMode.CHRONOLOGICAL)
        // Only select slot 0 by toggling others off
        viewModel.toggleSlot(1)
        viewModel.toggleSlot(2)
        viewModel.toggleSlot(3)
        
        // Ensure only SYS is selected
        viewModel.toggleType(MeasurementType.DIA)
        viewModel.toggleType(MeasurementType.PULSE)
        
        val state = viewModel.uiState.filter { 
            it.selectedSlots == setOf(0) && 
            it.chartMode == ChartMode.CHRONOLOGICAL && 
            !it.selectedTypes.contains(MeasurementType.DIA) &&
            !it.selectedTypes.contains(MeasurementType.PULSE)
        }.first()
        
        val sysDataSet = state.sysLineData?.dataSets?.find { it.label == "Systolic" }
        assertEquals(2, sysDataSet?.entryCount)
        assertEquals(0f, sysDataSet?.getEntryForIndex(0)?.x)
        assertEquals(1f, sysDataSet?.getEntryForIndex(1)?.x) // Index 1 is the next selected measurement
        
        // Check labels map (sequential mode uses dd.MM format)
        assertEquals("10.03", state.xLabels[0f])
        assertEquals("11.03", state.xLabels[1f])
        assertTrue("XLabels should not contain excluded indices", !state.xLabels.containsKey(2f))
    }

    @Test
    fun `date presets correctly filter the data`() = runTest {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        val today = LocalDate.now()
        
        // Last 7 Days
        viewModel.setDatePreset(DatePreset.LAST_7_DAYS)
        var state = viewModel.uiState.filter { it.selectedDatePreset == DatePreset.LAST_7_DAYS }.first()
        assertEquals(today.minusDays(6), state.fromDate)
        assertEquals(today, state.toDate)
    }

    @Test
    fun `pulse line is solid and has standard width`() = runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        measurementsFlow.value = measurements
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        
        // Wait for initial data
        viewModel.uiState.filter { !it.isLoading }.first()

        // Toggle SYS and DIA off (Pulse is ON by default)
        viewModel.toggleType(MeasurementType.SYS)
        viewModel.toggleType(MeasurementType.DIA)
        
        val state = viewModel.uiState.filter { it.selectedTypes == setOf(MeasurementType.PULSE) }.first()
        val pulseDataSet = state.pulseLineData?.dataSets?.get(0) as com.github.mikephil.charting.data.LineDataSet
        
        assertEquals(1.5f, pulseDataSet.lineWidth)
        assertTrue("Pulse line should be solid (no dash pattern)", pulseDataSet.dashPathEffect == null)
    }

    @Test
    fun `onShareChart triggers ShareFile event`() = runTest {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        val bitmap: android.graphics.Bitmap = mockk()
        val file = File("test.png")
        coEvery { chartExportManager.saveChartToCache(any()) } returns file

        val events = mutableListOf<ChartViewModel.ChartEvent>()
        val job = launch(UnconfinedTestDispatcher()) {
            viewModel.events.toList(events)
        }

        viewModel.onShareChart(bitmap)

        assertTrue(events.any { it is ChartViewModel.ChartEvent.ShareFile && (it as ChartViewModel.ChartEvent.ShareFile).file == file })
        job.cancel()
    }

    @Test
    fun `toggleTrackerVisibility updates the tracker definition`() = runTest {
        val tracker = TrackerDefinition(id = 1, name = "Weight", type = TrackerType.FLOAT, showOnChart = false)
        coEvery { trackerRepository.getTrackerDefinitionById(1) } returns tracker
        coEvery { trackerRepository.saveTrackerDefinition(any()) } returns 1

        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        viewModel.toggleTrackerVisibility(1)

        coVerify { trackerRepository.saveTrackerDefinition(match { it.id == 1L && it.showOnChart }) }
    }
}
