package com.example.underpressure.ui.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.underpressure.R
import com.example.underpressure.ui.settings.SlotConfig

/**
 * A row representing a single measurement slot configuration.
 */
@Composable
fun SlotRow(
    slot: SlotConfig,
    onTimeClick: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onAlarmChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Slot ${slot.number}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = slot.time,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onTimeClick() }
                    .padding(vertical = 4.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Alarm toggle - only enabled if slot is active
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.label_alarm),
                    style = MaterialTheme.typography.labelSmall
                )
                Switch(
                    checked = slot.isAlarmEnabled,
                    onCheckedChange = onAlarmChange,
                    enabled = slot.isActive,
                    scale = 0.8f // Slightly smaller
                )
            }

            // Active toggle
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.label_active),
                    style = MaterialTheme.typography.labelSmall
                )
                if (slot.isToggleable) {
                    Switch(
                        checked = slot.isActive,
                        onCheckedChange = onActiveChange,
                        scale = 0.8f
                    )
                } else {
                    // Always active for slot 1
                    Switch(
                        checked = true,
                        onCheckedChange = {},
                        enabled = false,
                        scale = 0.8f
                    )
                }
            }
        }
    }
}

/**
 * A row for the master alarm toggle.
 */
@Composable
fun GlobalAlarmRow(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Global Alarm Reminders",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

// Extension to scale Switch
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    scale: Float = 1f
) {
    androidx.compose.material3.Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.scale(scale),
        enabled = enabled
    )
}

/**
 * A dialog for picking a time for a measurement slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val initialHour = initialTime.substringBefore(":").toIntOrNull() ?: 7
    val initialMinute = initialTime.substringAfter(":").toIntOrNull() ?: 0
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
                )
                
                TimePicker(state = timePickerState)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = {
                            val formattedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                            onConfirm(formattedTime)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}
