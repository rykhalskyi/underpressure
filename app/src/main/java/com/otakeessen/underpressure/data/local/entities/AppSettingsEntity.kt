package com.otakeessen.underpressure.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.otakeessen.underpressure.domain.BpGuidelines

/**
 * Represents application settings in the local database.
 * This is intended to be a singleton record with a fixed ID.
 *
 * @property id Fixed identifier for the settings record (default: 1).
 * @property masterAlarmEnabled Whether the master alarm is enabled.
 * @property slotTimes List of strings representing times for each slot.
 * @property slotActiveFlags List of booleans representing whether each slot is active.
 * @property slotModifiedFlags List of booleans representing whether each slot has been modified by the user.
 * @property lastOnboardedVersion The last version of the app the user was onboarded to.
 */
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val masterAlarmEnabled: Boolean = false,
    val slotTimes: List<String> = listOf("07:00", "12:00", "18:00", "22:00"),
    val slotActiveFlags: List<Boolean> = listOf(true, false, false, false),
    val slotModifiedFlags: List<Boolean> = listOf(false, false, false, false),
    val lastOnboardedVersion: String? = null,
    val bpGuidelines: BpGuidelines = BpGuidelines.AHA_ACC
)

