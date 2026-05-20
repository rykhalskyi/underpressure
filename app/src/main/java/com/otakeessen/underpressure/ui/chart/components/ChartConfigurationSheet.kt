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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.ui.chart.MeasurementType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartConfigurationSheet(
    selectedSlots: Set<Int>,
    selectedTypes: Set<MeasurementType>,
    activeTrackers: List<TrackerDefinition>,
    showRiskZones: Boolean,
    showRollingAverage: Boolean,
    showInteractiveLegend: Boolean,
    onDismiss: () -> Unit,
    onToggleSlot: (Int) -> Unit,
    onToggleType: (MeasurementType) -> Unit,
    onToggleTracker: (Long) -> Unit,
    onToggleRiskZones: () -> Unit,
    onToggleRollingAverage: () -> Unit,
    onToggleInteractiveLegend: () -> Unit,
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = selectedSlots.contains(-1),
                    onCheckedChange = { onToggleSlot(-1) }
                )
                Text(text = stringResource(R.string.label_anytime_readings))
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

            if (activeTrackers.any { it.type == TrackerType.FLOAT }) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.label_select_trackers), style = MaterialTheme.typography.titleMedium)
                activeTrackers.filter { it.type == TrackerType.FLOAT }.forEach { tracker ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = tracker.showOnChart,
                            onCheckedChange = { onToggleTracker(tracker.id) }
                        )
                        Text(text = tracker.name)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(text = stringResource(R.string.label_visual_features), style = MaterialTheme.typography.titleMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = showRiskZones,
                    onCheckedChange = { onToggleRiskZones() }
                )
                Text(text = stringResource(R.string.label_show_risk_zones))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = showRollingAverage,
                    onCheckedChange = { onToggleRollingAverage() }
                )
                Text(text = stringResource(R.string.label_show_rolling_average))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = showInteractiveLegend,
                    onCheckedChange = { onToggleInteractiveLegend() }
                )
                Text(text = stringResource(R.string.label_show_slot_chips))
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
