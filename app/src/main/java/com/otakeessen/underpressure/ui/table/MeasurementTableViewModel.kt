package com.otakeessen.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.validation.BloodPressureValidator
import com.otakeessen.underpressure.domain.validation.ValidationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
import java.time.format.TextStyle
import java.util.Locale
import java.time.Month
import kotlin.math.abs

import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange

import com.otakeessen.underpressure.util.Constants.MIN_SLOT_DIFFERENCE_MINUTES
import com.otakeessen.underpressure.util.Constants.SLOT_WINDOW_MINUTES
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines

/**
 * ViewModel for the Measurement Table Screen.
 * Transforms raw measurements and settings into a summarized table format.
 */
class MeasurementTableViewModel(
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val validator = BloodPressureValidator()

    private val _dialogState = MutableStateFlow(MeasurementDialogState())
    private val _isSummaryVisible = MutableStateFlow(true)
    private val _manualError = MutableStateFlow<String?>(null)
    private val manualRefreshTrigger = MutableStateFlow(System.currentTimeMillis())
    
    private val _expandedYears = MutableStateFlow<Set<Int>>(
        setOf(LocalDate.now(clock).year)
    )
    private val _expandedMonths = MutableStateFlow<Set<String>>(
        setOf(LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM")))
    )

    init {
        // Ensure slot 0 is always active in DB if it's missing or inactive
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            if (!settings.slotActiveFlags.getOrElse(0) { true }) {
                val updated = settings.copy(
                    slotActiveFlags = settings.slotActiveFlags.toMutableList().apply { 
                        if (isEmpty()) add(true) else this[0] = true 
                    }
                )
                settingsRepository.saveSettings(updated)
            }
        }
    }

    private val _scrollToDateEvent = MutableSharedFlow<String>()
    val scrollToDateEvent: SharedFlow<String> = _scrollToDateEvent.asSharedFlow()

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
        _isSummaryVisible,
        _expandedYears,
        _expandedMonths,
        _manualError,
        tickFlow,
        manualRefreshTrigger
    ) { args: Array<Any?> ->
        val measurements = args[0] as List<MeasurementEntity>
        val settings = args[1] as AppSettingsEntity?
        val dialogState = args[2] as MeasurementDialogState
        val isSummaryVisible = args[3] as Boolean
        val expandedYears = args[4] as Set<Int>
        val expandedMonths = args[5] as Set<String>
        val manualError = args[6] as String?
        
        val today = LocalDate.now(clock)
        val todayStr = today.format(dateFormatter)
        val now = LocalTime.now(clock)
        
        val activeFlags = (settings?.slotActiveFlags ?: listOf(true, false, false, false))
            .toMutableList().apply { this[0] = true }
        val allTimesStr = settings?.slotTimes ?: listOf("07:00", "12:00", "18:00", "22:00")
        
        val headers = allTimesStr.filterIndexed { index, _ -> activeFlags.getOrElse(index) { false } }
        val activeIndices = activeFlags.mapIndexedNotNull { index, active -> if (active) index else null }
        
        val summarizedItems = measurements
            .groupBy { it.date }
            .map { (date, dailyMeasurements) ->
                val activeSlots = activeIndices.mapIndexedNotNull { uiIndex, originalIndex ->
                    dailyMeasurements.find { it.slotIndex == originalIndex }?.let { 
                        uiIndex to SlotData(it.systolic, it.diastolic, it.pulse)
                    }
                }.toMap()

                val clickableSlots = mutableSetOf<Int>()
                if (date == todayStr) {
                    activeIndices.forEachIndexed { uiIndex, originalIndex ->
                        val hasData = activeSlots.containsKey(uiIndex)
                        val slotTimeStr = allTimesStr.getOrNull(originalIndex) ?: "00:00"
                        val slotTime = LocalTime.parse(slotTimeStr, timeFormatter)
                        val diffMinutes = Duration.between(slotTime, now).toMinutes()
                        
                        // Clickable if it has data OR is not in the future (within window)
                        if (hasData || diffMinutes >= -SLOT_WINDOW_MINUTES) {
                            clickableSlots.add(uiIndex)
                        }
                    }
                }

                DayMeasurementSummary(
                    date = date,
                    slots = activeSlots,
                    isToday = date == todayStr,
                    clickableSlots = clickableSlots
                )
            }
            .sortedByDescending { it.date }

        val displayItems = mutableListOf<TableItem>()
        val groupedByYear = summarizedItems.groupBy { LocalDate.parse(it.date, dateFormatter).year }
        
        groupedByYear.keys.sortedDescending().forEach { year ->
            val isYearExpanded = expandedYears.contains(year)
            displayItems.add(TableItem.YearHeader(year, isYearExpanded))
            
            if (isYearExpanded) {
                val yearItems = groupedByYear[year] ?: emptyList()
                val groupedByMonth = yearItems.groupBy { 
                    val date = LocalDate.parse(it.date, dateFormatter)
                    date.format(DateTimeFormatter.ofPattern("yyyy-MM"))
                }
                
                groupedByMonth.keys.sortedDescending().forEach { yearMonth ->
                    val isMonthExpanded = expandedMonths.contains(yearMonth)
                    val monthDate = LocalDate.parse("$yearMonth-01", dateFormatter)
                    val monthName = monthDate.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault())
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    
                    val monthMeasurements = groupedByMonth[yearMonth] ?: emptyList()
                    val avgSystolic = monthMeasurements.flatMap { it.slots.values }.map { it.systolic }.average()
                    val avgDiastolic = monthMeasurements.flatMap { it.slots.values }.map { it.diastolic }.average()
                    val summary = if (!avgSystolic.isNaN() && !avgDiastolic.isNaN()) {
                        "${avgSystolic.toInt()}/${avgDiastolic.toInt()}"
                    } else null

                    displayItems.add(TableItem.MonthHeader(yearMonth, monthName, isMonthExpanded, summary))
                    
                    if (isMonthExpanded) {
                        monthMeasurements.forEach { summaryItem ->
                            displayItems.add(TableItem.DayRow(summaryItem))
                        }
                    }
                }
            }
        }

        // FAB & Guidance Logic
        val todayMeasurements = measurements.filter { it.date == todayStr }
        val modifiedFlags = settings?.slotModifiedFlags ?: listOf(false, false, false, false)
        val hasUnmodifiedSlot = modifiedFlags.any { !it }
        
        // 1. Check for slot within +/- 15 minutes
        val slotWindows = activeIndices.map { originalIndex ->
            val slotTime = LocalTime.parse(allTimesStr[originalIndex], timeFormatter)
            val diffMinutes = Duration.between(slotTime, now).toMinutes().toDouble()
            val alreadyExists = todayMeasurements.any { it.slotIndex == originalIndex }
            Triple(originalIndex, diffMinutes, alreadyExists)
        }

        val closestInWindow = slotWindows
            .filter { abs(it.second) <= SLOT_WINDOW_MINUTES }
            .sortedBy { 
                // Prefer past/exact, then closest
                if (it.second >= 0) it.second else 100.0 + abs(it.second) 
            }
            .firstOrNull()

        var fabTargetSlotIndex: Int? = null
        var isGuidanceRequired = false
        var fabHint: String? = null
        var suggestedSlotTime = ""

        if (closestInWindow != null) {
            val (originalIndex, _, alreadyExists) = closestInWindow
            if (!alreadyExists) {
                // Rule 1: Empty slot +/- 15 minutes -> Add
                fabTargetSlotIndex = originalIndex
            } else {
                // Rule 2: Set slot +/- 15 minutes -> Hint
                fabHint = "edit_slot|${originalIndex + 1}"
            }
        } else if (hasUnmodifiedSlot) {
            // Rule 3: No slot +/- 15 mins but unmodified exists -> Guidance
            val firstUnmodified = modifiedFlags.indexOfFirst { !it }
            
            // Calculate suggested time with 30-min buffer from active neighbors
            val activeTimes = activeIndices.map { LocalTime.parse(allTimesStr[it], timeFormatter) }
            val sortedActive = activeTimes.sorted()
            val before = sortedActive.filter { it <= now }.lastOrNull()
            val after = sortedActive.filter { it > now }.firstOrNull()
            
            var suggested = now
            var conflictNeighbor: LocalTime? = null
            
            if (before != null && Duration.between(before, suggested).toMinutes() < MIN_SLOT_DIFFERENCE_MINUTES) {
                suggested = before.plusMinutes(MIN_SLOT_DIFFERENCE_MINUTES.toLong())
            }
            
            if (after != null && Duration.between(suggested, after).toMinutes() < MIN_SLOT_DIFFERENCE_MINUTES) {
                val newSuggested = after.minusMinutes(MIN_SLOT_DIFFERENCE_MINUTES.toLong())
                // Verify it doesn't conflict with 'before' now
                if (before != null && Duration.between(before, newSuggested).toMinutes() < MIN_SLOT_DIFFERENCE_MINUTES) {
                    conflictNeighbor = after
                } else {
                    suggested = newSuggested
                }
            }
            
            if (conflictNeighbor != null) {
                fabHint = "cannot_create|${conflictNeighbor.format(timeFormatter)}"
            } else {
                fabTargetSlotIndex = firstUnmodified
                isGuidanceRequired = true
                suggestedSlotTime = suggested.format(timeFormatter)
            }
        } else {
            // Rule 4: All modified -> Existing hint
            fabHint = "all_modified"
        }

        // Calculate stats
        val guidelines = settings?.bpGuidelines ?: detectDefaultGuidelines()
        val stats = measurements.map { 
            BloodPressureClassifier.classify(it.systolic, it.diastolic, guidelines)
        }.groupingBy { it.level }.eachCount()

        TableUiState(
            isLoading = false,
            slotHeaders = headers,
            items = summarizedItems,
            displayItems = displayItems,
            expandedYears = expandedYears,
            expandedMonths = expandedMonths,
            dialogState = dialogState.copy(suggestedSlotTime = suggestedSlotTime),
            isFabEnabled = fabTargetSlotIndex != null,
            fabTargetSlotIndex = fabTargetSlotIndex,
            isGuidanceRequired = isGuidanceRequired,
            fabHint = fabHint,
            isMasterAlarmEnabled = settings?.masterAlarmEnabled ?: false,
            isSummaryVisible = isSummaryVisible,
            activeGuidelines = guidelines,
            error = manualError,
            classificationStats = stats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TableUiState(isLoading = true)
    )

    /**
     * Toggles visibility of the classification summary.
     */
    fun toggleSummaryVisibility() {
        _isSummaryVisible.update { !it }
    }

    /**
     * Toggles expansion state for a year.
     */
    fun toggleYearExpansion(year: Int) {
        _expandedYears.update { current ->
            if (current.contains(year)) current - year else current + year
        }
    }

    /**
     * Toggles expansion state for a month.
     */
    fun toggleMonthExpansion(yearMonth: String) {
        _expandedMonths.update { current ->
            if (current.contains(yearMonth)) current - yearMonth else current + yearMonth
        }
    }

    /**
     * Toggles the master alarm setting and updates the system alarms.
     */
    fun toggleMasterAlarm() {
        viewModelScope.launch {
            val currentSettings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            val newSettings = currentSettings.copy(masterAlarmEnabled = !currentSettings.masterAlarmEnabled)
            settingsRepository.saveSettings(newSettings)
            alarmScheduler.updateAlarms(newSettings)
        }
    }

    /**
     * Called when a date is selected from the search dialog.
     */
    fun onDateSelectedFromSearch(date: String) {
        viewModelScope.launch {
            _scrollToDateEvent.emit(date)
        }
    }

    /**
     * Forces a refresh of the UI state (e.g., when returning from Settings).
     */
    fun refresh() {
        manualRefreshTrigger.value = System.currentTimeMillis()
    }

    /**
     * Updates the time for a measurement slot.
     * @param uiSlotIndex The index of the slot as displayed in the UI.
     * @param newTimeStr The new time string (HH:mm).
     */
    fun updateSlotTime(uiSlotIndex: Int, newTimeStr: String) {
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: return@launch
            val activeFlags = settings.slotActiveFlags.toMutableList().apply { 
                if (isNotEmpty()) this[0] = true 
            }
            val activeIndices = activeFlags.mapIndexedNotNull { index, active -> if (active) index else null }
            val originalIndex = activeIndices.getOrNull(uiSlotIndex) ?: return@launch

            // Validate time difference from other active slots
            val newTime = LocalTime.parse(newTimeStr, timeFormatter)
            val conflictNeighbor = settings.slotTimes.mapIndexedNotNull { i, t ->
                if (i != originalIndex && activeFlags[i]) LocalTime.parse(t, timeFormatter) else null
            }.find { otherTime ->
                val diff = abs(Duration.between(newTime, otherTime).toMinutes())
                val wrappedDiff = kotlin.math.min(diff, 1440L - diff)
                wrappedDiff < MIN_SLOT_DIFFERENCE_MINUTES
            }

            if (conflictNeighbor != null) {
                _manualError.update { "hint_cannot_create_slot|${conflictNeighbor.format(timeFormatter)}" }
                return@launch
            }

            val newTimes = settings.slotTimes.toMutableList().apply {
                this[originalIndex] = newTimeStr
            }
            val newModifiedFlags = settings.slotModifiedFlags.toMutableList().apply {
                this[originalIndex] = true
            }
            
            val updatedSettings = settings.copy(
                slotTimes = newTimes,
                slotModifiedFlags = newModifiedFlags
            )
            settingsRepository.saveSettings(updatedSettings)
            alarmScheduler.updateAlarms(updatedSettings)
        }
    }

    /**
     * Clears the manual error message.
     */
    fun clearError() {
        _manualError.update { null }
    }

    private fun detectDefaultGuidelines(): BpGuidelines {
        return BpGuidelines.ESC_ESH
    }

    fun onCellClicked(date: String, uiSlotIndex: Int) {
        val todayStr = LocalDate.now(clock).format(dateFormatter)
        if (date != todayStr) return

        viewModelScope.launch {
            // We need to find the original slot index based on settings
            val settings = settingsRepository.getSettingsSync() 
            val activeFlags = (settings?.slotActiveFlags ?: listOf(true, false, false, false))
                .toMutableList().apply { if (isNotEmpty()) this[0] = true }
            val activeIndices = activeFlags.mapIndexedNotNull { index, active -> if (active) index else null }
            val originalIndex = activeIndices.getOrNull(uiSlotIndex) ?: return@launch
            
            // Check if slot is in the future
            val slotTimeStr = settings?.slotTimes?.getOrNull(originalIndex) ?: return@launch
            val slotTime = LocalTime.parse(slotTimeStr, timeFormatter)
            val now = LocalTime.now(clock)
            val diffMinutes = Duration.between(slotTime, now).toMinutes()
            
            val existing = measurementRepository.getMeasurementsByDateSync(date)
                .find { it.slotIndex == originalIndex }
            
            if (existing == null && diffMinutes < -SLOT_WINDOW_MINUTES) {
                // Future empty slot, ignore click
                return@launch
            }
            
            openDialog(date, originalIndex)
        }
    }

    /**
     * Called when the main action button is clicked.
     */
    fun onFabClicked() {
        val state = uiState.value
        val targetIndex = state.fabTargetSlotIndex ?: return
        val todayStr = LocalDate.now(clock).format(dateFormatter)

        if (state.isGuidanceRequired) {
            _dialogState.update { 
                it.copy(
                    isOpen = true, 
                    isGuidanceVisible = true, 
                    slotIndex = targetIndex,
                    date = todayStr,
                    suggestedSlotTime = state.dialogState.suggestedSlotTime
                ) 
            }
        } else {
            viewModelScope.launch {
                openDialog(todayStr, targetIndex)
            }
        }
    }

    /**
     * Called when the user accepts the guidance to reconfigure a slot.
     */
    fun onAcceptGuidance() {
        val currentState = _dialogState.value
        val suggestedTime = currentState.suggestedSlotTime
        
        viewModelScope.launch {
            val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
            val newTimes = settings.slotTimes.toMutableList().apply {
                this[currentState.slotIndex] = suggestedTime
            }
            val newActiveFlags = settings.slotActiveFlags.toMutableList().apply {
                this[currentState.slotIndex] = true
            }
            
            val updatedSettings = settings.copy(
                slotTimes = newTimes,
                slotActiveFlags = newActiveFlags
            )
            settingsRepository.saveSettings(updatedSettings)
            alarmScheduler.updateAlarms(updatedSettings)
            
            // Now open the actual edit dialog
            _dialogState.update { it.copy(isGuidanceVisible = false) }
            openDialog(currentState.date, currentState.slotIndex)
        }
    }

    private suspend fun openDialog(date: String, originalSlotIndex: Int) {
        // Find existing measurement if any
        val existing = measurementRepository.getMeasurementsByDateSync(date)
            .find { it.slotIndex == originalSlotIndex }

        val initialValue = existing?.let { 
            if (it.pulse > 0) "${it.systolic}/${it.diastolic} @${it.pulse}" 
            else "${it.systolic}/${it.diastolic}"
        } ?: ""

        _dialogState.update {
            it.copy(
                isOpen = true,
                date = date,
                slotIndex = originalSlotIndex,
                initialValue = initialValue,
                inputValue = TextFieldValue(initialValue, TextRange(initialValue.length)),
                existingMeasurementId = existing?.id,
                isGuidanceVisible = false
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
     * Called when the measurement input text changes.
     * Applies auto-formatting for delimiters and moves cursor to end.
     */
    fun onMeasurementInputChanged(newInput: TextFieldValue) {
        val oldInput = _dialogState.value.inputValue
        val formatted = formatBloodPressureInput(newInput, oldInput)
        _dialogState.update { it.copy(inputValue = formatted) }
    }

    private fun formatBloodPressureInput(newInput: TextFieldValue, oldInput: TextFieldValue): TextFieldValue {
        val newText = newInput.text
        val oldText = oldInput.text

        if (newText.length < oldText.length) return newInput // Deleting, don't auto-format

        val digits = newText.filter { it.isDigit() }
        val sb = StringBuilder()
        var i = 0

        // Systolic: 3 digits if starts with 1 or 2, else 2 digits
        if (i < digits.length) {
            val start = i
            val len = if (digits[i] == '1' || digits[i] == '2') 3 else 2
            while (i < digits.length && i < start + len) {
                sb.append(digits[i])
                i++
            }
            if (i == start + len) {
                sb.append("/")
            }
        }

        // Diastolic: 3 digits if starts with 1 or 2, else 2 digits
        if (i < digits.length) {
            val start = i
            val len = if (digits[i] == '1' || digits[i] == '2') 3 else 2
            while (i < digits.length && i < start + len) {
                sb.append(digits[i])
                i++
            }
            if (i == start + len) {
                sb.append(" @")
            }
        }

        // Pulse (optional, just the rest of digits)
        while (i < digits.length) {
            sb.append(digits[i])
            i++
        }

        val resultText = sb.toString()
        return if (resultText != newText) {
            // If we added a delimiter, move cursor to the end
            TextFieldValue(resultText, TextRange(resultText.length))
        } else {
            newInput
        }
    }

    /**
     * Saves or updates the measurement.
     */
    fun onSaveMeasurement(input: String) {
        val currentState = _dialogState.value
        // Trim trailing delimiters (e.g., "120/80 @" -> "120/80") for easier saving without pulse
        val trimmedInput = input.trim().removeSuffix("@").removeSuffix("/").trim()
        val validationResult = validator.validate(trimmedInput)

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
                    // Mark slot as modified and active
                    val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
                    val isModified = settings.slotModifiedFlags.getOrElse(currentState.slotIndex) { false }
                    val isActive = settings.slotActiveFlags.getOrElse(currentState.slotIndex) { false }
                    
                    if (!isModified || !isActive) {
                        val newModifiedFlags = settings.slotModifiedFlags.toMutableList().apply {
                            if (size > currentState.slotIndex) this[currentState.slotIndex] = true
                        }
                        val newActiveFlags = settings.slotActiveFlags.toMutableList().apply {
                            if (size > currentState.slotIndex) this[currentState.slotIndex] = true
                        }
                        settingsRepository.saveSettings(settings.copy(
                            slotModifiedFlags = newModifiedFlags,
                            slotActiveFlags = newActiveFlags
                        ))
                    }
                    // Dismiss notification if it was already showing for this slot
                    alarmScheduler.dismissNotification(currentState.slotIndex)
                } else {
                    measurementRepository.updateMeasurement(entity)
                }
                onDialogDismiss()
            }
        }
    }
}
