package com.otakeessen.underpressure.ui.chart.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartConfigurationSheet(
    selectedSlots: Set<Int>,
    selectedTypes: Set<MeasurementType>,
    onDismiss: () -> Unit,
    onToggleSlot: (Int) -> Unit,
    onToggleType: (MeasurementType) -> Unit,
    sheetState: SheetState
) {
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
                        checked = selectedSlots.contains(index),
                        onCheckedChange = { onToggleSlot(index) }
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
                        checked = selectedTypes.contains(type),
                        onCheckedChange = { onToggleType(type) }
                    )
                    val typeLabel = when (type) {
                        MeasurementType.SYS -> stringResource(R.string.header_systolic)
                        MeasurementType.DIA -> stringResource(R.string.header_diastolic)
                        MeasurementType.PULSE -> stringResource(R.string.header_pulse)
                    }
                    Text(text = typeLabel)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.button_close))
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
