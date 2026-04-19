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
 * @property displayItems Flattened list of items to display in the hierarchical list.
 * @property expandedYears Set of years that are currently expanded.
 * @property expandedMonths Set of year-months (YYYY-MM) that are currently expanded.
 * @property dialogState State for the measurement entry/edit dialog.
 * @property isFabEnabled True if a slot is currently eligible for measurement entry.
 * @property fabTargetSlotIndex The slot index the FAB should target, if enabled.
 * @property isMasterAlarmEnabled True if the global alarm reminder switch is ON.
 * @property error Error message if data load fails.
 */
data class TableUiState(
    val isLoading: Boolean = false,
    val slotHeaders: List<String> = emptyList(),
    val items: List<DayMeasurementSummary> = emptyList(),
    val displayItems: List<TableItem> = emptyList(),
    val expandedYears: Set<Int> = emptySet(),
    val expandedMonths: Set<String> = emptySet(),
    val dialogState: MeasurementDialogState = MeasurementDialogState(),
    val isFabEnabled: Boolean = false,
    val fabTargetSlotIndex: Int? = null,
    val isMasterAlarmEnabled: Boolean = false,
    val error: String? = null,
)

/**
 * Sealed class representing items in the hierarchical measurement table.
 */
sealed class TableItem {
    /**
     * Header for a specific year.
     */
    data class YearHeader(val year: Int, val isExpanded: Boolean) : TableItem()

    /**
     * Header for a specific month.
     * @property yearMonth String in YYYY-MM format.
     */
    data class MonthHeader(
        val yearMonth: String,
        val monthName: String,
        val isExpanded: Boolean,
        val summary: String? = null
    ) : TableItem()

    /**
     * Row for a specific day's measurements.
     */
    data class DayRow(val summary: DayMeasurementSummary) : TableItem()
}
