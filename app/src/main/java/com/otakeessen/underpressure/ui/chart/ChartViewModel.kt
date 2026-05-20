package com.otakeessen.underpressure.ui.chart

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.data.export.ChartExportManager
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.components.YAxis
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import com.otakeessen.underpressure.ui.chart.util.ChartColorUtil
import com.otakeessen.underpressure.ui.chart.util.ChartDataUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * ViewModel for the Blood Pressure Chart Screen.
 * Processes measurement data and prepares it for MPAndroidChart.
 */
class ChartViewModel(
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
    private val trackerRepository: TrackerRepository,
    private val chartExportManager: ChartExportManager
) : ViewModel() {

    private val _fromDate = MutableStateFlow<LocalDate?>(null)
    private val _toDate = MutableStateFlow<LocalDate?>(null)
    private val _isConfigSheetOpen = MutableStateFlow(false)
    private val _slotColors = MutableStateFlow(ChartColorUtil.getSlotColors())
    private val _levelColors = MutableStateFlow(ChartColorUtil.getLevelColors())

    sealed class ChartEvent {
        data class ShareFile(val file: File) : ChartEvent()
        data class Error(val messageResId: Int, val arg: String? = null) : ChartEvent()
    }

    private val _events = MutableSharedFlow<ChartEvent>()
    val events = _events.asSharedFlow()

    private val configFlow = combine(
        combine(
            settingsRepository.getSettings(),
            _fromDate,
            _toDate,
            _slotColors,
            _levelColors
        ) { settings, from, to, sc, lc ->
            val slots = settings?.chartSelectedSlots?.toSet() ?: setOf(0, 1, 2, 3, -1)
            val types = settings?.chartSelectedTypes?.mapNotNull { 
                try { MeasurementType.valueOf(it) } catch (e: Exception) { null } 
            }?.toSet() ?: setOf(MeasurementType.SYS, MeasurementType.DIA, MeasurementType.PULSE)
            
            ConfigBase(slots, types, from, to, sc, lc)
        },
        combine(
            settingsRepository.getSettings(),
            _isConfigSheetOpen
        ) { settings, open ->
            val mode = try { ChartMode.valueOf(settings?.chartMode ?: "TREND_BY_SLOT") } catch (e: Exception) { ChartMode.TREND_BY_SLOT }
            val preset = try { DatePreset.valueOf(settings?.chartDatePreset ?: "ALL_TIME") } catch (e: Exception) { DatePreset.ALL_TIME }
            ConfigMode(mode, preset, open)
        },
        settingsRepository.getSettings()
    ) { base, mode, settings ->
        ConfigState(
            base.slots, base.types, base.fromDate, base.toDate,
            mode.chartMode, mode.datePreset, mode.isOpen,
            settings?.chartShowRiskZones ?: true,
            settings?.chartShowRollingAverage ?: false,
            settings?.chartShowInteractiveLegend ?: true,
            base.slotColors, base.levelColors
        )
    }

    val uiState: StateFlow<ChartUiState> = combine(
        measurementRepository.getAllMeasurements(),
        settingsRepository.getSettings(),
        trackerRepository.getActiveTrackerDefinitions(),
        trackerRepository.getAllTrackerValues(),
        configFlow
    ) { args ->
        val measurements = args[0] as List<MeasurementEntity>
        val settings = args[1] as AppSettingsEntity?
        val activeTrackers = args[2] as List<TrackerDefinition>
        val allTrackerValues = args[3] as List<TrackerValue>
        val config = args[4] as ConfigState
        
        val slotTimes = settings?.slotTimes ?: listOf("07:00", "12:00", "18:00", "22:00")

        val typeLabelResIds = mapOf(
            MeasurementType.SYS to R.string.chart_legend_systolic,
            MeasurementType.DIA to R.string.chart_legend_diastolic,
            MeasurementType.PULSE to R.string.chart_legend_pulse
        )

        if (measurements.isEmpty()) {
            return@combine ChartUiState(
                isLoading = false,
                sysLineData = null,
                diaLineData = null,
                pulseLineData = null,
                selectedSlots = config.slots,
                selectedTypes = config.types,
                fromDate = config.fromDate,
                toDate = config.toDate,
                chartMode = config.mode,
                selectedDatePreset = config.preset,
                isConfigSheetOpen = config.isOpen,
                errorMessageResId = R.string.error_no_data,
                slotTimes = slotTimes,
                showRiskZones = config.showRiskZones,
                showRollingAverage = config.showRollingAverage,
                showInteractiveLegend = config.showInteractiveLegend,
                slotColors = config.slotColors,
                levelColors = config.levelColors,
                typeLabelResIds = typeLabelResIds,
                activeTrackers = activeTrackers
            )
        }

        // Filter by date range
        val filtered = measurements.filter { m ->
            val date = LocalDate.parse(m.date, DATE_FORMATTER)
            val afterFrom = config.fromDate == null || !date.isBefore(config.fromDate)
            val beforeTo = config.toDate == null || !date.isAfter(config.toDate)
            afterFrom && beforeTo
        }.sortedBy { it.date }

        if (filtered.isEmpty()) {
             return@combine ChartUiState(
                isLoading = false,
                sysLineData = null,
                diaLineData = null,
                pulseLineData = null,
                selectedSlots = config.slots,
                selectedTypes = config.types,
                fromDate = config.fromDate,
                toDate = config.toDate,
                chartMode = config.mode,
                selectedDatePreset = config.preset,
                isConfigSheetOpen = config.isOpen,
                errorMessageResId = R.string.error_no_data_in_range,
                slotTimes = slotTimes,
                showRiskZones = config.showRiskZones,
                showRollingAverage = config.showRollingAverage,
                showInteractiveLegend = config.showInteractiveLegend,
                slotColors = config.slotColors,
                levelColors = config.levelColors,
                typeLabelResIds = typeLabelResIds,
                activeTrackers = activeTrackers
            )
        }

        // Find min date for X-axis baseline (0-indexed days) in DAILY mode
        val minDateStr = filtered.minBy { it.date }.date
        val minDate = LocalDate.parse(minDateStr, DATE_FORMATTER)

        val sysDataSets = mutableListOf<LineDataSet>()
        val diaDataSets = mutableListOf<LineDataSet>()
        val pulseDataSets = mutableListOf<LineDataSet>()
        val trackerLineData = mutableMapOf<Long, LineData>()
        
        var barData: BarData? = null
        var pieData: PieData? = null
        val xLabels = mutableMapOf<Float, String>()
        var sequentialMeasurements = emptyList<MeasurementEntity>()

        if (config.mode == ChartMode.TREND_BY_SLOT) {
            // Logic for TREND_BY_SLOT mode
            config.slots.forEach { slotIndex ->
                val slotMeasurements = filtered.filter { it.slotIndex == slotIndex }
                if (slotMeasurements.isNotEmpty()) {
                    val slotTimeLabel = if (slotIndex == -1) {
                        "Anytime"
                    } else {
                        slotTimes.getOrElse(slotIndex) { "Slot ${slotIndex + 1}" }
                    }
                    config.types.forEach { type ->
                        val filteredSlotMeasurements = if (type == MeasurementType.PULSE) {
                            slotMeasurements.filter { it.pulse > 0 }
                        } else {
                            slotMeasurements
                        }

                        if (filteredSlotMeasurements.isNotEmpty()) {
                            val entries = filteredSlotMeasurements.map { m ->
                                val date = LocalDate.parse(m.date, DATE_FORMATTER)
                                val days = ChronoUnit.DAYS.between(minDate, date).toFloat()
                                val value = when (type) {
                                    MeasurementType.SYS -> m.systolic.toFloat()
                                    MeasurementType.DIA -> m.diastolic.toFloat()
                                    MeasurementType.PULSE -> m.pulse.toFloat()
                                }
                                Entry(days, value)
                            }.sortedBy { it.x }

                            val label = "$slotTimeLabel - ${type.name}"
                            val dataSet = LineDataSet(entries, label).apply {
                                val colorIndex = if (slotIndex == -1) 4 else slotIndex
                                val colorVal = ChartColorUtil.getSlotColors().getOrElse(colorIndex) { Color.BLACK }
                                color = colorVal
                                setCircleColor(colorVal)
                                lineWidth = 1.5f
                                mode = LineDataSet.Mode.LINEAR
                                setDrawValues(false)
                            }
                            when (type) {
                                MeasurementType.SYS -> sysDataSets.add(dataSet)
                                MeasurementType.DIA -> diaDataSets.add(dataSet)
                                MeasurementType.PULSE -> pulseDataSets.add(dataSet)
                            }
                        }
                    }
                }
            }
        } else if (config.mode == ChartMode.CHRONOLOGICAL) {
            // Logic for CHRONOLOGICAL mode (One plot for all slots, including anytime readings)
            sequentialMeasurements = filtered
                .filter { config.slots.contains(it.slotIndex) || it.isFlexible }
                .sortedWith(compareBy({ it.date }, { it.timestamp }))
            
            sequentialMeasurements.forEachIndexed { index, m ->
                val date = LocalDate.parse(m.date, DATE_FORMATTER)
                val formattedDate = date.format(DateTimeFormatter.ofPattern("dd.MM"))
                xLabels[index.toFloat()] = formattedDate
            }

            config.types.forEach { type ->
                val entries = mutableListOf<Entry>()
                
                sequentialMeasurements.forEachIndexed { index, m ->
                    val value = when (type) {
                        MeasurementType.SYS -> m.systolic.toFloat()
                        MeasurementType.DIA -> m.diastolic.toFloat()
                        MeasurementType.PULSE -> if (m.pulse > 0) m.pulse.toFloat() else null
                    }
                    
                    value?.let { 
                        entries.add(Entry(index.toFloat(), it))
                    }
                }

                if (entries.isNotEmpty()) {
                    val label = when(type) {
                        MeasurementType.SYS -> "Systolic"
                        MeasurementType.DIA -> "Diastolic"
                        MeasurementType.PULSE -> "Pulse"
                    }
                    val dataSet = LineDataSet(entries, label).apply {
                        val colorVal = if (type == MeasurementType.DIA) {
                            ChartColorUtil.getSlotColors()[1]
                        } else if (type == MeasurementType.PULSE) {
                            ChartColorUtil.getSlotColors()[2]
                        } else {
                            ChartColorUtil.getSlotColors()[0]
                        }
                        
                        color = colorVal
                        setCircleColor(colorVal)
                        lineWidth = 1.5f
                        mode = LineDataSet.Mode.LINEAR
                        setDrawValues(false)
                    }
                    when (type) {
                        MeasurementType.SYS -> sysDataSets.add(dataSet)
                        MeasurementType.DIA -> diaDataSets.add(dataSet)
                        MeasurementType.PULSE -> pulseDataSets.add(dataSet)
                    }
                }
            }
        } else if (config.mode == ChartMode.SUMMARY) {
            // Logic for SUMMARY mode
            val distributionData = filtered
                .filter { config.slots.contains(it.slotIndex) || it.isFlexible }
            
            val counts = mutableMapOf<BloodPressureLevel, Int>()
            BloodPressureLevel.entries.forEach { counts[it] = 0 }
            
            distributionData.forEach { m ->
                val result = BloodPressureClassifier.classify(m.systolic, m.diastolic, settings?.bpGuidelines ?: BpGuidelines.ESC_ESH)
                counts[result.level] = counts.getOrDefault(result.level, 0) + 1
            }

            val total = distributionData.size.toFloat()
            if (total > 0) {
                val activeLevels = BloodPressureLevel.entries.filter { (counts[it] ?: 0) > 0 }
                val barEntries = activeLevels.mapIndexed { index, level ->
                    BarEntry(index.toFloat(), counts[level]?.toFloat() ?: 0f)
                }
                activeLevels.forEachIndexed { index, level ->
                    xLabels[index.toFloat()] = level.name.replace("_", " ")
                }
                
                val barDataSet = BarDataSet(barEntries, "Frequency").apply {
                    colors = activeLevels.map { ChartColorUtil.getLevelColors()[it.ordinal] }
                    valueTextSize = 12f
                    setDrawValues(true)
                }
                barData = BarData(barDataSet)

                val pieEntries = activeLevels.map { level ->
                    PieEntry(counts[level]?.toFloat() ?: 0f, level.name.replace("_", " "))
                }
                
                val pieDataSet = PieDataSet(pieEntries, "Distribution").apply {
                    colors = activeLevels.map { ChartColorUtil.getLevelColors()[it.ordinal] }
                    valueTextSize = 12f
                    sliceSpace = 3f
                    setDrawValues(true)
                }
                pieData = PieData(pieDataSet)
            }
        }

        // --- Process Trackers ---
        activeTrackers.filter { it.showOnChart && it.type == TrackerType.FLOAT }.forEach { tracker ->
            val trackerValues = allTrackerValues.filter { it.trackerId == tracker.id }
            if (trackerValues.isNotEmpty()) {
                val entries = trackerValues.mapNotNull { v ->
                    val m = measurements.find { it.id == v.measurementId } ?: return@mapNotNull null
                    val date = LocalDate.parse(m.date, DATE_FORMATTER)
                    
                    // Filter by date range
                    val afterFrom = config.fromDate == null || !date.isBefore(config.fromDate)
                    val beforeTo = config.toDate == null || !date.isAfter(config.toDate)
                    if (!afterFrom || !beforeTo) return@mapNotNull null
                    
                    val x = if (config.mode == ChartMode.TREND_BY_SLOT) {
                        ChronoUnit.DAYS.between(minDate, date).toFloat()
                    } else if (config.mode == ChartMode.CHRONOLOGICAL) {
                        val seqIndex = sequentialMeasurements.indexOf(m)
                        if (seqIndex == -1) return@mapNotNull null
                        seqIndex.toFloat()
                    } else return@mapNotNull null
                    
                    v.floatValue?.let { Entry(x, it.toFloat()) }
                }.sortedBy { it.x }
                
                if (entries.isNotEmpty()) {
                    val dataSet = LineDataSet(entries, tracker.name).apply {
                        color = Color.MAGENTA // Use a distinct color for trackers
                        setCircleColor(Color.MAGENTA)
                        lineWidth = 2f
                        setDrawValues(false)
                        if (tracker.useSecondaryAxis) {
                            axisDependency = YAxis.AxisDependency.RIGHT
                        }
                    }
                    trackerLineData[tracker.id] = LineData(dataSet)
                }
            }
        }

        // 7-Day Rolling Average
        if (config.showRollingAverage && config.mode != ChartMode.SUMMARY) {
            val allForAverage = if (config.mode == ChartMode.TREND_BY_SLOT) filtered else sequentialMeasurements
            if (allForAverage.isNotEmpty()) {
                config.types.forEach { type ->
                    val avgEntries = calculateRollingAverage(allForAverage, minDate, type, config.mode)
                    if (avgEntries.size >= 2) {
                        val avgLabel = "${type.name} (7-day avg)"
                        val avgDataSet = LineDataSet(avgEntries, avgLabel).apply {
                            val baseColor = when (type) {
                                MeasurementType.SYS -> ChartColorUtil.getSlotColors()[0]
                                MeasurementType.DIA -> ChartColorUtil.getSlotColors()[1]
                                MeasurementType.PULSE -> ChartColorUtil.getSlotColors()[2]
                            }
                            color = Color.argb(180, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                            setCircleColor(Color.TRANSPARENT)
                            setDrawCircles(false)
                            lineWidth = 1.5f
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            enableDashedLine(10f, 10f, 0f)
                            setDrawValues(false)
                        }
                        when (type) {
                            MeasurementType.SYS -> sysDataSets.add(avgDataSet)
                            MeasurementType.DIA -> diaDataSets.add(avgDataSet)
                            MeasurementType.PULSE -> pulseDataSets.add(avgDataSet)
                        }
                    }
                }
            }
        }

        ChartUiState(
            isLoading = false,
            sysLineData = if (sysDataSets.isNotEmpty()) LineData(sysDataSets.toList()) else null,
            diaLineData = if (diaDataSets.isNotEmpty()) LineData(diaDataSets.toList()) else null,
            pulseLineData = if (pulseDataSets.isNotEmpty()) LineData(pulseDataSets.toList()) else null,
            trackerLineData = trackerLineData,
            distributionBarData = barData,
            distributionPieData = pieData,
            startDate = minDate,
            selectedSlots = config.slots,
            selectedTypes = config.types,
            fromDate = config.fromDate,
            toDate = config.toDate,
            chartMode = config.mode,
            selectedDatePreset = config.preset,
            isConfigSheetOpen = config.isOpen,
            errorMessageResId = if (config.mode != ChartMode.SUMMARY && sysDataSets.isEmpty() && diaDataSets.isEmpty() && pulseDataSets.isEmpty()) R.string.error_no_slots_selected else null,
            slotTimes = slotTimes,
            xLabels = xLabels,
            showRiskZones = config.showRiskZones,
            showRollingAverage = config.showRollingAverage,
            showInteractiveLegend = config.showInteractiveLegend,
            slotColors = config.slotColors,
            levelColors = config.levelColors,
            typeLabelResIds = typeLabelResIds,
            activeTrackers = activeTrackers
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChartUiState(isLoading = true)
    )

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private data class ConfigBase(
        val slots: Set<Int>,
        val types: Set<MeasurementType>,
        val fromDate: LocalDate?,
        val toDate: LocalDate?,
        val slotColors: List<Int>,
        val levelColors: List<Int>
    )

    private data class ConfigMode(
        val chartMode: ChartMode,
        val datePreset: DatePreset,
        val isOpen: Boolean
    )

    private data class ConfigState(
        val slots: Set<Int>,
        val types: Set<MeasurementType>,
        val fromDate: LocalDate?,
        val toDate: LocalDate?,
        val mode: ChartMode,
        val preset: DatePreset,
        val isOpen: Boolean,
        val showRiskZones: Boolean,
        val showRollingAverage: Boolean,
        val showInteractiveLegend: Boolean,
        val slotColors: List<Int>,
        val levelColors: List<Int>
    )

    private fun calculateRollingAverage(
        measurements: List<MeasurementEntity>,
        minDate: LocalDate,
        type: MeasurementType,
        mode: ChartMode
    ): List<Entry> {
        return ChartDataUtils.calculateRollingAverage(measurements, minDate, type, mode)
    }

    fun toggleSlot(slotIndex: Int) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            val current = settings.chartSelectedSlots.toSet()
            val isSelected = current.contains(slotIndex)
            val chartMode = try { ChartMode.valueOf(settings.chartMode) } catch (e: Exception) { ChartMode.TREND_BY_SLOT }
            
            val canToggleOff = if (chartMode == ChartMode.SUMMARY) {
                current.size > 1
            } else {
                current.size > 1 || settings.chartShowRollingAverage
            }

            if (isSelected && !canToggleOff) return@launch

            val next = if (isSelected) current - slotIndex else current + slotIndex
            settingsRepository.saveSettings(settings.copy(chartSelectedSlots = next.toList().sorted()))
        }
    }

    fun toggleType(type: MeasurementType) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            val current = settings.chartSelectedTypes.toSet()
            val typeStr = type.name
            
            val next = if (current.contains(typeStr)) {
                if (current.size <= 1) current else current - typeStr
            } else {
                current + typeStr
            }
            settingsRepository.saveSettings(settings.copy(chartSelectedTypes = next.toList()))
        }
    }

    fun toggleConfigSheet(open: Boolean) {
        _isConfigSheetOpen.value = open
    }

    fun toggleRiskZones() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            settingsRepository.saveSettings(settings.copy(chartShowRiskZones = !settings.chartShowRiskZones))
        }
    }

    fun toggleRollingAverage() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            if (settings.chartShowRollingAverage && settings.chartSelectedSlots.isEmpty()) return@launch
            settingsRepository.saveSettings(settings.copy(chartShowRollingAverage = !settings.chartShowRollingAverage))
        }
    }

    fun toggleInteractiveLegend() {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            settingsRepository.saveSettings(settings.copy(chartShowInteractiveLegend = !settings.chartShowInteractiveLegend))
        }
    }

    fun toggleTrackerVisibility(trackerId: Long) {
        viewModelScope.launch {
            val tracker = trackerRepository.getTrackerDefinitionById(trackerId)
            if (tracker != null) {
                trackerRepository.saveTrackerDefinition(tracker.copy(showOnChart = !tracker.showOnChart))
            }
        }
    }

    fun setChartMode(mode: ChartMode) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            var updatedSettings = settings.copy(chartMode = mode.name)
            
            if (mode == ChartMode.SUMMARY) {
                if (updatedSettings.chartSelectedSlots.isEmpty()) {
                    updatedSettings = updatedSettings.copy(chartSelectedSlots = listOf(0, 1, 2, 3))
                }
                updatedSettings = updatedSettings.copy(chartShowRollingAverage = false)
            }
            settingsRepository.saveSettings(updatedSettings)
        }
    }

    fun setDatePreset(preset: DatePreset) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            settingsRepository.saveSettings(settings.copy(chartDatePreset = preset.name))
            
            val today = LocalDate.now()
            when (preset) {
                DatePreset.ALL_TIME -> {
                    _fromDate.value = null
                    _toDate.value = null
                }
                DatePreset.LAST_7_DAYS -> {
                    _fromDate.value = today.minusDays(6)
                    _toDate.value = today
                }
                DatePreset.THIS_MONTH -> {
                    _fromDate.value = today.withDayOfMonth(1)
                    _toDate.value = today.withDayOfMonth(today.lengthOfMonth())
                }
                DatePreset.CUSTOM -> {
                    // Keep current values or let user pick
                }
            }
        }
    }

    fun setCustomDateRange(from: LocalDate?, to: LocalDate?) {
        _fromDate.value = from
        _toDate.value = to
        
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            settingsRepository.saveSettings(settings.copy(chartDatePreset = DatePreset.CUSTOM.name))
        }
        
        // UX: Emit warning if range is too large (> 1 year)
        if (from != null && to != null && ChronoUnit.DAYS.between(from, to) > 365) {
            viewModelScope.launch {
                _events.emit(ChartEvent.Error(R.string.warning_large_date_range))
            }
        }
    }

    fun onShareChart(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            try {
                val file = chartExportManager.saveChartToCache(bitmap)
                _events.emit(ChartEvent.ShareFile(file))
            } catch (e: Exception) {
                _events.emit(ChartEvent.Error(R.string.error_failed_to_export, e.message))
            }
        }
    }
}
