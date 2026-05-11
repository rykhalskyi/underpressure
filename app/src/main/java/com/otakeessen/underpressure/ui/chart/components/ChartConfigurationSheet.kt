package com.otakeessen.underpressure.ui.chart.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.ui.chart.MeasurementType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartConfigurationSheet(
    selectedSlots: Set<Int>,
    selectedTypes: Set<MeasurementType>,
    fromDate: LocalDate?,
    toDate: LocalDate?,
    onDismiss: () -> Unit,
    onApply: (Set<Int>, Set<MeasurementType>, LocalDate?, LocalDate?) -> Unit,
    sheetState: SheetState
) {
    var tempSlots by remember { mutableStateOf(selectedSlots) }
    var tempTypes by remember { mutableStateOf(selectedTypes) }
    var tempFromDate by remember { mutableStateOf(fromDate) }
    var tempToDate by remember { mutableStateOf(toDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.dialog_title_chart_config),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(R.string.label_select_slots), style = MaterialTheme.typography.titleMedium)
            (0..3).forEach { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = tempSlots.contains(index),
                        onCheckedChange = { checked ->
                            tempSlots = if (checked) tempSlots + index else tempSlots - index
                        }
                    )
                    Text(text = stringResource(R.string.label_slot_number, index + 1))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(R.string.label_select_types), style = MaterialTheme.typography.titleMedium)
            MeasurementType.entries.forEach { type ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = tempTypes.contains(type),
                        onCheckedChange = { checked ->
                            tempTypes = if (checked) tempTypes + type else tempTypes - type
                        }
                    )
                    val typeLabel = when (type) {
                        MeasurementType.SYS -> stringResource(R.string.header_systolic)
                        MeasurementType.DIA -> stringResource(R.string.header_diastolic)
                        MeasurementType.PULSE -> stringResource(R.string.header_pulse)
                    }
                    Text(text = typeLabel)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(R.string.label_select_date_range), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                val dateText = if (tempFromDate != null && tempToDate != null) {
                    "${tempFromDate!!.format(dateFormatter)} - ${tempToDate!!.format(dateFormatter)}"
                } else {
                    stringResource(R.string.label_all_time)
                }
                Text(text = dateText)
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            val errorSelectAtLeastOne = stringResource(R.string.error_no_slots_selected)

            Button(
                onClick = {
                    if (tempSlots.isEmpty() || tempTypes.isEmpty()) {
                        error = errorSelectAtLeastOne
                        return@Button
                    }
                    onApply(tempSlots, tempTypes, tempFromDate, tempToDate)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_apply))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = tempFromDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = tempToDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    val end = dateRangePickerState.selectedEndDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    tempFromDate = start
                    tempToDate = end
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text(modifier = Modifier.padding(16.dp), text = stringResource(R.string.label_select_date_range)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
