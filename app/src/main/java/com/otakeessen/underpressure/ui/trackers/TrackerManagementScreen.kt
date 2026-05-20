package com.otakeessen.underpressure.ui.trackers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerManagementScreen(
    viewModel: TrackerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var trackerToDelete by remember { mutableStateOf<TrackerDefinition?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Trackers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Tracker")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.trackers.isEmpty() && !uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No trackers defined", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            // Add some default trackers
                            viewModel.saveTracker(TrackerDefinition(name = "Weight", type = TrackerType.FLOAT, unit = "kg", useSecondaryAxis = true))
                            viewModel.saveTracker(TrackerDefinition(name = "Temperature", type = TrackerType.FLOAT, unit = "°C"))
                            viewModel.saveTracker(TrackerDefinition(name = "Took Medication", type = TrackerType.BOOLEAN))
                            viewModel.saveTracker(TrackerDefinition(name = "Symptoms", type = TrackerType.STRING))
                        }) {
                            Text("Add Default Trackers")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.trackers) { tracker ->
                        TrackerItem(
                            tracker = tracker,
                            onToggleActive = { viewModel.toggleTrackerActive(tracker) },
                            onDelete = { trackerToDelete = tracker }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddTrackerDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type, unit ->
                    viewModel.saveTracker(TrackerDefinition(name = name, type = type, unit = unit))
                    showAddDialog = false
                }
            )
        }

        trackerToDelete?.let { tracker ->
            AlertDialog(
                onDismissRequest = { trackerToDelete = null },
                title = { Text(stringResource(R.string.dialog_title_delete_tracker)) },
                text = {
                    Text(stringResource(R.string.dialog_message_delete_tracker, tracker.name))
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTracker(tracker)
                            trackerToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.button_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { trackerToDelete = null }) {
                        Text(stringResource(R.string.button_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun TrackerItem(
    tracker: TrackerDefinition,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val typeText = when (tracker.type) {
                TrackerType.FLOAT -> "Number"
                TrackerType.BOOLEAN -> "yes/no"
                TrackerType.STRING -> "Text"
            }
            val details = if (tracker.unit != null) "$typeText, ${tracker.unit}" else typeText

            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(tracker.name)
                    }
                    append(" ($details)")
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.label_active),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Switch(
                        checked = tracker.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier.scale(0.8f)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTrackerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, TrackerType, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TrackerType.FLOAT) }
    var unit by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Tracker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Weight)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (type) {
                            TrackerType.FLOAT -> "Number"
                            TrackerType.BOOLEAN -> "yes/no"
                            TrackerType.STRING -> "Text"
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TrackerType.values().forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (t) {
                                            TrackerType.FLOAT -> "Number"
                                            TrackerType.BOOLEAN -> "yes/no"
                                            TrackerType.STRING -> "Text"
                                        }
                                    )
                                },
                                onClick = {
                                    type = t
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                if (type == TrackerType.FLOAT) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (e.g. kg)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, type, unit.ifBlank { null }) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
