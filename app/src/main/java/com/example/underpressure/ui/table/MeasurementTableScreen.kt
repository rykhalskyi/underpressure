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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.underpressure.ui.table.components.DayRow
import com.example.underpressure.ui.table.components.TableHeader

/**
 * Root screen for the Measurement Table feature.
 *
 * @param viewModel The ViewModel providing the table data.
 * @param modifier Modifier to be applied to the screen.
 */
@Composable
fun MeasurementTableScreen(
    viewModel: MeasurementTableViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator()
        } else if (uiState.error != null) {
            Text(
                text = uiState.error ?: "Unknown Error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TableHeader()
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = uiState.items,
                        key = { it.date }
                    ) { summary ->
                        DayRow(summary = summary)
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
}
