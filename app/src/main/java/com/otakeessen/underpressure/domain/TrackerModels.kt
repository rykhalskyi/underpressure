package com.otakeessen.underpressure.domain

/**
 * Supported data types for custom trackers.
 */
enum class TrackerType {
    FLOAT,
    BOOLEAN,
    STRING
}

/**
 * Domain model for a tracker definition.
 */
data class TrackerDefinition(
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
 * Domain model for a tracker value associated with a measurement.
 */
data class TrackerValue(
    val id: Long = 0,
    val measurementId: Long,
    val trackerId: Long,
    val floatValue: Double? = null,
    val booleanValue: Boolean? = null,
    val stringValue: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
