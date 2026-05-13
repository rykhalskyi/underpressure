package com.otakeessen.underpressure.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a blood pressure measurement record in the local database.
 *
 * @property id Unique identifier for the measurement.
 * @property date The date of the measurement (format: YYYY-MM-DD). Indexed for fast retrieval.
 * @property slotIndex Index of the measurement slot in the day (-1 for anytime/flexible readings).
 * @property systolic Systolic blood pressure value.
 * @property diastolic Diastolic blood pressure value.
 * @property pulse Pulse rate value.
 * @property isFlexible Whether this is an anytime reading (not tied to a scheduled slot).
 * @property timestamp Epoch millis when the reading was actually taken (for anytime readings).
 * @property createdAt Timestamp when the record was created.
 * @property updatedAt Timestamp when the record was last updated.
 */
@Entity(
    tableName = "measurements",
    indices = [Index(value = ["date"])]
)
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val slotIndex: Int,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val isFlexible: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

