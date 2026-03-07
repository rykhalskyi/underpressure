package com.example.underpressure.ui.table

/**
 * UI state for the Measurement Edit Dialog.
 */
data class MeasurementDialogState(
    val isOpen: Boolean = false,
    val date: String = "",
    val slotIndex: Int = 0,
    val initialValue: String = "",
    val existingMeasurementId: Long? = null
)

/**
 * UI State for the Daily Measurements Table screen.
 *
 * @property isLoading True when the measurements are being loaded.
 * @property slotHeaders List of times (strings) to be used as column headers for slots.
 * @property items List of summarized daily measurements to display.
 * @property dialogState State for the measurement entry/edit dialog.
 * @property error Error message if data load fails.
 */
data class TableUiState(
    val isLoading: Boolean = false,
    val slotHeaders: List<String> = emptyList(),
    val items: List<DayMeasurementSummary> = emptyList(),
    val dialogState: MeasurementDialogState = MeasurementDialogState(),
    val isFabEnabled: Boolean = false,
    val fabTargetSlotIndex: Int? = null,
    val error: String? = null,
)
