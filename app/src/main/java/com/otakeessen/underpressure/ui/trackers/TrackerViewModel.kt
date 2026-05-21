package com.otakeessen.underpressure.ui.trackers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing health trackers.
 */
class TrackerViewModel(
    private val trackerRepository: TrackerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackerUiState(isLoading = true))
    val uiState: StateFlow<TrackerUiState> = _uiState.asStateFlow()

    init {
        loadTrackers()
    }

    private fun loadTrackers() {
        trackerRepository.getAllTrackerDefinitions()
            .onEach { trackers ->
                _uiState.update { it.copy(trackers = trackers, isLoading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun toggleTrackerActive(tracker: TrackerDefinition) {
        viewModelScope.launch {
            trackerRepository.saveTrackerDefinition(tracker.copy(isActive = !tracker.isActive))
        }
    }

    fun toggleTrackerOnChart(tracker: TrackerDefinition) {
        viewModelScope.launch {
            trackerRepository.saveTrackerDefinition(tracker.copy(showOnChart = !tracker.showOnChart))
        }
    }

    fun saveTracker(tracker: TrackerDefinition) {
        viewModelScope.launch {
            trackerRepository.saveTrackerDefinition(tracker)
        }
    }

    fun deleteTracker(tracker: TrackerDefinition) {
        viewModelScope.launch {
            trackerRepository.deleteTrackerDefinition(tracker)
        }
    }
}
