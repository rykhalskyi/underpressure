package com.example.underpressure.ui.table

/**
 * UI State for the Daily Measurements Table screen.
 *
 * @property isLoading True when the measurements are being loaded.
 * @property slotHeaders List of times (strings) to be used as column headers for slots.
 * @property items List of summarized daily measurements to display.
 * @property error Error message if data load fails.
 */
data class TableUiState(
    val isLoading: Boolean = false,
    val slotHeaders: List<String> = emptyList(),
    val items: List<DayMeasurementSummary> = emptyList(),
    val error: String? = null,
)
