package com.example.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.underpressure.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * ViewModel for the Measurement Table Screen.
 * Responsible for transforming measurement data into a summarized daily format.
 */
class MeasurementTableViewModel(
    private val repository: MeasurementRepository
) : ViewModel() {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Exposes the table UI state to the Compose layer.
     * Groups measurements by date and takes the latest entry for each day.
     */
    val uiState: StateFlow<TableUiState> = repository.getAllMeasurements()
        .map { measurements ->
            val today = LocalDate.now().format(dateFormatter)
            
            // Group by date and pick the latest measurement (highest ID or timestamp)
            val summarizedItems = measurements
                .groupBy { it.date }
                .map { (date, dailyMeasurements) ->
                    // Assuming higher ID or later updatedAt means "latest"
                    val latest = dailyMeasurements.maxByOrNull { it.updatedAt }
                    DayMeasurementSummary(
                        date = date,
                        systolic = latest?.systolic,
                        diastolic = latest?.diastolic,
                        pulse = latest?.pulse,
                        isToday = date == today
                    )
                }
                .sortedByDescending { it.date }

            TableUiState(
                isLoading = false,
                items = summarizedItems
            )
        }
        .onStart { 
            // Optional: You could emit a loading state here if the repository is slow
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TableUiState(isLoading = true)
        )
}
