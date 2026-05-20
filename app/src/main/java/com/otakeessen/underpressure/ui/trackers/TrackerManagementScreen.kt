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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
                            onToggleChart = { viewModel.toggleTrackerOnChart(tracker) },
                            onToggleSecondary = { viewModel.toggleTrackerSecondaryAxis(tracker) },
                            onDelete = { viewModel.deleteTracker(tracker) }
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
    }
}

@Composable
fun TrackerItem(
    tracker: TrackerDefinition,
    onToggleActive: () -> Unit,
    onToggleChart: () -> Unit,
    onToggleSecondary: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = tracker.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = "Type: ${tracker.type}${if (tracker.unit != null) " (${tracker.unit})" else ""}", style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = tracker.isActive, onCheckedChange = { onToggleActive() })
                Text("Active")
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Checkbox(checked = tracker.showOnChart, onCheckedChange = { onToggleChart() })
                Text("Show on Chart")
            }
            
            if (tracker.type == TrackerType.FLOAT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = tracker.useSecondaryAxis, onCheckedChange = { onToggleSecondary() })
                    Text("Use Secondary Y-Axis")
                }
            }
        }
    }
}

@Composable
fun AddTrackerDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, TrackerType, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TrackerType.FLOAT) }
    var unit by remember { mutableStateOf("") }

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
                
                Text("Type:")
                Row {
                    TrackerType.values().forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            RadioButton(selected = type == t, onClick = { type = t })
                            Text(t.name)
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
