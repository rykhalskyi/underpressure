package com.otakeessen.underpressure.ui.trackers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    var trackerToEdit by remember { mutableStateOf<TrackerDefinition?>(null) }
    var trackerToDelete by remember { mutableStateOf<TrackerDefinition?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.custom_trackers)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(
                            R.string.back
                        ))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_tracker))
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
                        Text(stringResource(R.string.no_trackers_defined), style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            // Add some default trackers
                            viewModel.saveTracker(TrackerDefinition(name = context.getString(R.string.weight), type = TrackerType.FLOAT, unit = "kg", min = 20.0, max = 320.0))
                            viewModel.saveTracker(TrackerDefinition(name = context.getString(R.string.temperature), type = TrackerType.FLOAT, unit = "°C", min = 35.0, max = 42.5))
                            viewModel.saveTracker(TrackerDefinition(name = context.getString(R.string.took_medication), type = TrackerType.BOOLEAN))
                            viewModel.saveTracker(TrackerDefinition(name = context.getString(R.string.notes), type = TrackerType.STRING))
                        }) {
                            Text(stringResource(R.string.button_add_default_trackers))
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
                            onEdit = { trackerToEdit = tracker },
                            onDelete = { trackerToDelete = tracker }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            TrackerDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { updatedTracker ->
                    viewModel.saveTracker(updatedTracker)
                    showAddDialog = false
                }
            )
        }

        trackerToEdit?.let { tracker ->
            TrackerDialog(
                tracker = tracker,
                onDismiss = { trackerToEdit = null },
                onConfirm = { updatedTracker ->
                    viewModel.saveTracker(updatedTracker)
                    trackerToEdit = null
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
    onEdit: () -> Unit,
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
                TrackerType.FLOAT -> stringResource(R.string.tracker_type_number)
                TrackerType.BOOLEAN -> stringResource(R.string.tracker_type_boolean)
                TrackerType.STRING -> stringResource(R.string.tracker_type_text)
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
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.button_edit),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.button_delete),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDialog(
    tracker: TrackerDefinition? = null,
    onDismiss: () -> Unit,
    onConfirm: (TrackerDefinition) -> Unit
) {
    var name by remember { mutableStateOf(tracker?.name ?: "") }
    var type by remember { mutableStateOf(tracker?.type ?: TrackerType.FLOAT) }
    var unit by remember { mutableStateOf(tracker?.unit ?: "") }
    var min by remember { mutableStateOf(tracker?.min?.toString() ?: "") }
    var max by remember { mutableStateOf(tracker?.max?.toString() ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                if (tracker == null) stringResource(R.string.dialog_title_add_tracker)
                else stringResource(R.string.dialog_title_edit_tracker)
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_tracker_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = when (type) {
                            TrackerType.FLOAT -> stringResource(R.string.tracker_type_number)
                            TrackerType.BOOLEAN -> stringResource(R.string.tracker_type_boolean)
                            TrackerType.STRING -> stringResource(R.string.tracker_type_text)
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.label_tracker_type)) },
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
                                            TrackerType.FLOAT -> stringResource(R.string.tracker_type_number)
                                            TrackerType.BOOLEAN -> stringResource(R.string.tracker_type_boolean)
                                            TrackerType.STRING -> stringResource(R.string.tracker_type_text)
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
                        label = { Text(stringResource(R.string.label_tracker_unit)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = min,
                            onValueChange = { min = it },
                            label = { Text(stringResource(R.string.label_tracker_min)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = max,
                            onValueChange = { max = it },
                            label = { Text(stringResource(R.string.label_tracker_max)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { 
                onConfirm(
                    TrackerDefinition(
                        id = tracker?.id ?: 0,
                        name = name,
                        type = type,
                        unit = unit.ifBlank { null },
                        min = min.toDoubleOrNull(),
                        max = max.toDoubleOrNull(),
                        isActive = tracker?.isActive ?: true,
                        showOnChart = tracker?.showOnChart ?: false
                    )
                ) 
            }, enabled = name.isNotBlank()) {
                Text(
                    if (tracker == null) stringResource(R.string.button_add)
                    else stringResource(R.string.button_save)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
