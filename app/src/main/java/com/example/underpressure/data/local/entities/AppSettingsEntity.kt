package com.example.underpressure.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents application settings in the local database.
 * This is intended to be a singleton record with a fixed ID.
 *
 * @property id Fixed identifier for the settings record (default: 1).
 * @property masterAlarmEnabled Whether the master alarm is enabled.
 * @property slotTimes List of strings representing times for each slot.
 * @property slotAlarmsEnabled List of booleans representing whether alarms are enabled for each slot.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val masterAlarmEnabled: Boolean = false,
    val slotTimes: List<String> = emptyList(),
    val slotAlarmsEnabled: List<Boolean> = emptyList(),
)
