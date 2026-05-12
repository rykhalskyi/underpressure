package com.otakeessen.underpressure.ui.chart

import com.github.mikephil.charting.data.LineData
import java.time.LocalDate

/**
 * Modes for displaying the chart data.
 */
enum class ChartMode {
    /**
     * Grouped by date, X-axis represents calendar days.
     */
    DAILY,

    /**
     * Continuous flow of measurements, X-axis represents measurement index.
     */
    SEQUENTIAL
}

/**
 * Presets for quick date range selection.
 */
enum class DatePreset {
    ALL_TIME,
    LAST_7_DAYS,
    THIS_MONTH,
    CUSTOM
}

/**
 * UI State for the Blood Pressure Chart Screen.
 *
 * @property isLoading Whether the data is being loaded.
 * @property sysLineData Line data for systolic.
 * @property diaLineData Line data for diastolic.
 * @property pulseLineData Line data for pulse.
 * @property startDate The reference start date for DAILY mode.
 * @property selectedSlots Indices of the slots selected for display (0-3).
 * @property selectedTypes Measurement types selected for display (SYS, DIA, PULSE).
 * @property fromDate The start date of the filtering range.
 * @property toDate The end date of the filtering range.
 * @property isConfigSheetOpen Whether the configuration bottom sheet is open.
 * @property errorMessageResId String resource ID for the error message.
 * @property slotTimes Labels for the time slots.
 * @property chartMode The current display mode (Daily/Sequential).
 * @property xLabels Mapping of X-axis values to their formatted labels (e.g., "Oct 12").
 * @property showRiskZones Whether to display risk zone background bands.
 * @property showRollingAverage Whether to display 7-day rolling average lines.
 * @property showInteractiveLegend Whether to display the interactive legend overlay.
 */
data class ChartUiState(
    val isLoading: Boolean = true,
    val sysLineData: LineData? = null,
    val diaLineData: LineData? = null,
    val pulseLineData: LineData? = null,
    val startDate: LocalDate? = null,
    val selectedSlots: Set<Int> = setOf(0, 1, 2, 3),
    val selectedTypes: Set<MeasurementType> = setOf(MeasurementType.SYS, MeasurementType.DIA),
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val isConfigSheetOpen: Boolean = false,
    val errorMessageResId: Int? = null,
    val slotTimes: List<String> = emptyList(),
    val chartMode: ChartMode = ChartMode.DAILY,
    val xLabels: Map<Float, String> = emptyMap(),
    val selectedDatePreset: DatePreset = DatePreset.ALL_TIME,
    val showRiskZones: Boolean = false,
    val showRollingAverage: Boolean = false,
    val showInteractiveLegend: Boolean = false
)
