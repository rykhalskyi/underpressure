package com.otakeessen.underpressure.ui.table

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.otakeessen.underpressure.data.export.TableExportManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/**
 * ViewModel for the Share Dialog.
 * Manages date range selection, validation, and triggers export operations.
 */
class ShareViewModel(
    private val exportManager: TableExportManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ShareUiState())
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    // Events to trigger UI actions (like opening share sheet)
    sealed class ShareEvent {
        data class ShareText(val text: String) : ShareEvent()
        data class ShareFile(val file: File) : ShareEvent()
        data class Error(val message: String) : ShareEvent()
    }

    private val _shareEvents = MutableSharedFlow<ShareEvent>()
    val shareEvents = _shareEvents.asSharedFlow()

    fun onOpenDialog() {
        viewModelScope.launch {
            val (minDate, maxDate) = exportManager.getDateRange()
            _uiState.update { 
                it.copy(
                    isOpen = true,
                    minDate = minDate,
                    maxDate = maxDate
                ) 
            }
        }
    }

    fun onDismissDialog() {
        _uiState.update { ShareUiState() } // Reset state on dismiss
    }

    fun updateDateRange(from: LocalDate?, to: LocalDate?) {
        val error = if (from != null && to != null && from.isAfter(to)) {
            "Start date cannot be after end date"
        } else {
            null
        }
        
        _uiState.update { 
            it.copy(
                fromDate = from, 
                toDate = to, 
                dateError = error
            ) 
        }
    }

    fun onShareAsMessage() {
        val state = _uiState.value
        if (state.dateError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val asciiTable = exportManager.generateAsciiTable(state.fromDate, state.toDate)
                if (asciiTable.isBlank()) {
                     _shareEvents.emit(ShareEvent.Error("No data found for selected range"))
                } else {
                    _shareEvents.emit(ShareEvent.ShareText(asciiTable))
                }
            } catch (e: Exception) {
                _shareEvents.emit(ShareEvent.Error("Failed to generate message: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun onExportCsv() {
        val state = _uiState.value
        if (state.dateError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val csvContent = exportManager.generateCsvContent(state.fromDate, state.toDate)
                if (csvContent.isBlank()) {
                     _shareEvents.emit(ShareEvent.Error("No data found for selected range"))
                } else {
                    val file = exportManager.saveCsvToCache(csvContent)
                    _shareEvents.emit(ShareEvent.ShareFile(file))
                }
            } catch (e: Exception) {
                _shareEvents.emit(ShareEvent.Error("Failed to export CSV: ${e.message}"))
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }
}

