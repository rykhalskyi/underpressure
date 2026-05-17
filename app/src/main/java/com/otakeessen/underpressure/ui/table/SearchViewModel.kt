package com.otakeessen.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.otakeessen.underpressure.R

enum class SearchFilter {
    NONE, HYPOTENSION, NORMAL, ELEVATED, STAGE_1, STAGE_2
}

/**
 * ViewModel for the Search Dialog.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _filter = MutableStateFlow(SearchFilter.NONE)
    val filter = _filter.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    val resultsState: StateFlow<SearchUiState> = combine(_query, _filter, settingsRepository.getSettings()) { query, filter, settings ->
        val guidelines = settings?.bpGuidelines ?: BpGuidelines.ESC_ESH
        Triple(query, filter, guidelines)
    }
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { (query, filter, guidelines) ->
            if (query.isBlank() && filter == SearchFilter.NONE) {
                _isLoading.value = false
                flowOf(SearchUiState())
            } else {
                _isLoading.value = true

                val flow = if (query.isBlank()) {
                    // Filter all measurements by level
                    measurementRepository.getAllMeasurements()
                        .flatMapLatest { results ->
                            _isLoading.value = false
                            flowOf(SearchUiState(query = query, results = results))
                        }
                } else {
                    // Separate Date vs Numeric logic
                    val isYearOnly = query.trim().matches(Regex("""^\d{4}$"""))
                    val containsHyphen = query.contains("-")

                    if (isYearOnly || containsHyphen) {
                        performDateSearch(query.trim())
                    } else {
                        performNumericSearch(query.trim())
                    }
                }

                flow.flatMapLatest { state ->
                    var results = state.results
                    if (filter != SearchFilter.NONE) {
                        val targetLevel = filter.toBloodPressureLevel()
                        results = results.filter { 
                            BloodPressureClassifier.classify(it.systolic, it.diastolic, guidelines).level == targetLevel
                        }
                    }
                    flowOf(state.copy(results = results, isNoResults = results.isEmpty(), isLoading = false))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState()
        )

    fun setFilter(filter: SearchFilter) {
        _filter.value = if (_filter.value == filter) SearchFilter.NONE else filter
    }

    private fun SearchFilter.toBloodPressureLevel(): BloodPressureLevel? = when (this) {
        SearchFilter.HYPOTENSION -> BloodPressureLevel.HYPOTENSION
        SearchFilter.NORMAL -> BloodPressureLevel.NORMAL
        SearchFilter.ELEVATED -> BloodPressureLevel.ELEVATED
        SearchFilter.STAGE_1 -> BloodPressureLevel.STAGE_1
        SearchFilter.STAGE_2 -> BloodPressureLevel.STAGE_2
        SearchFilter.NONE -> null
    }

    private fun performDateSearch(query: String) = run {
        val isValid = isValidDatePart(query)
        
        if (!isValid && query.contains("-")) {
            _isLoading.value = false
            flowOf(SearchUiState(query = query, dateErrorRes = R.string.error_invalid_date_format))
        } else {
            measurementRepository.searchMeasurementsByDate(query)
                .flatMapLatest { results ->
                    _isLoading.value = false
                    flowOf(SearchUiState(query = query, results = results, isNoResults = results.isEmpty()))
                }
        }
    }

    private fun isValidDatePart(query: String): Boolean {
        return try {
            when {
                query.matches(Regex("""^\d{4}$""")) -> true
                query.matches(Regex("""^\d{4}-\d{2}$""")) -> {
                    val month = query.substring(5).toInt()
                    month in 1..12
                }
                query.matches(Regex("""^\d{4}-\d{2}-\d{2}$""")) -> {
                    LocalDate.parse(query, dateFormatter)
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun performNumericSearch(query: String) = run {
        // Parse numeric patterns: SYS, SYS/DIA, SYS/DIA/PULSE, SYS/DIA@PULSE
        // Use a regex to extract numeric parts, ignoring non-digit separators
        val digits = query.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }
        
        when {
            digits.isEmpty() -> {
                _isLoading.value = false
                flowOf(SearchUiState(query = query, results = emptyList(), isNoResults = true))
            }
            digits.size == 1 -> {
                // If only one digit, search across all numeric fields (SYS OR DIA OR PULSE)
                measurementRepository.searchMeasurements(digits[0])
                    .flatMapLatest { results ->
                        _isLoading.value = false
                        flowOf(SearchUiState(query = query, results = results, isNoResults = results.isEmpty()))
                    }
            }
            else -> {
                // If multiple digits, search specifically (SYS AND DIA [AND PULSE])
                measurementRepository.searchMeasurementsComplex(digits)
                    .flatMapLatest { results ->
                        _isLoading.value = false
                        flowOf(SearchUiState(query = query, results = results, isNoResults = results.isEmpty()))
                    }
            }
        }
    }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
        // If it's a numeric search (not date), clear filters
        val isYearOnly = newQuery.trim().matches(Regex("""^\d{4}$"""))
        val containsHyphen = newQuery.contains("-")
        if (newQuery.isNotBlank() && !isYearOnly && !containsHyphen) {
            _filter.value = SearchFilter.NONE
        }
    }
}
