package com.example.underpressure.ui.table

/**
 * Data model representing a summarized measurement for a single day.
 *
 * @property date The date of the measurement in YYYY-MM-DD format.
 * @property systolic Latest systolic value for the day, if any.
 * @property diastolic Latest diastolic value for the day, if any.
 * @property pulse Latest pulse value for the day, if any.
 * @property isToday Whether the date corresponds to the current system date.
 */
data class DayMeasurementSummary(
    val date: String,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val isToday: Boolean = false,
)
