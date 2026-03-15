package com.example.underpressure.ui.table

import java.time.LocalDate

/**
 * UI State for the Share Dialog.
 *
 * @property isOpen Whether the dialog is currently visible.
 * @property fromDate The start date of the export range.
 * @property toDate The end date of the export range.
 * @property dateError Validation error message for the date range.
 * @property isProcessing Whether an export operation is currently in progress.
 */
data class ShareUiState(
    val isOpen: Boolean = false,
    val fromDate: LocalDate? = null,
    val toDate: LocalDate? = null,
    val dateError: String? = null,
    val isProcessing: Boolean = false
)
