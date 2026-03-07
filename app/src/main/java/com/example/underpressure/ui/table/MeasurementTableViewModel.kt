package com.example.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import com.example.underpressure.domain.validation.BloodPressureValidator
import com.example.underpressure.domain.validation.ValidationResult
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.Duration
import kotlin.math.abs

/**
 * ViewModel for the Measurement Table Screen.
 * Transforms raw measurements and settings into a summarized table format.
 */
class MeasurementTableViewModel(
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val validator = BloodPressureValidator()

    private val _dialogState = MutableStateFlow(MeasurementDialogState())
    private val manualRefreshTrigger = MutableStateFlow(System.currentTimeMillis())

    // Emits a value every minute to trigger UI refresh (especially for FAB eligibility)
    private val tickFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(30_000) // 30 seconds for more frequent updates
        }
    }

    val uiState: StateFlow<TableUiState> = combine(
        measurementRepository.getAllMeasurements(),
        settingsRepository.getSettings(),
        _dialogState,
        tickFlow,
        manualRefreshTrigger
    ) { measurements, settings, dialogState, _, _ ->
        val todayStr = LocalDate.now(clock).format(dateFormatter)
        
        // Use default if settings are null
        val activeFlags = settings?.slotActiveFlags ?: listOf(true, false, false, false)
        val allTimesStr = settings?.slotTimes ?: listOf("07:00", "12:00", "18:00", "22:00")
        
        // Filter headers by active status
        val headers = allTimesStr.filterIndexed { index, _ -> activeFlags.getOrElse(index) { false } }
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
                    isToday = date == todayStr
                )
            }
            .sortedByDescending { it.date }

        // FAB Logic
        val now = LocalTime.now(clock)
        val todayMeasurements = measurements.filter { it.date == todayStr }
        
        val eligibleSlots = activeIndices.mapNotNull { originalIndex ->
            val slotTimeStr = allTimesStr.getOrNull(originalIndex) ?: return@mapNotNull null
            val slotTime = LocalTime.parse(slotTimeStr, timeFormatter)
            
            // Using seconds for better precision during the 15-min window check
            val diffSeconds = Duration.between(slotTime, now).getSeconds()
            val diffMinutes = diffSeconds / 60.0
            
            Log.d("MeasurementVM", "Checking slot $originalIndex at $slotTimeStr. Now: $now. Diff mins: $diffMinutes")

            if (abs(diffMinutes) <= 15.0) {
                // Check if slot is empty
                val alreadyExists = todayMeasurements.any { it.slotIndex == originalIndex }
                if (!alreadyExists) {
                    originalIndex to diffMinutes
                } else {
                    Log.d("MeasurementVM", "Slot $originalIndex already filled for today")
                    null
                }
            } else null
        }

        val fabTargetSlotIndex = when {
            eligibleSlots.isEmpty() -> null
            eligibleSlots.size == 1 -> eligibleSlots.first().first
            else -> {
                // If many, take the closest in the past (positive diffMinutes)
                // If multiple in past, take smallest positive diff
                // If none in past, take closest in future (most negative diff)
                val inPast = eligibleSlots.filter { it.second >= 0 }
                if (inPast.isNotEmpty()) {
                    inPast.minByOrNull { it.second }?.first
                } else {
                    eligibleSlots.maxByOrNull { it.second }?.first
                }
            }
        }

        Log.d("MeasurementVM", "FAB Enabled: ${fabTargetSlotIndex != null}, Target: $fabTargetSlotIndex")

        TableUiState(
            isLoading = false,
            slotHeaders = headers,
            items = summarizedItems,
            dialogState = dialogState,
            isFabEnabled = fabTargetSlotIndex != null,
            fabTargetSlotIndex = fabTargetSlotIndex
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TableUiState(isLoading = true)
    )

    /**
     * Forces a refresh of the UI state (e.g., when returning from Settings).
     */
    fun refresh() {
        manualRefreshTrigger.value = System.currentTimeMillis()
    }

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
            
            openDialog(date, originalIndex)
        }
    }

    /**
     * Called when the main action button is clicked.
     */
    fun onFabClicked() {
        val targetIndex = uiState.value.fabTargetSlotIndex ?: return
        val todayStr = LocalDate.now(clock).format(dateFormatter)
        viewModelScope.launch {
            openDialog(todayStr, targetIndex)
        }
    }

    private suspend fun openDialog(date: String, originalSlotIndex: Int) {
        // Find existing measurement if any
        val existing = measurementRepository.getMeasurementsByDateSync(date)
            .find { it.slotIndex == originalSlotIndex }

        val initialValue = existing?.let { "${it.systolic}/${it.diastolic} @${it.pulse}" } ?: ""

        _dialogState.update {
            it.copy(
                isOpen = true,
                date = date,
                slotIndex = originalSlotIndex,
                initialValue = initialValue,
                existingMeasurementId = existing?.id
            )
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
