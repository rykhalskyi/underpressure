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

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(300L)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(SearchUiState())
            } else {
                _isLoading.value = true
                
                // Determine if query is potentially a date or a numeric search
                val isDateQuery = query.any { it == '-' }
                
                if (isDateQuery) {
                    validateAndSearchByDate(query)
                } else {
                    searchByNumericValue(query)
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

    private fun validateAndSearchByDate(query: String) = flowOf(
        try {
            LocalDate.parse(query, dateFormatter)
            _isLoading.value = false
            SearchUiState(query = query) // Valid date format, result will be handled by navigation
        } catch (e: DateTimeParseException) {
            _isLoading.value = false
            SearchUiState(query = query, dateErrorRes = R.string.error_invalid_date)
        }
    )

    private fun searchByNumericValue(query: String) = 
        measurementRepository.searchMeasurements(query)
            .flatMapLatest { results ->
                _isLoading.value = false
                flowOf(SearchUiState(
                    query = query,
                    results = results,
                    isNoResults = results.isEmpty()
                ))
            }

    fun updateQuery(newQuery: String) {
        _query.value = newQuery
    }
}

