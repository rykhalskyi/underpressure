package com.example.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.underpressure.alarm.AlarmScheduler
import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.data.local.entities.MeasurementEntryEntity
import com.example.underpressure.domain.repository.GenericMeasurementRepository
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import com.example.underpressure.domain.validation.BloodPressureValidator
import com.example.underpressure.domain.validation.ValidationResult
import android.util.Log
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
import kotlinx.coroutines.flow.first
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
    private val genericRepository: GenericMeasurementRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val validator = BloodPressureValidator()

    private val _dialogState = MutableStateFlow(MeasurementDialogState())
    private val manualRefreshTrigger = MutableStateFlow(System.currentTimeMillis())
    
    private val _expandedYears = MutableStateFlow<Set<Int>>(
        setOf(LocalDate.now(clock).year)
    )
    private val _expandedMonths = MutableStateFlow<Set<String>>(
        setOf(LocalDate.now(clock).format(DateTimeFormatter.ofPattern("yyyy-MM")))
    )

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
        genericRepository.getAllLists(),
        genericRepository.getAllEntries(),
        _dialogState,
        _expandedYears,
        _expandedMonths,
        tickFlow,
        manualRefreshTrigger
    ) { args: Array<Any?> ->
        val measurements = args[0] as List<MeasurementEntity>
        val settings = args[1] as AppSettingsEntity?
        val genericLists = args[2] as List<com.example.underpressure.data.local.entities.MeasurementListEntity>
        val genericEntries = args[3] as List<MeasurementEntryEntity>
        val dialogState = args[4] as MeasurementDialogState
        val expandedYears = args[5] as Set<Int>
        val expandedMonths = args[6] as Set<String>
        
        val today = LocalDate.now(clock)
        val todayStr = today.format(dateFormatter)
        
        val activeFlags = settings?.slotActiveFlags ?: listOf(true, false, false, false)
        val allTimesStr = settings?.slotTimes ?: listOf("07:00", "12:00", "18:00", "22:00")
        val headers = allTimesStr.filterIndexed { index, _ -> activeFlags.getOrElse(index) { false } }
        val activeIndices = activeFlags.mapIndexedNotNull { index, active -> if (active) index else null }
        
        // Group generic entries by date and slot
        val genericByDate = genericEntries.groupBy { it.date }

        val summarizedItems = measurements
            .groupBy { it.date }
            .map { (date, dailyMeasurements) ->
                val activeSlots = activeIndices.mapIndexedNotNull { uiIndex, originalIndex ->
                    dailyMeasurements.find { it.slotIndex == originalIndex }?.let { 
                        uiIndex to SlotData(it.systolic, it.diastolic, it.pulse)
                    }
                }.toMap()

                val dailyGenericEntries = genericByDate[date]?.groupBy { it.slotIndex } ?: emptyMap()
                val activeGenericSlots = activeIndices.mapIndexedNotNull { uiIndex, originalIndex ->
                    val entriesForSlot = dailyGenericEntries[originalIndex] ?: emptyList()
                    if (entriesForSlot.isNotEmpty()) {
                        uiIndex to entriesForSlot.mapNotNull { entry ->
                            val list = genericLists.find { it.id == entry.listId } ?: return@mapNotNull null
                            GenericSlotData(
                                listId = entry.listId,
                                listName = list.name,
                                value = entry.value,
                                type = list.type.name
                            )
                        }
                    } else null
                }.toMap()

                DayMeasurementSummary(
                    date = date,
                    slots = activeSlots,
                    genericEntries = activeGenericSlots,
                    isToday = date == todayStr
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
                    val monthName = monthDate.month.name.lowercase().replaceFirstChar { it.uppercase() }
                    
                    // Optional: calculate summary for month
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
            displayItems = displayItems,
            expandedYears = expandedYears,
            expandedMonths = expandedMonths,
            dialogState = dialogState,
            isFabEnabled = fabTargetSlotIndex != null,
            fabTargetSlotIndex = fabTargetSlotIndex,
            isMasterAlarmEnabled = settings?.masterAlarmEnabled ?: false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TableUiState(isLoading = true)
    )

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
     * Called when a table cell is clicked.
     */
    fun onCellClicked(date: String, uiSlotIndex: Int) {
        val todayStr = LocalDate.now(clock).format(dateFormatter)
        if (date != todayStr) return

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

        // Fetch active generic lists and their entries
        val activeLists = genericRepository.getActiveLists().first()
        val existingEntries = genericRepository.getEntriesForSlot(date, originalSlotIndex).first()

        val genericInputs = activeLists.map { list ->
            val existingEntry = existingEntries.find { it.listId == list.id }
            GenericInputState(
                listId = list.id,
                name = list.name,
                type = list.type.name,
                value = existingEntry?.value ?: ""
            )
        }

        _dialogState.update {
            it.copy(
                isOpen = true,
                date = date,
                slotIndex = originalSlotIndex,
                initialValue = initialValue,
                genericInputs = genericInputs,
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
     * Called when a generic input value is changed in the dialog.
     */
    fun onGenericInputChanged(listId: Long, newValue: String) {
        _dialogState.update { state ->
            state.copy(
                genericInputs = state.genericInputs.map {
                    if (it.listId == listId) it.copy(value = newValue) else it
                }
            )
        }
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
                    // Dismiss notification if it was already showing for this slot
                    alarmScheduler.dismissNotification(currentState.slotIndex)
                } else {
                    measurementRepository.updateMeasurement(entity)
                }

                // Save generic entries
                currentState.genericInputs.forEach { inputState ->
                    if (inputState.value.isNotBlank()) {
                        genericRepository.saveEntry(
                            MeasurementEntryEntity(
                                date = currentState.date,
                                slotIndex = currentState.slotIndex,
                                listId = inputState.listId,
                                value = inputState.value
                            )
                        )
                    }
                }

                onDialogDismiss()
            }
        }
    }
}
