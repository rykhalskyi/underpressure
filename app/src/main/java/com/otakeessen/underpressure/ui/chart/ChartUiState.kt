package com.otakeessen.underpressure.ui.chart

import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.PieData
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerValue
import java.time.LocalDate

/**
 * Modes for displaying the chart data.
 */
enum class ChartMode {
    /**
     * Grouped by date, X-axis represents calendar days, grouped by slots.
     */
    TREND_BY_SLOT,

    /**
     * Continuous flow of all measurements, X-axis represents individual reading instances.
     */
    CHRONOLOGICAL,

    /**
     * Statistical breakdown of blood pressure levels.
     */
    SUMMARY
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
 * @property trackerLineData Map of tracker ID to LineData for custom trackers.
 * @property trackerValuesMap Mapping of X-axis coordinate to associated list of tracker values.
 * @property distributionBarData Bar data for blood pressure level distribution.
 * @property distributionPieData Pie data for blood pressure level distribution.
 * @property startDate The reference start date for DAILY mode.
 * @property selectedSlots Indices of the slots selected for display (0-3).
 * @property selectedTypes Measurement types selected for display (SYS, DIA, PULSE).
 * @property fromDate The start date of the filtering range.
 * @property toDate The end date of the filtering range.
 * @property isConfigSheetOpen Whether the configuration bottom sheet is open.
 * @property errorMessageResId String resource ID for the error message.
 * @property slotTimes Labels for the time slots.
 * @property chartMode The current display mode (Daily/Sequential/Distribution).
 * @property xLabels Mapping of X-axis values to their formatted labels (e.g., "Oct 12").
 * @property showRiskZones Whether to display risk zone background bands.
 * @property showRollingAverage Whether to display 7-day rolling average lines.
 * @property showInteractiveLegend Whether to display the interactive legend overlay.
 * @property activeTrackers List of trackers that are currently active and configured for chart display.
 */
data class ChartUiState(
    val isLoading: Boolean = true,
    val sysLineData: LineData? = null,
    val diaLineData: LineData? = null,
    val pulseLineData: LineData? = null,
    val trackerLineData: Map<Long, LineData> = emptyMap(),
    val trackerValuesMap: Map<Float, List<TrackerValue>> = emptyMap(),
    val trackerDefinitionsMap: Map<Long, TrackerDefinition> = emptyMap(),
    val distributionBarData: BarData? = null,
    val distributionPieData: PieData? = null,
    val startDate: LocalDate? = null,
    val selectedSlots: Set<Int> = setOf(0, 1, 2, 3),
    val selectedTypes: Set<MeasurementType> = setOf(MeasurementType.SYS, MeasurementType.DIA),
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val isConfigSheetOpen: Boolean = false,
    val errorMessageResId: Int? = null,
    val slotTimes: List<String> = emptyList(),
    val chartMode: ChartMode = ChartMode.TREND_BY_SLOT,
    val xLabels: Map<Float, String> = emptyMap(),
    val selectedDatePreset: DatePreset = DatePreset.ALL_TIME,
    val showRiskZones: Boolean = false,
    val showRollingAverage: Boolean = false,
    val showInteractiveLegend: Boolean = false,
    val slotColors: List<Int> = emptyList(),
    val levelColors: List<Int> = emptyList(),
    val typeLabelResIds: Map<MeasurementType, Int> = emptyMap(),
    val activeTrackers: List<TrackerDefinition> = emptyList()
)
