package com.otakeessen.underpressure.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otakeessen.underpressure.ui.settings.components.GlobalAlarmRow
import com.otakeessen.underpressure.ui.settings.components.SlotRow
import com.otakeessen.underpressure.ui.settings.components.TimePickerDialog

import androidx.compose.ui.res.stringResource
import com.otakeessen.underpressure.R

import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ListItem
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.ui.onboarding.OnboardingDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.material3.RadioButton
import androidx.compose.material3.LinearProgressIndicator
import com.otakeessen.underpressure.ui.settings.components.ImportMappingDialog
import com.otakeessen.underpressure.ui.settings.components.ImpressumDialog

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
    val allTrackers by viewModel.allTrackers.collectAsStateWithLifecycle()
    var showTimePickerForIndex by remember { mutableStateOf<Int?>(null) }
    var showOnboarding by remember { mutableStateOf(false) }
    var showImpressum by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val csvPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                viewModel.onImportCsvUriSelected(it)
            }
        }
    )

    val versionName = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                ).versionName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    // Refresh permission status when returning to the app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { _ ->
            viewModel.refreshPermissionStatus()
        }
    )

    LaunchedEffect(uiState.isMasterAlarmEnabled) {
        if (uiState.isMasterAlarmEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_go_back)
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
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        if (!uiState.canScheduleExactAlarms && uiState.isMasterAlarmEnabled) {
                            ExactAlarmWarning(
                                onGrantClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                        context.startActivity(intent)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.header_measurement_slots),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    itemsIndexed(uiState.slots) { index, slot ->
                        SlotRow(
                            slot = slot,
                            onTimeClick = { showTimePickerForIndex = index },
                            onActiveChange = { viewModel.updateSlotActive(index, it) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                    item {
                        GlobalAlarmRow(
                            enabled = uiState.isMasterAlarmEnabled,
                            onCheckedChange = { viewModel.updateMasterAlarmEnabled(it) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    item {
                        Text(
                            text = stringResource(R.string.header_classification_guidelines),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                        )
                    }

                    item {
                        GuidelineSelection(
                            selected = uiState.bpGuidelines,
                            onSelected = { viewModel.updateBpGuidelines(it) }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    item {
                        Text(
                            text = stringResource(R.string.header_data_management),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.button_import_csv)) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
                                csvPickerLauncher.launch("text/csv")
                            }
                        )
                        if (uiState.isImporting) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    item {
                        Text(
                            text = stringResource(R.string.header_about),
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
                        )
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.label_version, versionName ?: "Unknown")) },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null
                                )
                            },
                            modifier = Modifier.clickable {
                                showOnboarding = true
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.label_privacy_policy)) },
                            supportingContent = { Text(stringResource(R.string.url_privacy_policy)) },
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_privacy_policy)))
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.label_terms_of_service)) },
                            supportingContent = { Text(stringResource(R.string.url_terms_of_service)) },
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.url_terms_of_service)))
                                context.startActivity(intent)
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Impressum only for German locale
                    if (context.resources.configuration.locales.get(0).language == "de") {
                        item {
                            ListItem(
                                headlineContent = { Text(stringResource(R.string.label_impressum)) },
                                modifier = Modifier.clickable {
                                    showImpressum = true
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }

                    item {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.label_repo)) },
                            supportingContent = { Text("https://github.com/rykhalskyi/underpressure") },
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rykhalskyi/underpressure"))
                                context.startActivity(intent)
                            }
                        )
                    }

                }
            }
        }

        if (showOnboarding) {
            OnboardingDialog(
                onDismiss = { showOnboarding = false }
            )
        }

        if (showImpressum) {
            val content = try {
                context.assets.open("impressum.md").bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                "Impressum not available."
            }
            ImpressumDialog(
                content = content,
                onDismiss = { showImpressum = false }
            )
        }

        // Error Dialog
        uiState.error?.let { errorText ->
            val isTimeError = errorText.startsWith("hint_cannot_create_slot|")
            val displayedError = if (isTimeError) {
                val time = errorText.substringAfter("|")
                stringResource(R.string.hint_cannot_create_slot, time)
            } else {
                errorText
            }
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { 
                    Text(
                        if (isTimeError) stringResource(R.string.dialog_title_invalid_time) 
                        else stringResource(R.string.unknown_error)
                    ) 
                },
                text = { Text(displayedError) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text(stringResource(R.string.button_ok))
                    }
                }
            )
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

        if (uiState.showImportStrategyDialog) {
            ImportDialog(
                onDismiss = { viewModel.cancelImport() },
                onConfirm = { overwrite ->
                    viewModel.onImportCsv(overwrite)
                }
            )
        }

        uiState.trackerDiscoveryResult?.let { result ->
            ImportMappingDialog(
                discoveryResult = result,
                existingTrackers = allTrackers,
                onConfirm = { overwrite, mapping ->
                    viewModel.onImportCsv(overwrite, mapping)
                },
                onDismiss = { viewModel.cancelImport() }
            )
        }

        uiState.importResult?.let { result ->
            AlertDialog(
                onDismissRequest = { viewModel.clearImportResult() },
                title = { Text(stringResource(R.string.dialog_title_import)) },
                text = {
                    Text(
                        if (result.contains("/"))
                            stringResource(R.string.message_import_success, result.split("/")[0].toInt())
                        else
                            stringResource(R.string.message_import_error, result)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearImportResult() }) {
                        Text(stringResource(R.string.button_ok))
                    }
                }
            )
        }
    }
}

@Composable
fun GuidelineSelection(
    selected: BpGuidelines,
    onSelected: (BpGuidelines) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.selectableGroup()) {
        GuidelineOption(
            title = stringResource(R.string.label_guideline_aha_title),
            subtitle = stringResource(R.string.label_guideline_aha_subtitle),
            selected = selected == BpGuidelines.AHA_ACC,
            onClick = { onSelected(BpGuidelines.AHA_ACC) }
        )
        GuidelineOption(
            title = stringResource(R.string.label_guideline_esc_title),
            subtitle = stringResource(R.string.label_guideline_esc_subtitle),
            selected = selected == BpGuidelines.ESC_ESH,
            onClick = { onSelected(BpGuidelines.ESC_ESH) }
        )
    }
}

@Composable
fun GuidelineOption(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var overwrite by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_title_import)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                Text(
                    text = stringResource(R.string.label_import_strategy),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                StrategyOption(
                    text = stringResource(R.string.option_skip),
                    selected = !overwrite,
                    onClick = { overwrite = false }
                )
                StrategyOption(
                    text = stringResource(R.string.option_overwrite),
                    selected = overwrite,
                    onClick = { overwrite = true }
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(overwrite) }) {
                Text(stringResource(R.string.button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}

@Composable
fun StrategyOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null // null depends on the selectable modifier
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
fun ExactAlarmWarning(
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.warning_exact_alarms_title),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(R.string.warning_exact_alarms_body),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onGrantClick) {
                Text(stringResource(R.string.button_grant))
            }
        }
    }
}
