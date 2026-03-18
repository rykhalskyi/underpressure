package com.example.underpressure.ui.table

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.underpressure.ui.table.ShareViewModel.ShareEvent
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import com.example.underpressure.ui.table.components.ShareDialog
import com.example.underpressure.ui.table.components.DayRow
import com.example.underpressure.ui.table.components.MeasurementEditDialog
import com.example.underpressure.ui.table.components.SearchDialog
import com.example.underpressure.ui.table.components.TableHeader

import androidx.compose.ui.res.stringResource
import com.example.underpressure.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeasurementTableScreen(
    viewModel: MeasurementTableViewModel,
    searchViewModel: SearchViewModel,
    shareViewModel: ShareViewModel,
    onSettingsClick: () -> Unit,
    onChartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isSearchDialogOpen by remember { mutableStateOf(false) }
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // Clean up observer on dispose
    }

    LaunchedEffect(shareViewModel.shareEvents) {
        shareViewModel.shareEvents.collectLatest { event ->
            when (event) {
                is ShareEvent.ShareText -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, event.text)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title))
                    context.startActivity(shareIntent)
                    shareViewModel.onDismissDialog()
                }
                is ShareEvent.ShareFile -> {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        event.file
                    )
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "text/csv"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.export_chooser_title))
                    context.startActivity(shareIntent)
                    shareViewModel.onDismissDialog()
                }
                is ShareEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    LaunchedEffect(viewModel.scrollToDateEvent) {
        viewModel.scrollToDateEvent.collect { date ->
             val index = uiState.items.indexOfFirst { it.date == date }
             if (index != -1) {
                 lazyListState.animateScrollToItem(index)
             }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onChartClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = stringResource(R.string.cd_chart)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleMasterAlarm() }) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = stringResource(R.string.cd_toggle_alarms),
                            tint = if (uiState.isMasterAlarmEnabled) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    }
                    IconButton(onClick = { isSearchDialogOpen = true }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(R.string.cd_search)
                        )
                    }
                    IconButton(onClick = { shareViewModel.onOpenDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.cd_settings)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (uiState.isFabEnabled) viewModel.onFabClicked() },
                containerColor = if (uiState.isFabEnabled) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (uiState.isFabEnabled)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.cd_add_measurement)
                )
            }
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
                    text = uiState.error ?: stringResource(R.string.unknown_error),
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    TableHeader(slotHeaders = uiState.slotHeaders)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = lazyListState
                    ) {
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

        if (isSearchDialogOpen) {
            SearchDialog(
                viewModel = searchViewModel,
                onDismiss = { isSearchDialogOpen = false },
                onResultClick = { date ->
                    isSearchDialogOpen = false
                    viewModel.onDateSelectedFromSearch(date)
                }
            )
        }

        ShareDialog(
            viewModel = shareViewModel,
            onDismiss = { shareViewModel.onDismissDialog() }
        )
    }
}
