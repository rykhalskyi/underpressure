package com.otakeessen.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.otakeessen.underpressure.R

/**
 * ViewModel for the Search Dialog.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val measurementRepository: MeasurementRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _isLoading = MutableStateFlow(false)

    val resultsState: StateFlow<SearchUiState> = _query
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchUiState())
            } else {
                _isLoading.value = true

                // Separate Date vs Numeric logic
                // If it's a 4-digit number, treat it as a year (date search). 
                // Otherwise check for hyphen or mixed digits/other chars to route correctly.
                val isYearOnly = query.matches(Regex("""^\d{4}$"""))
                val containsHyphen = query.contains("-")
                
                if (isYearOnly || containsHyphen) {
                    performDateSearch(query)
                } else {
                    performNumericSearch(query)
                }
            }
        }
        .combine(_isLoading) { state, loading ->
            state.copy(isLoading = loading)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SearchUiState()
        )

    private fun performDateSearch(query: String) =
        measurementRepository.searchMeasurementsByDate(query)
            .flatMapLatest { results ->
                _isLoading.value = false
                flowOf(SearchUiState(query = query, results = results, isNoResults = results.isEmpty()))
            }

    private fun performNumericSearch(query: String) = run {
        // Parse numeric patterns: SYS, SYS/DIA, SYS/DIA/PULSE, SYS/DIA@PULSE
        // Use a regex to extract numeric parts, ignoring non-digit separators
        val digits = query.split(Regex("[^0-9]+")).filter { it.isNotEmpty() }
        
        if (digits.isEmpty()) {
            _isLoading.value = false
            flowOf(SearchUiState(query = query, results = emptyList(), isNoResults = true))
        } else {
            measurementRepository.searchMeasurementsComplex(digits)
                .flatMapLatest { results ->
                    _isLoading.value = false
                    flowOf(SearchUiState(query = query, results = results, isNoResults = results.isEmpty()))
                }
        }
    }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }
    }
