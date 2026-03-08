package com.example.underpressure.ui.table

import com.example.underpressure.data.local.entities.MeasurementEntity

/**
 * UI State for the Search feature.
 */
data class SearchUiState(
    val query: String = "",
    val results: List<MeasurementEntity> = emptyList(),
    val isLoading: Boolean = false,
    val dateError: String? = null,
    val isNoResults: Boolean = false
)
