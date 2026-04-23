package com.otakeessen.underpressure.ui.table

import com.otakeessen.underpressure.data.local.entities.MeasurementEntity

/**
 * UI State for the Search feature.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<MeasurementEntity> = emptyList(),
    val isLoading: Boolean = false,
    val dateErrorRes: Int? = null,
    val isNoResults: Boolean = false
)

