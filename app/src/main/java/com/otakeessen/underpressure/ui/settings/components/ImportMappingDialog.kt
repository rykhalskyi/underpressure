package com.otakeessen.underpressure.ui.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
        title = { Text("Import Trackers") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "We found custom trackers in your CSV. Choose how to map them:",
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
                        "Overwrite existing data",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Text(
                    "If unchecked, existing readings for the same date/time will be skipped.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(overwrite, mappingState.toMap()) }) {
                Text("Start Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TrackerMappingItem(
    tracker: DiscoveredTracker,
    existingTrackers: List<TrackerDefinition>,
    currentAction: TrackerMappingAction,
    onActionChange: (TrackerMappingAction) -> Unit
) {
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

            Column {
                MappingRadioButton(
                    selected = currentAction is TrackerMappingAction.MapToExisting,
                    onClick = {
                        val targetId = tracker.existingDefinition?.id ?: existingTrackers.firstOrNull()?.id ?: 0L
                        onActionChange(TrackerMappingAction.MapToExisting(targetId))
                    },
                    label = "Map to Existing"
                )
                
                if (currentAction is TrackerMappingAction.MapToExisting) {
                    TrackerSelector(
                        selectedId = currentAction.trackerId,
                        allTrackers = existingTrackers,
                        onSelect = { onActionChange(TrackerMappingAction.MapToExisting(it)) }
                    )
                }

                MappingRadioButton(
                    selected = currentAction is TrackerMappingAction.CreateNew,
                    onClick = { 
                        onActionChange(TrackerMappingAction.CreateNew(tracker.extractedName, TrackerType.FLOAT, tracker.unit)) 
                    },
                    label = "Create New Tracker"
                )
                
                if (currentAction is TrackerMappingAction.CreateNew) {
                    TypeSelector(
                        selectedType = currentAction.type,
                        onSelect = { onActionChange(currentAction.copy(type = it)) }
                    )
                }

                MappingRadioButton(
                    selected = currentAction is TrackerMappingAction.Skip,
                    onClick = { onActionChange(TrackerMappingAction.Skip) },
                    label = "Skip this column"
                )
            }
        }
    }
}

@Composable
fun MappingRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerSelector(
    selectedId: Long,
    allTrackers: List<TrackerDefinition>,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTracker = allTrackers.find { it.id == selectedId }

    Box(modifier = Modifier.padding(start = 32.dp, bottom = 8.dp)) {
        OutlinedCard(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = selectedTracker?.let { "${it.name}${if (!it.unit.isNullOrBlank()) " (${it.unit})" else ""}" } ?: "Select tracker",
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodySmall
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            allTrackers.forEach { tracker ->
                DropdownMenuItem(
                    text = { Text("${tracker.name} (${tracker.unit ?: "no unit"})", style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        onSelect(tracker.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSelector(
    selectedType: TrackerType,
    onSelect: (TrackerType) -> Unit
) {
    Row(
        modifier = Modifier.padding(start = 32.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrackerType.values().forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onSelect(type) },
                label = { Text(type.name, style = MaterialTheme.typography.labelSmall) }
            )
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
