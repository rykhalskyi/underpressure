package com.example.underpressure.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.underpressure.ui.settings.components.SlotRow
import com.example.underpressure.ui.settings.components.TimePickerDialog

/**
 * Screen for configuring application settings, specifically measurement slot times and activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back"
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
                    text = uiState.error ?: "Error loading settings",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Text(
                            text = "Measurement Slots",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    itemsIndexed(uiState.slots) { index, slot ->
                        SlotRow(
                            slot = slot,
                            onTimeClick = { showTimePickerForIndex = index },
                            onActiveChange = { viewModel.updateSlotActive(index, it) },
                            onAlarmChange = { viewModel.updateSlotAlarmEnabled(index, it) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }

    // Time Picker Dialog Logic
    showTimePickerForIndex?.let { index ->
        val initialTime = uiState.slots[index].time
        TimePickerDialog(
            initialTime = initialTime,
            onDismiss = { showTimePickerForIndex = null },
            onConfirm = { newTime ->
                viewModel.updateSlotTime(index, newTime)
                showTimePickerForIndex = null
            }
        )
    }
}
