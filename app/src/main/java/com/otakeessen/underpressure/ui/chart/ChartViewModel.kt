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
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
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
import kotlin.math.max
import kotlin.math.min

/**
 * ViewModel for the Blood Pressure Chart Screen.
 * Processes measurement data and prepares it for MPAndroidChart.
 */
class ChartViewModel(
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
    private val chartExportManager: ChartExportManager
) : ViewModel() {

    private val _selectedSlots = MutableStateFlow(setOf(0, 1, 2, 3))
    private val _selectedTypes = MutableStateFlow(setOf(MeasurementType.SYS, MeasurementType.DIA, MeasurementType.PULSE))
    private val _fromDate = MutableStateFlow<LocalDate?>(null)
    private val _toDate = MutableStateFlow<LocalDate?>(null)
    private val _chartMode = MutableStateFlow(ChartMode.DAILY)
    private val _datePreset = MutableStateFlow(DatePreset.ALL_TIME)
    private val _isConfigSheetOpen = MutableStateFlow(false)
    private val _showRiskZones = MutableStateFlow(true)
    private val _showRollingAverage = MutableStateFlow(false)
    private val _showInteractiveLegend = MutableStateFlow(true)

    sealed class ChartEvent {
        data class ShareFile(val file: File) : ChartEvent()
        data class Error(val messageResId: Int, val arg: String? = null) : ChartEvent()
    }

    private val _events = MutableSharedFlow<ChartEvent>()
    val events = _events.asSharedFlow()

    private val configFlow = combine(
        combine(
            _selectedSlots,
            _selectedTypes,
            _fromDate,
            _toDate
        ) { slots, types, from, to ->
            ConfigBase(slots, types, from, to)
        },
        combine(
            _chartMode,
            _datePreset,
            _isConfigSheetOpen
        ) { mode, preset, open ->
            ConfigMode(mode, preset, open)
        },
        combine(
            _showRiskZones,
            _showRollingAverage,
            _showInteractiveLegend
        ) { riskZones, rolling, legend ->
            ConfigVisuals(riskZones, rolling, legend)
        }
    ) { base, mode, visuals ->
        ConfigState(
            base.slots, base.types, base.fromDate, base.toDate,
            mode.chartMode, mode.datePreset, mode.isOpen,
            visuals.showRiskZones, visuals.showRollingAverage, visuals.showInteractiveLegend
        )
    }

    val uiState: StateFlow<ChartUiState> = combine(
        measurementRepository.getAllMeasurements(),
        settingsRepository.getSettings(),
        configFlow
    ) { measurements: List<MeasurementEntity>, settings, config: ConfigState ->
        
        val slotTimes = settings?.slotTimes ?: listOf("07:00", "12:00", "18:00", "22:00")

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
                showInteractiveLegend = config.showInteractiveLegend
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
                showInteractiveLegend = config.showInteractiveLegend
            )
        }

        // Find min date for X-axis baseline (0-indexed days) in DAILY mode
        val minDateStr = filtered.minBy { it.date }.date
        val minDate = LocalDate.parse(minDateStr, DATE_FORMATTER)

        val sysDataSets = mutableListOf<LineDataSet>()
        val diaDataSets = mutableListOf<LineDataSet>()
        val pulseDataSets = mutableListOf<LineDataSet>()
        var barData: BarData? = null
        var pieData: PieData? = null
        val xLabels = mutableMapOf<Float, String>()
        var sequentialMeasurements = emptyList<MeasurementEntity>()

        if (config.mode == ChartMode.DAILY) {
            // Logic for DAILY mode
            config.slots.forEach { slotIndex ->
                val slotMeasurements = filtered.filter { it.slotIndex == slotIndex }
                if (slotMeasurements.isNotEmpty()) {
                    val slotTimeLabel = slotTimes.getOrElse(slotIndex) { "Slot ${slotIndex + 1}" }
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
                                val colorVal = SLOT_COLORS.getOrElse(slotIndex) { Color.BLACK }
                                color = colorVal
                                setCircleColor(colorVal)
                                lineWidth = when (type) {
                                    MeasurementType.SYS -> 3f
                                    MeasurementType.DIA -> 1.5f
                                    MeasurementType.PULSE -> 3f
                                }
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
        } else if (config.mode == ChartMode.SEQUENTIAL) {
            // Logic for SEQUENTIAL mode (One plot for all slots)
            sequentialMeasurements = filtered
                .filter { config.slots.contains(it.slotIndex) }
                .sortedWith(compareBy({ it.date }, { it.slotIndex }))
            
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
                        // Use a single color for all points in sequential mode
                        val colorVal = if (type == MeasurementType.DIA) {
                            SLOT_COLORS[1] // Green for Diastolic
                        } else if (type == MeasurementType.PULSE) {
                            SLOT_COLORS[2] // Orange for Pulse
                        } else {
                            SLOT_COLORS[0] // Blue for Systolic/Default
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
        } else if (config.mode == ChartMode.DISTRIBUTION) {
            // Logic for DISTRIBUTION mode
            val distributionData = filtered
                .filter { config.slots.contains(it.slotIndex) }
            
            val counts = mutableMapOf<BloodPressureLevel, Int>()
            BloodPressureLevel.entries.forEach { counts[it] = 0 }
            
            distributionData.forEach { m ->
                val result = BloodPressureClassifier.classify(m.systolic, m.diastolic, settings?.bpGuidelines ?: BpGuidelines.ESC_ESH)
                counts[result.level] = counts.getOrDefault(result.level, 0) + 1
            }

            val total = distributionData.size.toFloat()
            if (total > 0) {
                // Filter only levels with values
                val activeLevels = BloodPressureLevel.entries.filter { (counts[it] ?: 0) > 0 }
                
                // Bar Data
                val barEntries = activeLevels.mapIndexed { index, level ->
                    BarEntry(index.toFloat(), counts[level]?.toFloat() ?: 0f)
                }
                activeLevels.forEachIndexed { index, level ->
                    xLabels[index.toFloat()] = level.name.replace("_", " ")
                }
                
                val barDataSet = BarDataSet(barEntries, "Frequency").apply {
                    colors = activeLevels.map { LEVEL_COLORS[it.ordinal] }
                    valueTextSize = 12f
                    setDrawValues(true)
                }
                barData = BarData(barDataSet)

                // Pie Data
                val pieEntries = activeLevels.map { level ->
                    PieEntry(counts[level]?.toFloat() ?: 0f, level.name.replace("_", " "))
                }
                
                val pieDataSet = PieDataSet(pieEntries, "Distribution").apply {
                    colors = activeLevels.map { LEVEL_COLORS[it.ordinal] }
                    valueTextSize = 12f
                    sliceSpace = 3f
                    setDrawValues(true)
                }
                pieData = PieData(pieDataSet)
            }
        }

        // 7-Day Rolling Average
        if (config.showRollingAverage && config.mode != ChartMode.DISTRIBUTION) {
            val allForAverage = if (config.mode == ChartMode.DAILY) filtered else sequentialMeasurements
            if (allForAverage.isNotEmpty()) {
                config.types.forEach { type ->
                    val avgEntries = calculateRollingAverage(allForAverage, minDate, type, config.mode)
                    if (avgEntries.size >= 2) {
                        val avgLabel = "${type.name} (7-day avg)"
                        val avgDataSet = LineDataSet(avgEntries, avgLabel).apply {
                            val baseColor = when (type) {
                                MeasurementType.SYS -> SLOT_COLORS[0]
                                MeasurementType.DIA -> SLOT_COLORS[1]
                                MeasurementType.PULSE -> SLOT_COLORS[2]
                            }
                            color = Color.argb(180, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor))
                            setCircleColor(Color.TRANSPARENT)
                            setDrawCircles(false)
                            lineWidth = 2f
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
            errorMessageResId = if (config.mode != ChartMode.DISTRIBUTION && sysDataSets.isEmpty() && diaDataSets.isEmpty() && pulseDataSets.isEmpty()) R.string.error_no_slots_selected else null,
            slotTimes = slotTimes,
            xLabels = xLabels,
            showRiskZones = config.showRiskZones,
            showRollingAverage = config.showRollingAverage,
            showInteractiveLegend = config.showInteractiveLegend
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(),
        initialValue = ChartUiState(isLoading = true)
    )

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val SLOT_COLORS = listOf(
            Color.parseColor("#2196F3"), // Blue
            Color.parseColor("#4CAF50"), // Green
            Color.parseColor("#FF9800"), // Orange
            Color.parseColor("#E91E63")  // Pink
        )
        private val LEVEL_COLORS = listOf(
            Color.parseColor("#2E7D32"), // Normal - Green
            Color.parseColor("#E6AC00"), // Elevated - Amber
            Color.parseColor("#E67E22"), // Stage 1 - Orange
            Color.parseColor("#C0392B"), // Stage 2 - Red
            Color.parseColor("#8B0000")  // Crisis - Dark Red
        )
    }

    private data class ConfigBase(
        val slots: Set<Int>,
        val types: Set<MeasurementType>,
        val fromDate: LocalDate?,
        val toDate: LocalDate?
    )

    private data class ConfigMode(
        val chartMode: ChartMode,
        val datePreset: DatePreset,
        val isOpen: Boolean
    )

    private data class ConfigVisuals(
        val showRiskZones: Boolean,
        val showRollingAverage: Boolean,
        val showInteractiveLegend: Boolean
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
        val showInteractiveLegend: Boolean
    )

    private fun calculateRollingAverage(
        measurements: List<MeasurementEntity>,
        minDate: LocalDate,
        type: MeasurementType,
        mode: ChartMode
    ): List<Entry> {
        val sorted = measurements
            .filter { m ->
                when (type) {
                    MeasurementType.PULSE -> m.pulse > 0
                    else -> true
                }
            }
            .sortedBy { it.date }

        if (sorted.isEmpty()) return emptyList()

        if (mode == ChartMode.DAILY) {
            val result = mutableListOf<Entry>()
            val valueForDate = sorted.groupBy { it.date }
            val sortedDates = valueForDate.keys.sorted().map { LocalDate.parse(it, DATE_FORMATTER) }

            var windowStartIdx = 0
            var currentSum = 0f
            var currentCount = 0

            for (currentDate in sortedDates) {
                val windowStartLimit = currentDate.minusDays(6)

                // Add values for the current date
                val currentMeasurements = valueForDate[currentDate.format(DATE_FORMATTER)]!!
                for (m in currentMeasurements) {
                    currentSum += when (type) {
                        MeasurementType.SYS -> m.systolic.toFloat()
                        MeasurementType.DIA -> m.diastolic.toFloat()
                        MeasurementType.PULSE -> m.pulse.toFloat()
                    }
                    currentCount++
                }

                // Remove values that are now outside the 7-day window
                while (windowStartIdx < sortedDates.size && sortedDates[windowStartIdx].isBefore(windowStartLimit)) {
                    val oldDate = sortedDates[windowStartIdx]
                    val oldMeasurements = valueForDate[oldDate.format(DATE_FORMATTER)]!!
                    for (m in oldMeasurements) {
                        currentSum -= when (type) {
                            MeasurementType.SYS -> m.systolic.toFloat()
                            MeasurementType.DIA -> m.diastolic.toFloat()
                            MeasurementType.PULSE -> m.pulse.toFloat()
                        }
                        currentCount--
                    }
                    windowStartIdx++
                }

                if (currentCount > 0) {
                    val x = ChronoUnit.DAYS.between(minDate, currentDate).toFloat()
                    result.add(Entry(x, currentSum / currentCount))
                }
            }
            return result
        }

        var windowStartIdx = 0
        var currentSum = 0f

        return sorted.mapIndexed { index, m ->
            currentSum += when (type) {
                MeasurementType.SYS -> m.systolic.toFloat()
                MeasurementType.DIA -> m.diastolic.toFloat()
                MeasurementType.PULSE -> m.pulse.toFloat()
            }

            if (index >= 7) {
                val oldM = sorted[windowStartIdx]
                currentSum -= when (type) {
                    MeasurementType.SYS -> oldM.systolic.toFloat()
                    MeasurementType.DIA -> oldM.diastolic.toFloat()
                    MeasurementType.PULSE -> oldM.pulse.toFloat()
                }
                windowStartIdx++
            }

            val count = index - windowStartIdx + 1
            Entry(index.toFloat(), currentSum / count)
        }
    }

    fun toggleSlot(slotIndex: Int) {
        val current = _selectedSlots.value
        val isSelected = current.contains(slotIndex)
        
        val canToggleOff = if (_chartMode.value == ChartMode.DISTRIBUTION) {
            current.size > 1
        } else {
            current.size > 1 || _showRollingAverage.value
        }

        if (isSelected && !canToggleOff) return

        _selectedSlots.value = if (isSelected) {
            current - slotIndex
        } else {
            current + slotIndex
        }
    }

    fun toggleType(type: MeasurementType) {
        val current = _selectedTypes.value
        _selectedTypes.value = if (current.contains(type)) {
            if (current.size <= 1) current else current - type
        } else {
            current + type
        }
    }

    fun toggleConfigSheet(open: Boolean) {
        _isConfigSheetOpen.value = open
    }

    fun toggleRiskZones() {
        _showRiskZones.value = !_showRiskZones.value
    }

    fun toggleRollingAverage() {
        if (_showRollingAverage.value && _selectedSlots.value.isEmpty()) return
        _showRollingAverage.value = !_showRollingAverage.value
    }

    fun toggleInteractiveLegend() {
        _showInteractiveLegend.value = !_showInteractiveLegend.value
    }

    fun setChartMode(mode: ChartMode) {
        if (mode == ChartMode.DISTRIBUTION) {
            if (_selectedSlots.value.isEmpty()) {
                _selectedSlots.value = setOf(0, 1, 2, 3)
            }
            _showRollingAverage.value = false
        }
        _chartMode.value = mode
    }

    fun setDatePreset(preset: DatePreset) {
        _datePreset.value = preset
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

    fun setCustomDateRange(from: LocalDate?, to: LocalDate?) {
        _fromDate.value = from
        _toDate.value = to
        _datePreset.value = DatePreset.CUSTOM
        
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
