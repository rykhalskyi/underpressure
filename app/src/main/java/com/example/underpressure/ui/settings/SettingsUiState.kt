package com.example.underpressure.ui.settings

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val slots: List<SlotConfig> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * Configuration for a single measurement slot in the UI.
 */
data class SlotConfig(
    val number: Int,
    val time: String,
    val isActive: Boolean,
    val isAlarmEnabled: Boolean,
    val isToggleable: Boolean
)
