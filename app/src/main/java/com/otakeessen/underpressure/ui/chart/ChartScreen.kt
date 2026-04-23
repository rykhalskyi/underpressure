package com.otakeessen.underpressure.ui.chart

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.ui.chart.components.BloodPressureChart
import com.otakeessen.underpressure.ui.chart.components.ChartConfigurationSheet
import kotlinx.coroutines.flow.collectLatest

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
    
    var captureBPBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }
    var capturePulseBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }

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
                            val bpBitmap = captureBPBitmap?.invoke()
                            val pulseBitmap = capturePulseBitmap?.invoke()
                            
                            val finalBitmap = when {
                                bpBitmap != null && pulseBitmap != null -> combineBitmaps(bpBitmap, pulseBitmap)
                                bpBitmap != null -> bpBitmap
                                pulseBitmap != null -> pulseBitmap
                                else -> null
                            }
                            
                            finalBitmap?.let { viewModel.onShareChart(it) }
                        },
                        enabled = uiState.bpLineData != null || uiState.pulseLineData != null
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                } else if (uiState.errorMessageResId != null && uiState.bpLineData == null && uiState.pulseLineData == null) {
                    Text(
                        text = stringResource(uiState.errorMessageResId!!),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    // Blood Pressure Chart
                    if (uiState.bpLineData != null) {
                        BloodPressureChart(
                            lineData = uiState.bpLineData,
                            startDate = uiState.startDate,
                            modifier = Modifier
                                .weight(if (uiState.pulseLineData != null) 2f else 1f)
                                .fillMaxWidth(),
                            showXAxisLabels = uiState.pulseLineData == null,
                            onChartReady = { captureBPBitmap = it }
                        )
                    }

                    // Pulse Chart
                    if (uiState.pulseLineData != null) {
                        BloodPressureChart(
                            lineData = uiState.pulseLineData,
                            startDate = uiState.startDate,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onChartReady = { capturePulseBitmap = it }
                        )
                    }
                }
            }

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
                fromDate = uiState.fromDate,
                toDate = uiState.toDate,
                onDismiss = { viewModel.toggleConfigSheet(false) },
                onApply = { slots, types, from, to ->
                    viewModel.updateConfiguration(slots, types, from, to)
                },
                sheetState = sheetState
            )
        }
    }
}

private fun combineBitmaps(top: Bitmap, bottom: Bitmap): Bitmap {
    val width = maxOf(top.width, bottom.width)
    val height = top.height + bottom.height
    val combined = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(combined)
    canvas.drawBitmap(top, 0f, 0f, null)
    canvas.drawBitmap(bottom, 0f, top.height.toFloat(), null)
    return combined
}

