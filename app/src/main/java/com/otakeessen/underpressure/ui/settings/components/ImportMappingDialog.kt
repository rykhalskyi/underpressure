package com.otakeessen.underpressure.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.export.DiscoveredTracker
import com.otakeessen.underpressure.domain.export.TrackerDiscoveryResult
import com.otakeessen.underpressure.domain.export.TrackerMappingAction
import com.otakeessen.underpressure.domain.export.TrackerMatchStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportMappingDialog(
    discoveryResult: TrackerDiscoveryResult,
    existingTrackers: List<TrackerDefinition>,
    onConfirm: (overwrite: Boolean, mapping: Map<String, TrackerMappingAction>) -> Unit,
    onDismiss: () -> Unit
) {
    var overwrite by remember { mutableStateOf(false) }
    val mappingState = remember {
        mutableStateMapOf<String, TrackerMappingAction>().apply {
            discoveryResult.discoveredTrackers.forEach { tracker ->
                val defaultAction = when (tracker.matchStatus) {
                    TrackerMatchStatus.EXACT_MATCH -> TrackerMappingAction.MapToExisting(tracker.existingDefinition!!.id)
                    TrackerMatchStatus.CONFLICT -> TrackerMappingAction.MapToExisting(tracker.existingDefinition!!.id)
                    TrackerMatchStatus.NEW -> TrackerMappingAction.CreateNew(tracker.extractedName, TrackerType.FLOAT, tracker.unit)
                }
                put(tracker.headerName, defaultAction)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.import_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.import_description),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(bottom = 16.dp)
                ) {
                    items(discoveryResult.discoveredTrackers) { tracker ->
                        TrackerMappingItem(
                            tracker = tracker,
                            existingTrackers = existingTrackers,
                            currentAction = mappingState[tracker.headerName]!!,
                            onActionChange = { mappingState[tracker.headerName] = it }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = overwrite,
                        onCheckedChange = { overwrite = it }
                    )
                    Text(
                        stringResource(R.string.overwrite_existing_data),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    stringResource(R.string.overwrite_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(overwrite, mappingState.toMap()) }) {
                Text(stringResource(R.string.button_start_import))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerMappingItem(
    tracker: DiscoveredTracker,
    existingTrackers: List<TrackerDefinition>,
    currentAction: TrackerMappingAction,
    onActionChange: (TrackerMappingAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tracker.headerName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                MatchStatusBadge(tracker.matchStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Compact Action Selector
            Box {
                OutlinedCard(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val label = when (currentAction) {
                        is TrackerMappingAction.MapToExisting -> existingTrackers.find { it.id == currentAction.trackerId }?.name ?: stringResource(R.string.mapping_map_to_existing)
                        is TrackerMappingAction.CreateNew -> {
                            val typeLabel = tracker.type?.let { " (${it})" } ?: ""
                            "${stringResource(R.string.mapping_create_new)}$typeLabel"
                        }
                        is TrackerMappingAction.Skip -> stringResource(R.string.mapping_skip)
                    }
                    Text(
                        text = label,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mapping_skip)) },
                        onClick = { onActionChange(TrackerMappingAction.Skip); expanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.mapping_create_new)) },
                        onClick = { 
                            onActionChange(TrackerMappingAction.CreateNew(
                                tracker.extractedName, 
                                tracker.type ?: TrackerType.FLOAT, 
                                tracker.unit
                            )); 
                            expanded = false 
                        }
                    )
                    existingTrackers.forEach { trackerDef ->
                        DropdownMenuItem(
                            text = { Text("${trackerDef.name} (${trackerDef.unit ?: "no unit"})") },
                            onClick = { onActionChange(TrackerMappingAction.MapToExisting(trackerDef.id)); expanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchStatusBadge(status: TrackerMatchStatus) {
    val color = when (status) {
        TrackerMatchStatus.EXACT_MATCH -> Color(0xFF4CAF50)
        TrackerMatchStatus.CONFLICT -> Color(0xFFFF9800)
        TrackerMatchStatus.NEW -> Color(0xFF2196F3)
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Text(
            text = status.name.replace("_", " "),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
