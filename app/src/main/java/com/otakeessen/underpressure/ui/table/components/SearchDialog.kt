package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.ui.table.SearchUiState
import com.otakeessen.underpressure.ui.table.SearchViewModel

/**
 * Dialog for searching and jumping to specific measurements.
 */
@Composable
fun SearchDialog(
    viewModel: SearchViewModel,
    guidelines: BpGuidelines,
    onDismiss: () -> Unit,
    onResultClick: (date: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.resultsState.collectAsStateWithLifecycle()

    AlertDialog(
        modifier = modifier.fillMaxHeight(0.8f),
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.dialog_title_search))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { viewModel.updateQuery(it) },
                    label = { Text(stringResource(R.string.label_search)) },
                    supportingText = { Text(stringResource(R.string.helper_search_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                if (uiState.results.isEmpty() && query.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.message_no_results),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.results) { measurement ->
                            SearchResultItem(
                                measurement = measurement,
                                guidelines = guidelines,
                                onClick = { onResultClick(measurement.date) }
                            )
                            HorizontalDivider(
                                thickness = 0.5.dp, 
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_close))
            }
        }
    )
}
