package com.otakeessen.underpressure.ui.chart

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.data.export.ChartExportManager
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
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
    private val chartExportManager: ChartExportManager
) : ViewModel() {

    private val _selectedSlots = MutableStateFlow(setOf(0, 1, 2, 3))
    private val _selectedTypes = MutableStateFlow(setOf(MeasurementType.SYS, MeasurementType.DIA, MeasurementType.PULSE))
    private val _fromDate = MutableStateFlow<LocalDate?>(null)
    private val _toDate = MutableStateFlow<LocalDate?>(null)
    private val _chartMode = MutableStateFlow(ChartMode.DAILY)
    private val _datePreset = MutableStateFlow(DatePreset.ALL_TIME)
    private val _isConfigSheetOpen = MutableStateFlow(false)

    sealed class ChartEvent {
        data class ShareFile(val file: File) : ChartEvent()
        data class Error(val messageResId: Int, val arg: String? = null) : ChartEvent()
    }

    private val _events = MutableSharedFlow<ChartEvent>()
    val events = _events.asSharedFlow()

    private val configFlow = combine(
        _selectedSlots,
        _selectedTypes,
        _fromDate,
        _toDate,
        combine(_chartMode, _datePreset, _isConfigSheetOpen) { mode, preset, open -> 
            Triple(mode, preset, open)
        }
    ) { slots, types, from, to, (mode, preset, open) ->
        ConfigState(slots, types, from, to, mode, preset, open)
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
                slotTimes = slotTimes
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
                slotTimes = slotTimes
            )
        }

        // Find min date for X-axis baseline (0-indexed days) in DAILY mode
        val minDateStr = filtered.minBy { it.date }.date
        val minDate = LocalDate.parse(minDateStr, DATE_FORMATTER)

        val sysDataSets = mutableListOf<LineDataSet>()
        val diaDataSets = mutableListOf<LineDataSet>()
        val pulseDataSets = mutableListOf<LineDataSet>()
        val xLabels = mutableMapOf<Float, String>()

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
        } else {
            // Logic for SEQUENTIAL mode (One plot for all slots)
            val sequentialMeasurements = filtered
                .filter { config.slots.contains(it.slotIndex) }
                .sortedWith(compareBy({ it.date }, { it.slotIndex }))
            
            sequentialMeasurements.forEachIndexed { index, m ->
                val date = LocalDate.parse(m.date, DATE_FORMATTER)
                val formattedDate = date.format(DateTimeFormatter.ofPattern("MMM dd"))
                val slotTimeLabel = slotTimes.getOrElse(m.slotIndex) { "Slot ${m.slotIndex + 1}" }
                xLabels[index.toFloat()] = "$formattedDate\n$slotTimeLabel"
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
        }

        ChartUiState(
            isLoading = false,
            sysLineData = if (sysDataSets.isNotEmpty()) LineData(sysDataSets.toList()) else null,
            diaLineData = if (diaDataSets.isNotEmpty()) LineData(diaDataSets.toList()) else null,
            pulseLineData = if (pulseDataSets.isNotEmpty()) LineData(pulseDataSets.toList()) else null,
            startDate = minDate,
            selectedSlots = config.slots,
            selectedTypes = config.types,
            fromDate = config.fromDate,
            toDate = config.toDate,
            chartMode = config.mode,
            selectedDatePreset = config.preset,
            isConfigSheetOpen = config.isOpen,
            errorMessageResId = if (sysDataSets.isEmpty() && diaDataSets.isEmpty() && pulseDataSets.isEmpty()) R.string.error_no_slots_selected else null,
            slotTimes = slotTimes,
            xLabels = xLabels
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
    }

    private data class ConfigState(
        val slots: Set<Int>,
        val types: Set<MeasurementType>,
        val fromDate: LocalDate?,
        val toDate: LocalDate?,
        val mode: ChartMode,
        val preset: DatePreset,
        val isOpen: Boolean
    )

    fun toggleSlot(slotIndex: Int) {
        _selectedSlots.value = if (_selectedSlots.value.contains(slotIndex)) {
            _selectedSlots.value - slotIndex
        } else {
            _selectedSlots.value + slotIndex
        }
    }

    fun toggleType(type: MeasurementType) {
        _selectedTypes.value = if (_selectedTypes.value.contains(type)) {
            _selectedTypes.value - type
        } else {
            _selectedTypes.value + type
        }
    }

    fun toggleConfigSheet(open: Boolean) {
        _isConfigSheetOpen.value = open
    }

    fun setChartMode(mode: ChartMode) {
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
