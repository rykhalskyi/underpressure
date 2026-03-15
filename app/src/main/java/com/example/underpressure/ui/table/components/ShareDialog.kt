package com.example.underpressure.ui.table.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.underpressure.ui.table.ShareViewModel
import androidx.compose.ui.res.stringResource
import com.example.underpressure.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareDialog(
    viewModel: ShareViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (uiState.isOpen) {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = modifier,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.dialog_title_share),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.label_select_date_range),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Date Selection Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DateSelectorButton(
                            label = stringResource(R.string.label_date_from),
                            date = uiState.fromDate,
                            minDate = uiState.minDate,
                            maxDate = uiState.maxDate,
                            onDateSelected = { date ->
                                viewModel.updateDateRange(date, uiState.toDate)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        DateSelectorButton(
                            label = stringResource(R.string.label_date_to),
                            date = uiState.toDate,
                            minDate = uiState.minDate,
                            maxDate = uiState.maxDate,
                            onDateSelected = { date ->
                                viewModel.updateDateRange(uiState.fromDate, date)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (uiState.dateError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.dateError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons
                    Button(
                        onClick = { viewModel.onShareAsMessage() },
                        enabled = !uiState.isProcessing && uiState.dateError == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.button_share_message))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.onExportCsv() },
                        enabled = !uiState.isProcessing && uiState.dateError == null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.button_export_csv))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.button_cancel))
                    }
                }
            }
        }
    }
}

