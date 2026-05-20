package com.otakeessen.underpressure.ui.table

import androidx.compose.ui.text.input.TextFieldValue
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerValue

/**
 * UI state for the Measurement Edit Dialog.
 */
data class MeasurementDialogState(
    val isOpen: Boolean = false,
    val date: String = "",
    val slotIndex: Int = 0,
    val slotTime: String = "",
    val initialValue: String = "",
    val inputValue: TextFieldValue = TextFieldValue(""),
    val existingMeasurementId: Long? = null,
    val isGuidanceVisible: Boolean = false,
    val suggestedSlotTime: String = "",
    val isFlexibleMode: Boolean = false,
    val isAnytimeConfirmationVisible: Boolean = false,
    val nearestSlotLabel: String = "",
    val trackerValues: Map<Long, TrackerValue> = emptyMap(), // trackerId -> TrackerValue
    val activeTrackers: List<TrackerDefinition> = emptyList()
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
 * @property isGuidanceRequired True if clicking the FAB should show guidance instead of the edit dialog.
 * @property fabHint Optional hint message to show when FAB is clicked (or if disabled).
 * @property isMasterAlarmEnabled True if the global alarm reminder switch is ON.
 * @property isAllView True to show anytime readings alongside scheduled slots in the table.
 * @property activeTrackers List of trackers that are currently active and should be shown in the table.
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
    val isFabEnabled: Boolean = true,
    val fabTargetSlotIndex: Int? = null,
    val isGuidanceRequired: Boolean = false,
    val fabHint: String? = null,
    val isMasterAlarmEnabled: Boolean = false,
    val isSummaryVisible: Boolean = true,
    val isAllView: Boolean = false,
    val activeTrackers: List<TrackerDefinition> = emptyList(),
    val activeGuidelines: BpGuidelines = BpGuidelines.ESC_ESH,
    val error: String? = null,
    val classificationStats: Map<BloodPressureLevel, Int> = emptyMap()
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

    /**
     * Section listing anytime (flexible) readings for a given date.
     */
    data class AnytimeSection(
        val date: String,
        val readings: List<AnytimeReadingData>
    ) : TableItem()
}

