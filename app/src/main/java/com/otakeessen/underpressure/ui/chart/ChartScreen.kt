package com.otakeessen.underpressure.ui.chart

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.ui.chart.components.BloodPressureBarChart
import com.otakeessen.underpressure.ui.chart.components.BloodPressureChart
import com.otakeessen.underpressure.ui.chart.components.BloodPressurePieChart
import com.otakeessen.underpressure.ui.chart.components.ChartConfigurationSheet
import kotlinx.coroutines.flow.collectLatest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    viewModel: ChartViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState()
    
    var captureSysBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }
    var captureDiaBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }
    var capturePulseBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }
    var captureBarBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }
    var capturePieBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ChartViewModel.ChartEvent.ShareFile -> {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        event.file
                    )
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.share_chooser_title))
                    context.startActivity(shareIntent)
                }
                is ChartViewModel.ChartEvent.Error -> {
                    val message = if (event.arg != null) {
                        context.getString(event.messageResId, event.arg)
                    } else {
                        context.getString(event.messageResId)
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_chart)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_go_back)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val bitmaps = if (uiState.chartMode == ChartMode.DISTRIBUTION) {
                                listOfNotNull(captureBarBitmap?.invoke(), capturePieBitmap?.invoke())
                            } else {
                                listOfNotNull(captureSysBitmap?.invoke(), captureDiaBitmap?.invoke(), capturePulseBitmap?.invoke())
                            }
                            
                            val finalBitmap = when {
                                bitmaps.size >= 2 -> combineBitmaps(bitmaps)
                                bitmaps.size == 1 -> bitmaps[0]
                                else -> null
                            }
                            
                            finalBitmap?.let { viewModel.onShareChart(it) }
                        },
                        enabled = if (uiState.chartMode == ChartMode.DISTRIBUTION) {
                            uiState.distributionBarData != null
                        } else {
                            uiState.sysLineData != null || uiState.diaLineData != null || uiState.pulseLineData != null
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.cd_share)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Switcher at the top
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                ChartMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ChartMode.entries.size),
                        onClick = { viewModel.setChartMode(mode) },
                        selected = uiState.chartMode == mode
                    ) {
                        Text(stringResource(when(mode) {
                            ChartMode.DAILY -> R.string.label_chart_mode_daily
                            ChartMode.SEQUENTIAL -> R.string.label_chart_mode_sequential
                            ChartMode.DISTRIBUTION -> R.string.label_chart_mode_distribution
                        }))
                    }
                }
            }

            // Charts in the middle
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.errorMessageResId != null && 
                    uiState.sysLineData == null && uiState.diaLineData == null && uiState.pulseLineData == null &&
                    uiState.distributionBarData == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(uiState.errorMessageResId!!),
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            if (uiState.chartMode == ChartMode.DISTRIBUTION) {
                                // Bar Chart
                                if (uiState.distributionBarData != null) {
                                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        Text(
                                            text = stringResource(R.string.label_distribution_bar_chart),
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                        BloodPressureBarChart(
                                            barData = uiState.distributionBarData,
                                            xLabels = uiState.xLabels,
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            onChartReady = { captureBarBitmap = it }
                                        )
                                    }
                                }

                                // Pie Chart
                                if (uiState.distributionPieData != null) {
                                    Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        Text(
                                            text = stringResource(R.string.label_distribution_pie_chart),
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )
                                        BloodPressurePieChart(
                                            pieData = uiState.distributionPieData,
                                            modifier = Modifier.weight(1f).fillMaxWidth(),
                                            onChartReady = { capturePieBitmap = it }
                                        )
                                    }
                                }
                            } else {
                                // Systolic Chart
                                if (uiState.sysLineData != null) {
                                    BloodPressureChart(
                                        lineData = uiState.sysLineData,
                                        startDate = uiState.startDate,
                                        xLabels = uiState.xLabels,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        showXAxisLabels = true,
                                        onChartReady = { captureSysBitmap = it },
                                        showRiskZones = uiState.showRiskZones
                                    )
                                }

                                // Diastolic Chart
                                if (uiState.diaLineData != null) {
                                    BloodPressureChart(
                                        lineData = uiState.diaLineData,
                                        startDate = uiState.startDate,
                                        xLabels = uiState.xLabels,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        showXAxisLabels = true,
                                        onChartReady = { captureDiaBitmap = it },
                                        showRiskZones = uiState.showRiskZones
                                    )
                                }

                                // Pulse Chart
                                if (uiState.pulseLineData != null) {
                                    BloodPressureChart(
                                        lineData = uiState.pulseLineData,
                                        startDate = uiState.startDate,
                                        xLabels = uiState.xLabels,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        showXAxisLabels = true,
                                        onChartReady = { capturePulseBitmap = it },
                                        showRiskZones = uiState.showRiskZones
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Range Presets above the bottom bar
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val presets = listOf(
                    DatePreset.ALL_TIME to R.string.label_all_time,
                    DatePreset.LAST_7_DAYS to R.string.label_date_preset_7d,
                    DatePreset.THIS_MONTH to R.string.label_date_preset_month,
                    DatePreset.CUSTOM to R.string.label_date_preset_custom
                )
                items(presets) { (preset, labelRes) ->
                    FilterChip(
                        selected = uiState.selectedDatePreset == preset,
                        onClick = { 
                            viewModel.setDatePreset(preset)
                            if (preset == DatePreset.CUSTOM) {
                                showDatePicker = true
                            }
                        },
                        label = { Text(stringResource(labelRes)) },
                        leadingIcon = if (uiState.selectedDatePreset == preset) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null
                    )
                }
            }

            // Slot and feature toggle chips
            // Slot and feature toggle chips
            if (uiState.showInteractiveLegend) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(uiState.slotTimes.withIndex().toList()) { (index, label) ->
                        FilterChip(
                            selected = uiState.selectedSlots.contains(index),
                            onClick = { viewModel.toggleSlot(index) },
                            label = { Text(label) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = uiState.showRollingAverage,
                            onClick = { viewModel.toggleRollingAverage() },
                            label = { Text(stringResource(R.string.label_show_rolling_average)) }
                        )
                    }
                }
            }

            // Bottom Bar with Configure button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.toggleConfigSheet(true) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text(stringResource(R.string.button_configure))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (uiState.isConfigSheetOpen) {
            ChartConfigurationSheet(
                selectedSlots = uiState.selectedSlots,
                selectedTypes = uiState.selectedTypes,
                showRiskZones = uiState.showRiskZones,
                showRollingAverage = uiState.showRollingAverage,
                showInteractiveLegend = uiState.showInteractiveLegend,
                onDismiss = { viewModel.toggleConfigSheet(false) },
                onToggleSlot = { viewModel.toggleSlot(it) },
                onToggleType = { viewModel.toggleType(it) },
                onToggleRiskZones = { viewModel.toggleRiskZones() },
                onToggleRollingAverage = { viewModel.toggleRollingAverage() },
                onToggleInteractiveLegend = { viewModel.toggleInteractiveLegend() },
                sheetState = sheetState
            )
        }
    }

    if (showDatePicker) {
        val dateRangePickerState = rememberDateRangePickerState(
            initialSelectedStartDateMillis = uiState.fromDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
            initialSelectedEndDateMillis = uiState.toDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = {
                viewModel.setDatePreset(DatePreset.ALL_TIME)
                showDatePicker = false
            },
            confirmButton = {
                TextButton(onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    val end = dateRangePickerState.selectedEndDateMillis?.let {
                        Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    viewModel.setCustomDateRange(start, end)
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setDatePreset(DatePreset.ALL_TIME)
                    showDatePicker = false
                }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text(modifier = Modifier.padding(16.dp), text = stringResource(R.string.label_select_date_range)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun combineBitmaps(bitmaps: List<Bitmap>): Bitmap {
    val width = bitmaps.maxOf { it.width }
    val height = bitmaps.sumOf { it.height }
    val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(combined)
    var y = 0f
    for (bitmap in bitmaps) {
        canvas.drawBitmap(bitmap, 0f, y, null)
        y += bitmap.height.toFloat()
    }
    return combined
}
