package com.otakeessen.underpressure.domain.export

import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType

/**
 * Represents a custom tracker found in a CSV file during the discovery phase.
 */
data class DiscoveredTracker(
    val columnIndex: Int,
    val headerName: String,
    val extractedName: String,
    val unit: String?,
    val type: TrackerType?,
    val matchStatus: TrackerMatchStatus,
    val existingDefinition: TrackerDefinition? = null
)

/**
 * Categories for discovered trackers compared against the local database.
 */
enum class TrackerMatchStatus {
    /** Name and unit match an existing tracker definition. */
    EXACT_MATCH,
    /** Name matches an existing tracker, but the unit differs. */
    CONFLICT,
    /** No matching tracker name found in the database. */
    NEW
}

/**
 * Result of the tracker discovery phase.
 */
data class TrackerDiscoveryResult(
    val discoveredTrackers: List<DiscoveredTracker>,
    val totalColumns: Int
)

/**
 * User-selected actions for mapping discovered trackers to the database.
 */
sealed class TrackerMappingAction {
    data class MapToExisting(val trackerId: Long) : TrackerMappingAction()
    data class CreateNew(val name: String, val type: TrackerType, val unit: String?) : TrackerMappingAction()
    object Skip : TrackerMappingAction()
}
