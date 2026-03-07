package com.example.underpressure.ui.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.underpressure.ui.table.components.DayRow
import com.example.underpressure.ui.table.components.MeasurementEditDialog
import com.example.underpressure.ui.table.components.TableHeader

/**
 * Root screen for the Measurement Table feature (Multi-slot view).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementTableScreen(
    viewModel: MeasurementTableViewModel,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("UnderPressure") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Open Settings"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error ?: "Unknown Error",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TableHeader(slotHeaders = uiState.slotHeaders)
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = uiState.items,
                            key = { it.date }
                        ) { summary ->
                            DayRow(
                                summary = summary,
                                slotCount = uiState.slotHeaders.size,
                                onCellClick = { slotIndex -> 
                                    viewModel.onCellClicked(summary.date, slotIndex)
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        }

        if (uiState.dialogState.isOpen) {
            MeasurementEditDialog(
                state = uiState.dialogState,
                onSave = { viewModel.onSaveMeasurement(it) },
                onDismiss = { viewModel.onDialogDismiss() }
            )
        }
    }
}
