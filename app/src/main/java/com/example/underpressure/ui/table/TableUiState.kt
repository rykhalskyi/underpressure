package com.example.underpressure.ui.table

/**
 * UI State for the Daily Measurements Table screen.
 *
 * @property isLoading True when the measurements are being loaded from the repository.
 * @property items List of summarized daily measurements to display in the table.
 * @property error Error message to display if the data load fails.
 */
data class TableUiState(
    val isLoading: Boolean = false,
    val items: List<DayMeasurementSummary> = emptyList(),
    val error: String? = null,
)
