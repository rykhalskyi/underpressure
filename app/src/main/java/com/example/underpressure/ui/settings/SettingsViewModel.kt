package com.example.underpressure.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.underpressure.alarm.AlarmScheduler
import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for the Settings screen.
 * Handles loading and updating measurement slot configurations.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var currentSettings: AppSettingsEntity? = null

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
                    val entity = settings ?: AppSettingsEntity()
                    currentSettings = entity
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            slots = entity.toSlotConfigs(),
                            isMasterAlarmEnabled = entity.masterAlarmEnabled,
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
        saveSettings(settings.copy(slotActiveFlags = newActiveFlags))
    }

    fun updateSlotAlarmEnabled(index: Int, isEnabled: Boolean) {
        val settings = currentSettings ?: return
        val newAlarmsEnabled = settings.slotAlarmsEnabled.toMutableList().apply {
            this[index] = isEnabled
        }
        saveSettings(settings.copy(slotAlarmsEnabled = newAlarmsEnabled))
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
