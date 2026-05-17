package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.ui.table.SearchFilter
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
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val uiState by viewModel.resultsState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

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
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.button_clear))
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .focusRequester(focusRequester)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilter.values().filter { it != SearchFilter.NONE }.forEach { f ->
                        FilterChip(
                            selected = filter == f,
                            onClick = { viewModel.setFilter(f) },
                            label = {
                                val label = when (f) {
                                    SearchFilter.HYPOTENSION -> stringResource(R.string.bp_level_hypotension)
                                    SearchFilter.NORMAL -> stringResource(R.string.bp_level_normal)
                                    SearchFilter.ELEVATED -> stringResource(if (guidelines == BpGuidelines.AHA_ACC) R.string.bp_level_elevated else R.string.bp_level_high_normal)
                                    SearchFilter.STAGE_1 -> stringResource(if (guidelines == BpGuidelines.AHA_ACC) R.string.bp_level_stage1 else R.string.bp_level_grade1)
                                    SearchFilter.STAGE_2 -> stringResource(if (guidelines == BpGuidelines.AHA_ACC) R.string.bp_level_stage2 else R.string.bp_level_grade2)
                                    else -> ""
                                }
                                Text(label)
                            }
                        )
                    }
                }

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
