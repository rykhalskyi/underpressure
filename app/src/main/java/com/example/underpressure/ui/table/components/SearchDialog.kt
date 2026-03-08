package com.example.underpressure.ui.table.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.underpressure.ui.table.SearchViewModel

/**
 * A dialog providing the search interface.
 */
@Composable
fun SearchDialog(
    viewModel: SearchViewModel,
    onDismiss: () -> Unit,
    onResultClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Search Measurements") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.updateQuery(it) },
                    label = { Text("Enter date (YYYY-MM-DD) or value") },
                    isError = uiState.dateError != null,
                    supportingText = {
                        if (uiState.dateError != null) {
                            Text(text = uiState.dateError!!, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else if (uiState.isNoResults) {
                        Text(
                            text = "No results found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(uiState.results) { measurement ->
                                SearchResultItem(
                                    measurement = measurement,
                                    onClick = { onResultClick(measurement.date) }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    thickness = 0.5.dp
                                )
                            }
                            
                            // Special case for valid date query that has no specific result list
                            if (uiState.query.isNotBlank() && uiState.dateError == null && uiState.results.isEmpty()) {
                                item {
                                    TextButton(
                                        onClick = { onResultClick(uiState.query) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Jump to Date: ${uiState.query}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        modifier = modifier
    )
}
