package com.example.underpressure.ui.chart

import android.content.Intent
import android.graphics.Bitmap
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
import com.example.underpressure.R
import com.example.underpressure.ui.chart.components.BloodPressureChart
import com.example.underpressure.ui.chart.components.ChartConfigurationSheet
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
    
    var captureChartBitmap by remember { mutableStateOf<(() -> Bitmap)?>(null) }

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
                    snackbarHostState.showSnackbar(event.message)
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
                            captureChartBitmap?.invoke()?.let { bitmap ->
                                viewModel.onShareChart(bitmap)
                            }
                        },
                        enabled = uiState.lineData != null
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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator()
                } else if (uiState.errorMessage != null && uiState.lineData == null) {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    BloodPressureChart(
                        lineData = uiState.lineData,
                        modifier = Modifier.fillMaxSize(),
                        onChartReady = { captureChartBitmap = it }
                    )
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
