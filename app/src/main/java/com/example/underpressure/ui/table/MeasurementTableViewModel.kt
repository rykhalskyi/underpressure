package com.example.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import com.example.underpressure.domain.validation.BloodPressureValidator
import com.example.underpressure.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel for the Measurement Table Screen.
 * Transforms raw measurements and settings into a summarized table format.
 */
class MeasurementTableViewModel(
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val validator = BloodPressureValidator()

    private val _dialogState = MutableStateFlow(MeasurementDialogState())

    val uiState: StateFlow<TableUiState> = combine(
        measurementRepository.getAllMeasurements(),
        settingsRepository.getSettings(),
        _dialogState
    ) { measurements, settings, dialogState ->
        val today = LocalDate.now().format(dateFormatter)
        
        // Use default if settings are null
        val activeFlags = settings?.slotActiveFlags ?: listOf(true, false, false, false)
        val allTimes = settings?.slotTimes ?: listOf("07:00", "12:00", "18:00", "22:00")
        
        // Filter headers by active status
        val headers = allTimes.filterIndexed { index, _ -> activeFlags.getOrElse(index) { false } }
        val activeIndices = activeFlags.mapIndexedNotNull { index, active -> if (active) index else null }
        
        val summarizedItems = measurements
            .groupBy { it.date }
            .map { (date, dailyMeasurements) ->
                // Map only active slots to a 0-indexed map for DayRow
                val activeSlots = activeIndices.mapIndexedNotNull { uiIndex, originalIndex ->
                    dailyMeasurements.find { it.slotIndex == originalIndex }?.let { 
                        uiIndex to SlotData(it.systolic, it.diastolic, it.pulse)
                    }
                }.toMap()

                DayMeasurementSummary(
                    date = date,
                    slots = activeSlots,
                    isToday = date == today
                )
            }
            .sortedByDescending { it.date }

        TableUiState(
            isLoading = false,
            slotHeaders = headers,
            items = summarizedItems,
            dialogState = dialogState
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TableUiState(isLoading = true)
    )

    /**
     * Called when a table cell is clicked.
     */
    fun onCellClicked(date: String, uiSlotIndex: Int) {
        viewModelScope.launch {
            // We need to find the original slot index based on settings
            val settings = settingsRepository.getSettingsSync() 
            val activeFlags = settings?.slotActiveFlags ?: listOf(true, false, false, false)
            val activeIndices = activeFlags.mapIndexedNotNull { index, active -> if (active) index else null }
            val originalIndex = activeIndices.getOrNull(uiSlotIndex) ?: return@launch

            // Find existing measurement if any
            // Note: For simplicity, we could fetch all measurements and filter, 
            // but repository has getMeasurementsByDate.
            // However, we already have them in the flow. 
            // Let's use the repository to be sure we get the ID.
            val existing = measurementRepository.getMeasurementsByDateSync(date)
                .find { it.slotIndex == originalIndex }

            val initialValue = existing?.let { "${it.systolic}/${it.diastolic} @${it.pulse}" } ?: ""

            _dialogState.update {
                it.copy(
                    isOpen = true,
                    date = date,
                    slotIndex = originalIndex,
                    initialValue = initialValue,
                    existingMeasurementId = existing?.id
                )
            }
        }
    }

    /**
     * Dismisses the edit dialog.
     */
    fun onDialogDismiss() {
        _dialogState.update { MeasurementDialogState() }
    }

    /**
     * Saves or updates the measurement.
     */
    fun onSaveMeasurement(input: String) {
        val currentState = _dialogState.value
        val validationResult = validator.validate(input)

        if (validationResult is ValidationResult.Success) {
            viewModelScope.launch {
                val entity = MeasurementEntity(
                    id = currentState.existingMeasurementId ?: 0,
                    date = currentState.date,
                    slotIndex = currentState.slotIndex,
                    systolic = validationResult.systolic,
                    diastolic = validationResult.diastolic,
                    pulse = validationResult.pulse,
                    updatedAt = System.currentTimeMillis()
                )

                if (currentState.existingMeasurementId == null) {
                    measurementRepository.saveMeasurement(entity)
                } else {
                    measurementRepository.updateMeasurement(entity)
                }
                onDialogDismiss()
            }
        }
    }
}
