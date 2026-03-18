package com.example.underpressure.ui.chart

import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.underpressure.data.export.ChartExportManager
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
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

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _selectedSlots = MutableStateFlow(setOf(0, 1, 2, 3))
    private val _selectedTypes = MutableStateFlow(setOf(MeasurementType.SYS, MeasurementType.DIA))
    private val _fromDate = MutableStateFlow<LocalDate?>(null)
    private val _toDate = MutableStateFlow<LocalDate?>(null)
    private val _isConfigSheetOpen = MutableStateFlow(false)

    sealed class ChartEvent {
        data class ShareFile(val file: File) : ChartEvent()
        data class Error(val message: String) : ChartEvent()
    }

    private val _events = MutableSharedFlow<ChartEvent>()
    val events = _events.asSharedFlow()

    // Slot colors (Distinguishable)
    private val slotColors = listOf(
        Color.parseColor("#2196F3"), // Blue
        Color.parseColor("#4CAF50"), // Green
        Color.parseColor("#FF9800"), // Orange
        Color.parseColor("#E91E63")  // Pink
    )

    private val configFlow = combine(
        _selectedSlots,
        _selectedTypes,
        _fromDate,
        _toDate,
        _isConfigSheetOpen
    ) { slots, types, from, to, open ->
        ConfigState(slots, types, from, to, open)
    }

    val uiState: StateFlow<ChartUiState> = combine(
        measurementRepository.getAllMeasurements(),
        configFlow
    ) { measurements: List<MeasurementEntity>, config: ConfigState ->
        
        if (measurements.isEmpty()) {
            return@combine ChartUiState(
                isLoading = false,
                lineData = null,
                selectedSlots = config.slots,
                selectedTypes = config.types,
                fromDate = config.fromDate,
                toDate = config.toDate,
                isConfigSheetOpen = config.isOpen,
                errorMessage = "No data available"
            )
        }

        // Filter by date range
        val filtered = measurements.filter { m ->
            val date = LocalDate.parse(m.date, dateFormatter)
            val afterFrom = config.fromDate == null || !date.isBefore(config.fromDate)
            val beforeTo = config.toDate == null || !date.isAfter(config.toDate)
            afterFrom && beforeTo
        }

        if (filtered.isEmpty()) {
             return@combine ChartUiState(
                isLoading = false,
                lineData = null,
                selectedSlots = config.slots,
                selectedTypes = config.types,
                fromDate = config.fromDate,
                toDate = config.toDate,
                isConfigSheetOpen = config.isOpen,
                errorMessage = "No data in selected date range"
            )
        }

        // Find min date for X-axis baseline (0-indexed days)
        val minDateStr = filtered.minBy { it.date }.date
        val minDate = LocalDate.parse(minDateStr, dateFormatter)

        val dataSets = mutableListOf<LineDataSet>()

        // Generate datasets per slot and measurement type
        config.slots.forEach { slotIndex ->
            val slotMeasurements = filtered.filter { it.slotIndex == slotIndex }
            if (slotMeasurements.isNotEmpty()) {
                config.types.forEach { type ->
                    val entries = slotMeasurements.map { m ->
                        val date = LocalDate.parse(m.date, dateFormatter)
                        val days = ChronoUnit.DAYS.between(minDate, date).toFloat()
                        val value = when (type) {
                            MeasurementType.SYS -> m.systolic.toFloat()
                            MeasurementType.DIA -> m.diastolic.toFloat()
                            MeasurementType.PULSE -> m.pulse.toFloat()
                        }
                        Entry(days, value)
                    }.sortedBy { it.x }

                    val label = "Slot ${slotIndex + 1} - ${type.name}"
                    val dataSet = LineDataSet(entries, label).apply {
                        val colorVal = slotColors.getOrElse(slotIndex) { Color.BLACK }
                        color = colorVal
                        setCircleColor(colorVal)
                        lineWidth = when (type) {
                            MeasurementType.SYS -> 3f
                            MeasurementType.DIA -> 1.5f
                            MeasurementType.PULSE -> 1.5f
                        }
                        if (type == MeasurementType.PULSE) {
                            enableDashedLine(10f, 10f, 0f)
                        }
                        mode = LineDataSet.Mode.LINEAR
                        setDrawValues(false)
                    }
                    dataSets.add(dataSet)
                }
            }
        }

        ChartUiState(
            isLoading = false,
            lineData = if (dataSets.isNotEmpty()) LineData(dataSets.toList()) else null,
            selectedSlots = config.slots,
            selectedTypes = config.types,
            fromDate = config.fromDate,
            toDate = config.toDate,
            isConfigSheetOpen = config.isOpen,
            errorMessage = if (dataSets.isEmpty()) "Select at least one slot and type" else null
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ChartUiState(isLoading = true)
    )

    private data class ConfigState(
        val slots: Set<Int>,
        val types: Set<MeasurementType>,
        val fromDate: LocalDate?,
        val toDate: LocalDate?,
        val isOpen: Boolean
    )

    fun toggleConfigSheet(open: Boolean) {
        _isConfigSheetOpen.value = open
    }

    fun updateConfiguration(
        slots: Set<Int>,
        types: Set<MeasurementType>,
        from: LocalDate?,
        to: LocalDate?
    ) {
        _selectedSlots.value = slots
        _selectedTypes.value = types
        _fromDate.value = from
        _toDate.value = to
        _isConfigSheetOpen.value = false
    }

    fun onShareChart(bitmap: android.graphics.Bitmap) {
        viewModelScope.launch {
            try {
                val file = chartExportManager.saveChartToCache(bitmap)
                _events.emit(ChartEvent.ShareFile(file))
            } catch (e: Exception) {
                _events.emit(ChartEvent.Error("Failed to export chart: ${e.message}"))
            }
        }
    }
}
