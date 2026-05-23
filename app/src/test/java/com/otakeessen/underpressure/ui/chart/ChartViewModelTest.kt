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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
    private val testScope = TestScope(testDispatcher)

    private lateinit var viewModel: ChartViewModel
    private lateinit var uiStateCollector: kotlinx.coroutines.Job

    private val measurementsFlow = MutableStateFlow<List<MeasurementEntity>>(emptyList())
    private val settingsFlow = MutableStateFlow<AppSettingsEntity?>(AppSettingsEntity())
    private val trackersDefinitionsFlow = MutableStateFlow<List<TrackerDefinition>>(emptyList())
    private val trackerValuesFlow = MutableStateFlow<List<TrackerValue>>(emptyList())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { measurementRepository.getAllMeasurements() } returns measurementsFlow
        every { settingsRepository.getSettings() } returns settingsFlow
        coEvery { settingsRepository.getSettingsSync() } answers { settingsFlow.value }
        coEvery { settingsRepository.saveSettings(any()) } answers {
            val newSettings = firstArg<AppSettingsEntity>()
            settingsFlow.value = newSettings
            Unit
        }
        every { trackerRepository.getActiveTrackerDefinitions() } returns trackersDefinitionsFlow
        every { trackerRepository.getAllTrackerValues() } returns trackerValuesFlow
        coEvery { trackerRepository.getTrackerDefinitionById(any()) } returns null
        coEvery { trackerRepository.saveTrackerDefinition(any()) } returns 1
        coEvery { chartExportManager.saveChartToCache(any()) } returns File("test.png")
    }

    private fun startCollecting() {
        uiStateCollector = testScope.launch(UnconfinedTestDispatcher(testScope.testScheduler)) {
            viewModel.uiState.collect { }
        }
    }

    @After
    fun tearDown() {
        if (::uiStateCollector.isInitialized) uiStateCollector.cancel()
        clearAllMocks()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state shows no data when repository is empty`() = testScope.runTest {
        measurementsFlow.value = emptyList()
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        startCollecting()

        try {
            advanceUntilIdle()
            val state = viewModel.uiState.value
            assertEquals(R.string.error_no_data, state.errorMessageResId)
            assertEquals(DatePreset.ALL_TIME, state.selectedDatePreset)
        } finally {
            uiStateCollector.cancel()
            viewModel.testOnlyClear()
        }
    }

    @Test
    fun `data is correctly filtered by slot`() = testScope.runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75)
        )
        measurementsFlow.value = measurements
        val settings = AppSettingsEntity(
            slotTimes = listOf("07:00", "08:00", "12:00", "18:00"),
            slotActiveFlags = listOf(true, true, true, true)
        )
        settingsFlow.value = settings
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        startCollecting()
        
        try {
            advanceUntilIdle()

            // Filter only slot 0 by toggling others off
            viewModel.toggleSlot(1)
            viewModel.toggleSlot(2)
            viewModel.toggleSlot(3)
            
            // Settings are updated asynchronously, so update settings flow to mimic repository behavior
            settingsFlow.value = settings.copy(chartSelectedSlots = listOf(0))
            
            // Filter only SYS by toggling DIA and PULSE off
            viewModel.toggleType(MeasurementType.DIA)
            viewModel.toggleType(MeasurementType.PULSE)
            
            settingsFlow.value = settings.copy(chartSelectedSlots = listOf(0), chartSelectedTypes = listOf(MeasurementType.SYS.name))
            
            advanceUntilIdle()
            val state = viewModel.uiState.value
            
            assertNotNull("SYS LineData should not be null", state.sysLineData)
            assertEquals(1, state.sysLineData?.dataSets?.size)
            assertTrue(state.sysLineData?.dataSets?.get(0)?.label?.contains("07:00") == true)
        } finally {
            uiStateCollector.cancel()
            viewModel.testOnlyClear()
            advanceUntilIdle()
        }
    }

    @Test
    fun `sequential mode uses continuous indices and combined plots`() = testScope.runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75),
            MeasurementEntity(id = 3, date = "2026-03-11", slotIndex = 0, systolic = 125, diastolic = 82, pulse = 72)
        )
        measurementsFlow.value = measurements
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        startCollecting()
        
        try {
            // Wait for initial data
            advanceUntilIdle()

            // Switch to Sequential Mode
            viewModel.setChartMode(ChartMode.CHRONOLOGICAL)
            settingsFlow.value = settingsFlow.value?.copy(chartMode = ChartMode.CHRONOLOGICAL.name)
            advanceUntilIdle()
            
            val state = viewModel.uiState.value
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
        } finally {
            uiStateCollector.cancel()
            viewModel.testOnlyClear()
            advanceUntilIdle()
        }
    }

    @Test
    fun `sequential mode filters by slot before indexing`() = testScope.runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70),
            MeasurementEntity(id = 2, date = "2026-03-10", slotIndex = 1, systolic = 130, diastolic = 85, pulse = 75),
            MeasurementEntity(id = 3, date = "2026-03-11", slotIndex = 0, systolic = 125, diastolic = 82, pulse = 72)
        )
        measurementsFlow.value = measurements
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { } }
        advanceUntilIdle()

        viewModel.setChartMode(ChartMode.CHRONOLOGICAL)
        // Ensure state update is picked up
        settingsFlow.value = settingsFlow.value?.copy(chartMode = ChartMode.CHRONOLOGICAL.name)
        
        viewModel.toggleSlot(1)
        viewModel.toggleSlot(2)
        viewModel.toggleSlot(3)
        settingsFlow.value = settingsFlow.value?.copy(chartSelectedSlots = listOf(0))
        
        viewModel.toggleType(MeasurementType.DIA)
        viewModel.toggleType(MeasurementType.PULSE)
        settingsFlow.value = settingsFlow.value?.copy(chartSelectedTypes = listOf(MeasurementType.SYS.name))
        
        advanceUntilIdle()
        val state = viewModel.uiState.value
        
        val sysDataSet = state.sysLineData?.dataSets?.find { it.label == "Systolic" }
        assertNotNull(sysDataSet)
        assertEquals(2, sysDataSet?.entryCount)
        assertEquals(0f, sysDataSet?.getEntryForIndex(0)?.x)
        assertEquals(1f, sysDataSet?.getEntryForIndex(1)?.x)
        
        // Check labels map (sequential mode uses dd.MM format)
        assertEquals("10.03", state.xLabels[0f])
        assertEquals("11.03", state.xLabels[1f])
        
        job.cancel()
    }

    @Test
    fun `date presets correctly filter the data`() = testScope.runTest {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        startCollecting()
        
        try {
            advanceUntilIdle()

            val today = LocalDate.now()
            
            // Last 7 Days
            viewModel.setDatePreset(DatePreset.LAST_7_DAYS)
            advanceUntilIdle()
            
            val state = viewModel.uiState.value
            assertEquals(DatePreset.LAST_7_DAYS, state.selectedDatePreset)
            assertEquals(today.minusDays(6), state.fromDate)
            assertEquals(today, state.toDate)
        } finally {
            uiStateCollector.cancel()
            viewModel.testOnlyClear()
            advanceUntilIdle()
        }
    }

    @Test
    fun `pulse line is solid and has standard width`() = testScope.runTest {
        val measurements = listOf(
            MeasurementEntity(id = 1, date = "2026-03-10", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        )
        measurementsFlow.value = measurements
        
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        startCollecting()
        
        try {
            // Wait for initial data
            advanceUntilIdle()

            // Toggle SYS and DIA off (Pulse is ON by default)
            viewModel.toggleType(MeasurementType.SYS)
            viewModel.toggleType(MeasurementType.DIA)
            
            advanceUntilIdle()
            val state = viewModel.uiState.value
            val pulseDataSet = state.pulseLineData?.dataSets?.get(0) as com.github.mikephil.charting.data.LineDataSet
            
            assertEquals(1.5f, pulseDataSet.lineWidth)
            assertTrue("Pulse line should be solid (no dash pattern)", pulseDataSet.dashPathEffect == null)
        } finally {
            uiStateCollector.cancel()
            viewModel.testOnlyClear()
            advanceUntilIdle()
        }
    }

    @Test
    fun `onShareChart triggers ShareFile event`() = testScope.runTest {
        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        val bitmap: android.graphics.Bitmap = mockk()
        val file = File("test.png")
        coEvery { chartExportManager.saveChartToCache(any()) } returns file

        val events = mutableListOf<ChartViewModel.ChartEvent>()
        val job = launch(UnconfinedTestDispatcher(testScope.testScheduler)) {
            viewModel.events.toList(events)
        }

        viewModel.onShareChart(bitmap)

        assertTrue(events.any { it is ChartViewModel.ChartEvent.ShareFile && (it as ChartViewModel.ChartEvent.ShareFile).file == file })
        job.cancel()
        
        viewModel.testOnlyClear()
    }

    @Test
    fun `toggleTrackerVisibility updates the tracker definition`() = testScope.runTest {
        val tracker = TrackerDefinition(id = 1, name = "Weight", type = TrackerType.FLOAT, showOnChart = false)
        coEvery { trackerRepository.getTrackerDefinitionById(1) } returns tracker
        coEvery { trackerRepository.saveTrackerDefinition(any()) } returns 1

        viewModel = ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager)
        viewModel.toggleTrackerVisibility(1)

        coVerify { trackerRepository.saveTrackerDefinition(match { it.id == 1L && it.showOnChart }) }
        
        viewModel.testOnlyClear()
    }
}
