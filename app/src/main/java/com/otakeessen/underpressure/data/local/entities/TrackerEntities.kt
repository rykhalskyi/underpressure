package com.otakeessen.underpressure.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.otakeessen.underpressure.domain.TrackerType

/**
 * Entity representing a custom health tracker definition.
 */
@Entity(tableName = "tracker_definitions")
data class TrackerDefinitionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: TrackerType,
    val unit: String? = null,
    val isActive: Boolean = true,
    val showOnChart: Boolean = false,
    val min: Double? = null,
    val max: Double? = null
)

/**
 * Entity representing a value for a specific tracker associated with a measurement.
 */
@Entity(
    tableName = "tracker_values",
    foreignKeys = [
        ForeignKey(
            entity = MeasurementEntity::class,
            parentColumns = ["id"],
            childColumns = ["measurementId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TrackerDefinitionEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["measurementId"]),
        Index(value = ["trackerId"])
    ]
)
data class TrackerValueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val measurementId: Long,
    val trackerId: Long,
    val floatValue: Double? = null,
    val booleanValue: Boolean? = null,
    val stringValue: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
