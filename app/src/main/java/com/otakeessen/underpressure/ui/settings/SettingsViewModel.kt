package com.otakeessen.underpressure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.export.TableImportManager
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random

/**
 * ViewModel for the Settings screen.
 * Handles loading and updating measurement slot configurations.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val measurementRepository: MeasurementRepository,
    private val alarmScheduler: AlarmScheduler,
    private val importManager: TableImportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentSettings: AppSettingsEntity? = null
    private var selectedImportUri: Uri? = null

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
                    var entity = settings ?: AppSettingsEntity()
                    
                    // Bug fix: Ensure slot 1 alarm is enabled if it's active but alarm is disabled.
                    // This can happen for existing users because slot 1 cannot be toggled in the UI.
                    if (entity.slotActiveFlags.getOrNull(0) == true && entity.slotAlarmsEnabled.getOrNull(0) == false) {
                        val fixedAlarms = entity.slotAlarmsEnabled.toMutableList().apply { this[0] = true }
                        val fixedSettings = entity.copy(slotAlarmsEnabled = fixedAlarms)
                        saveSettings(fixedSettings)
                        return@collect
                    }

                    currentSettings = entity
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            slots = entity.toSlotConfigs(),
                            isMasterAlarmEnabled = entity.masterAlarmEnabled,
                            lastOnboardedVersion = entity.lastOnboardedVersion,
                            error = null
                        )
                    }
                }
        }
    }

    fun refreshPermissionStatus() {
        _uiState.update { it.copy(canScheduleExactAlarms = alarmScheduler.canScheduleExactAlarms()) }
    }

    fun updateMasterAlarmEnabled(isEnabled: Boolean) {
        val settings = currentSettings ?: return
        saveSettings(settings.copy(masterAlarmEnabled = isEnabled))
    }

    fun updateSlotTime(index: Int, time: String) {
        val settings = currentSettings ?: return
        val newTimes = settings.slotTimes.toMutableList().apply {
            this[index] = time
        }
        saveSettings(settings.copy(slotTimes = newTimes))
    }

    fun updateSlotActive(index: Int, isActive: Boolean) {
        // Slot 1 (index 0) cannot be disabled
        if (index == 0) return

        val settings = currentSettings ?: return
        val newActiveFlags = settings.slotActiveFlags.toMutableList().apply {
            this[index] = isActive
        }
        val newAlarmsEnabled = settings.slotAlarmsEnabled.toMutableList().apply {
            this[index] = isActive
        }
        saveSettings(settings.copy(slotActiveFlags = newActiveFlags, slotAlarmsEnabled = newAlarmsEnabled))
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
    }

    /**
     * Imports data from a CSV file.
     */
    fun onImportCsv(overwrite: Boolean) {
        val uri = selectedImportUri ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, importResult = null) }
            val strategy = if (overwrite) TableImportManager.ImportStrategy.Overwrite 
                           else TableImportManager.ImportStrategy.Skip
            
            val result = importManager.importCsv(uri, strategy)
            _uiState.update { 
                it.copy(
                    isImporting = false, 
                    importResult = if (result.error != null) result.error 
                                   else "${result.successCount}/${result.totalCount}"
                ) 
            }
            selectedImportUri = null
        }
    }

    /**
     * Clears the import result message.
     */
    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    /**
     * Populates the database with debug measurement data for the last 30 days.
     */
    fun populateDebugData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true) }
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val calendar = Calendar.getInstance()
                
                for (i in 0 until 30) {
                    val date = dateFormat.format(calendar.time)
                    
                    // Add 2-4 measurements per day
                    val measurementsCount = Random.nextInt(2, 5)
                    for (slot in 0 until measurementsCount) {
                        val measurement = MeasurementEntity(
                            date = date,
                            slotIndex = slot,
                            systolic = Random.nextInt(110, 150),
                            diastolic = Random.nextInt(70, 100),
                            pulse = Random.nextInt(60, 90)
                        )
                        measurementRepository.saveMeasurement(measurement)
                    }
                    calendar.add(Calendar.DAY_OF_YEAR, -1)
                }
                _uiState.update { it.copy(isImporting = false, importResult = "30 days of data generated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, error = "Debug population failed: ${e.message}") }
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
                isActive = slotActiveFlags.getOrElse(i) { i == 0 },
                isAlarmEnabled = slotAlarmsEnabled.getOrElse(i) { false },
                isToggleable = i > 0
            )
        }
    }
}

