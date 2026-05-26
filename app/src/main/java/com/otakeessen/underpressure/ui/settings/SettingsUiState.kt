package com.otakeessen.underpressure.ui.settings

import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.export.TrackerDiscoveryResult

/**
 * UI state for the Settings screen.
 */
data class SettingsUiState(
    val slots: List<SlotConfig> = emptyList(),
    val isMasterAlarmEnabled: Boolean = false,
    val bpGuidelines: BpGuidelines = BpGuidelines.ESC_ESH,
    val canScheduleExactAlarms: Boolean = true,
    val lastOnboardedVersion: String? = null,
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val showImportStrategyDialog: Boolean = false,
    val showDeleteAllConfirmation: Boolean = false,
    val trackerDiscoveryResult: TrackerDiscoveryResult? = null,
    val importResult: String? = null,
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

