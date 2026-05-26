package com.otakeessen.underpressure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.export.TableImportManager
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.export.TrackerMappingAction
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import com.otakeessen.underpressure.util.Constants.MIN_SLOT_DIFFERENCE_MINUTES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.Duration
import kotlin.math.abs
import kotlin.math.min

/**
 * ViewModel for the Settings screen.
 * Handles loading and updating measurement slot configurations.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler,
    private val importManager: TableImportManager,
    private val trackerRepository: TrackerRepository,
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentSettings: AppSettingsEntity? = null
    private var selectedImportUri: Uri? = null

    val allTrackers: StateFlow<List<TrackerDefinition>> = trackerRepository.getAllTrackerDefinitions()
        .catch { e -> _uiState.update { it.copy(error = e.message) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSettings()
        refreshPermissionStatus()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getSettings()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { settings ->
                    val entity = settings ?: AppSettingsEntity(
                        bpGuidelines = detectDefaultGuidelines()
                    )
                    
                    // Self-healing: if slot 1 is false in DB, force it to true and save
                    if (!entity.slotActiveFlags.getOrElse(0) { true }) {
                        val healedEntity = entity.copy(
                            slotActiveFlags = entity.slotActiveFlags.toMutableList().apply { this[0] = true }
                        )
                        saveSettings(healedEntity)
                        return@collect
                    }

                    currentSettings = entity
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            slots = entity.toSlotConfigs(),
                            isMasterAlarmEnabled = entity.masterAlarmEnabled,
                            bpGuidelines = entity.bpGuidelines,
                            lastOnboardedVersion = entity.lastOnboardedVersion,
                            error = null
                        )
                    }
                }
        }
    }

    private fun detectDefaultGuidelines(): BpGuidelines {
        return BpGuidelines.ESC_ESH
    }

    fun refreshPermissionStatus() {
        _uiState.update { it.copy(canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()) }
    }

    fun updateMasterAlarmEnabled(isEnabled: Boolean) {
        val settings = currentSettings ?: return
        saveSettings(settings.copy(masterAlarmEnabled = isEnabled))
    }

    fun updateBpGuidelines(guidelines: BpGuidelines) {
        val settings = currentSettings ?: return
        saveSettings(settings.copy(bpGuidelines = guidelines))
    }

    fun updateSlotTime(index: Int, time: String) {
        val settings = currentSettings ?: return

        // Validate time difference from other active slots
        val newTime = LocalTime.parse(time)
        val conflictNeighbor = settings.slotTimes.mapIndexedNotNull { i, t ->
            if (i != index && settings.slotActiveFlags[i]) LocalTime.parse(t) else null
        }.find { otherTime ->
            val diff = abs(Duration.between(newTime, otherTime).toMinutes())
            val wrappedDiff = min(diff, 1440 - diff)
            wrappedDiff < MIN_SLOT_DIFFERENCE_MINUTES
        }

        if (conflictNeighbor != null) {
            _uiState.update { it.copy(error = "hint_cannot_create_slot|$conflictNeighbor") }
            return
        }

        val newTimes = settings.slotTimes.toMutableList().apply {
            this[index] = time
        }
        val newModifiedFlags = settings.slotModifiedFlags.toMutableList().apply {
            this[index] = true
        }
        saveSettings(settings.copy(slotTimes = newTimes, slotModifiedFlags = newModifiedFlags))
    }

    fun updateSlotActive(index: Int, isActive: Boolean) {
        // Slot 1 (index 0) cannot be disabled
        if (index == 0) return

        val settings = currentSettings ?: return
        val newActiveFlags = settings.slotActiveFlags.toMutableList().apply {
            this[index] = isActive
        }
        saveSettings(settings.copy(slotActiveFlags = newActiveFlags))
    }

    fun setOnboardingSeen(version: String) {
        val settings = currentSettings ?: AppSettingsEntity()
        saveSettings(settings.copy(lastOnboardedVersion = version))
    }

    /**
     * Called when a CSV file is selected for import.
     */
    fun onImportCsvUriSelected(uri: Uri) {
        selectedImportUri = uri
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val discoveryResult = importManager.discoverTrackers(uri)
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    trackerDiscoveryResult = if (discoveryResult.discoveredTrackers.isNotEmpty()) discoveryResult else null,
                    showImportStrategyDialog = discoveryResult.discoveredTrackers.isEmpty()
                ) 
            }
        }
    }

    /**
     * Imports data from a CSV file.
     */
    fun onImportCsv(overwrite: Boolean, trackerMapping: Map<String, TrackerMappingAction> = emptyMap()) {
        val uri = selectedImportUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importResult = null, trackerDiscoveryResult = null, showImportStrategyDialog = false) }
            val strategy = if (overwrite) TableImportManager.ImportStrategy.Overwrite 
                           else TableImportManager.ImportStrategy.Skip
            
            val result = importManager.importCsv(uri, strategy, trackerMapping)
            _uiState.update { 
                it.copy(
                    isImporting = false, 
                    importResult = if (result.error != null) result.error 
                                   else {
                                       val msg = "${result.successCount}/${result.totalCount}"
                                       if (result.trackerValuesCount > 0) "$msg (+${result.trackerValuesCount} trackers)" else msg
                                   }
                ) 
            }
            selectedImportUri = null
        }
    }

    /**
     * Cancels the current import process.
     */
    fun cancelImport() {
        selectedImportUri = null
        _uiState.update { it.copy(trackerDiscoveryResult = null, showImportStrategyDialog = false) }
    }

    /**
     * Clears the current error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clears the import result message.
     */
    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    fun showDeleteAllDialog() {
        _uiState.update { it.copy(showDeleteAllConfirmation = true) }
    }

    fun dismissDeleteAllDialog() {
        _uiState.update { it.copy(showDeleteAllConfirmation = false) }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            try {
                // Delete all trackers and measurements
                trackerRepository.deleteAllTrackerDefinitions()
                measurementRepository.deleteAllMeasurements()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete all data: ${e.message}") }
            } finally {
                dismissDeleteAllDialog()
            }
        }
    }

    private fun saveSettings(settings: AppSettingsEntity) {
        viewModelScope.launch {
            try {
                settingsRepository.saveSettings(settings)
                // Schedule/Update alarms after successful save
                alarmScheduler.updateAlarms(settings)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to save settings: ${e.message}") }
            }
        }
    }

    private fun AppSettingsEntity.toSlotConfigs(): List<SlotConfig> {
        return List(4) { i ->
            SlotConfig(
                number = i + 1,
                time = slotTimes.getOrElse(i) { if (i == 0) "07:00" else "12:00" },
                isActive = if (i == 0) true else slotActiveFlags.getOrElse(i) { false },
                isAlarmEnabled = if (i == 0) true else slotActiveFlags.getOrElse(i) { false },
                isToggleable = i > 0
            )
        }
    }
}
