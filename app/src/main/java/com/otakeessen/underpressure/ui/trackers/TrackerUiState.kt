package com.otakeessen.underpressure.ui.trackers

import com.otakeessen.underpressure.domain.TrackerDefinition

/**
 * UI state for the Tracker Management screen.
 */
data class TrackerUiState(
    val trackers: List<TrackerDefinition> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
