package com.example.underpressure.ui.chart

import com.github.mikephil.charting.data.LineData
import java.time.LocalDate

/**
 * UI State for the Blood Pressure Chart Screen.
 *
 * @property isLoading Whether the data is being loaded.
 * @property lineData The data to be displayed in the MPAndroidChart.
 * @property selectedSlots Indices of the slots selected for display (0-3).
 * @property selectedTypes Measurement types selected for display (SYS, DIA, PULSE).
 * @property fromDate The start date of the filtering range.
 * @property toDate The end date of the filtering range.
 * @property isConfigSheetOpen Whether the configuration bottom sheet is open.
 * @property errorMessage Error message to be displayed, if any.
 */
data class ChartUiState(
    val isLoading: Boolean = true,
    val bpLineData: LineData? = null,
    val pulseLineData: LineData? = null,
    val startDate: LocalDate? = null,
    val selectedSlots: Set<Int> = setOf(0, 1, 2, 3),
    val selectedTypes: Set<MeasurementType> = setOf(MeasurementType.SYS, MeasurementType.DIA),
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val isConfigSheetOpen: Boolean = false,
    val errorMessageResId: Int? = null,
    val slotTimes: List<String> = emptyList()
)
