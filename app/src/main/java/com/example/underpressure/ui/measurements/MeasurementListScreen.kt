package com.example.underpressure.ui.measurements

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.underpressure.R
import com.example.underpressure.data.local.entities.MeasurementListEntity
import com.example.underpressure.data.local.entities.MeasurementListType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementListScreen(
    viewModel: MeasurementListViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_measurement_lists)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("add_list_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add List")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(uiState.lists) { list ->
                    MeasurementListItem(
                        list = list,
                        onToggleActive = { viewModel.toggleListActive(list) },
                        onDelete = { viewModel.deleteList(list) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddListDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, type ->
                    viewModel.addList(name, type)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun MeasurementListItem(
    list: MeasurementListEntity,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent = { Text(list.name) },
        supportingContent = { Text(list.type.name) },
        trailingContent = {
            Row {
                Switch(checked = list.active, onCheckedChange = { onToggleActive() })
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }
        }
    )
}

@Composable
fun AddListDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, MeasurementListType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(MeasurementListType.DOUBLE) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Measurement List") },
        text = {
            Column {
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.testTag("list_name_input")
                )
                Spacer(Modifier.height(8.dp))
                Text("Type:")
                MeasurementListType.values().forEach { t ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = type == t, onClick = { type = t })
                        Text(t.name)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, type) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("confirm_add_list")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
