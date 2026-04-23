package com.otakeessen.underpressure.ui.chart.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
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
import java.time.LocalDate
import java.time.format.DateTimeParseException

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
    var fromDateStr by remember { mutableStateOf(fromDate?.toString() ?: "") }
    var toDateStr by remember { mutableStateOf(toDate?.toString() ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

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
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = fromDateStr,
                    onValueChange = { fromDateStr = it },
                    label = { Text(stringResource(R.string.label_date_from)) },
                    placeholder = { Text(stringResource(R.string.placeholder_date)) },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = toDateStr,
                    onValueChange = { toDateStr = it },
                    label = { Text(stringResource(R.string.label_date_to)) },
                    placeholder = { Text(stringResource(R.string.placeholder_date)) },
                    modifier = Modifier.weight(1f)
                )
            }

            error?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.height(24.dp))

            val errorSelectAtLeastOne = stringResource(R.string.error_no_slots_selected)
            val errorInvalidDateFormat = stringResource(R.string.error_invalid_date_format)
            val errorFromAfterTo = stringResource(R.string.error_from_after_to)

            Button(
                onClick = {
                    if (tempSlots.isEmpty() || tempTypes.isEmpty()) {
                        error = errorSelectAtLeastOne
                        return@Button
                    }
                    
                    val parsedFrom = try {
                        if (fromDateStr.isNotBlank()) LocalDate.parse(fromDateStr) else null
                    } catch (e: DateTimeParseException) {
                        error = errorInvalidDateFormat
                        return@Button
                    }

                    val parsedTo = try {
                        if (toDateStr.isNotBlank()) LocalDate.parse(toDateStr) else null
                    } catch (e: DateTimeParseException) {
                        error = errorInvalidDateFormat
                        return@Button
                    }

                    if (parsedFrom != null && parsedTo != null && parsedFrom.isAfter(parsedTo)) {
                        error = errorFromAfterTo
                        return@Button
                    }

                    onApply(tempSlots, tempTypes, parsedFrom, parsedTo)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_apply))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

