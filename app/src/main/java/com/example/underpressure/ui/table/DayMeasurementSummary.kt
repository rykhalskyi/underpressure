package com.example.underpressure.ui.table

/**
 * Data model for a single measurement slot's values.
 */
data class SlotData(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int
)

/**
 * Data model for a generic measurement entry in the UI.
 */
data class GenericSlotData(
    val listId: Long,
    val listName: String,
    val value: String,
    val type: String // MeasurementListType as string
)

/**
 * Data model representing a summarized measurement for a single day with multiple slots.
 *
 * @property date The date of the measurement in YYYY-MM-DD format.
 * @property slots Map of slotIndex to SlotData.
 * @property genericEntries Map of slotIndex to a list of generic measurements for that slot.
 * @property isToday Whether the date corresponds to the current system date.
 */
data class DayMeasurementSummary(
    val date: String,
    val slots: Map<Int, SlotData> = emptyMap(),
    val genericEntries: Map<Int, List<GenericSlotData>> = emptyMap(),
    val isToday: Boolean = false,
)
