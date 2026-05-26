package com.otakeessen.underpressure

import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.export.ChartExportManager
import com.otakeessen.underpressure.data.export.TableExportManager
import com.otakeessen.underpressure.data.export.TableImportManager
import com.otakeessen.underpressure.data.local.database.AppDatabase
import com.otakeessen.underpressure.data.repository.MeasurementRepositoryImpl
import com.otakeessen.underpressure.data.repository.SettingsRepositoryImpl
import com.otakeessen.underpressure.ui.chart.ChartScreen
import com.otakeessen.underpressure.ui.chart.ChartViewModel
import com.otakeessen.underpressure.ui.onboarding.OnboardingDialog
import com.otakeessen.underpressure.ui.settings.SettingsScreen
import com.otakeessen.underpressure.ui.settings.SettingsViewModel
import com.otakeessen.underpressure.ui.table.MeasurementTableScreen
import com.otakeessen.underpressure.ui.table.MeasurementTableViewModel
import com.otakeessen.underpressure.ui.table.SearchViewModel
import com.otakeessen.underpressure.ui.table.ShareViewModel
import com.otakeessen.underpressure.ui.theme.UnderPressureTheme

import com.otakeessen.underpressure.data.repository.TrackerRepositoryImpl
import com.otakeessen.underpressure.ui.trackers.TrackerManagementScreen
import com.otakeessen.underpressure.ui.trackers.TrackerViewModel

enum class Screen {
    Table,
    Settings,
    Chart,
    Trackers
}

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val settingsRepository = SettingsRepositoryImpl(database.appSettingsDao())
                val measurementRepository = MeasurementRepositoryImpl(database.measurementDao())
                val trackerRepository = TrackerRepositoryImpl(database.trackerDao())
                val alarmScheduler = AlarmScheduler(applicationContext)
                
                return when {
                    modelClass.isAssignableFrom(MeasurementTableViewModel::class.java) -> {
                        MeasurementTableViewModel(measurementRepository, settingsRepository, trackerRepository, alarmScheduler = alarmScheduler) as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        val importManager = TableImportManager(applicationContext, measurementRepository, settingsRepository, trackerRepository)
                        SettingsViewModel(settingsRepository, alarmScheduler, importManager, trackerRepository, measurementRepository) as T
                    }
                    modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                        SearchViewModel(measurementRepository, settingsRepository, trackerRepository) as T
                    }
                    modelClass.isAssignableFrom(ShareViewModel::class.java) -> {
                        val exportManager = TableExportManager(applicationContext, measurementRepository, settingsRepository, trackerRepository)
                        ShareViewModel(exportManager) as T
                    }
                    modelClass.isAssignableFrom(ChartViewModel::class.java) -> {
                        val chartExportManager = ChartExportManager(applicationContext)
                        ChartViewModel(measurementRepository, settingsRepository, trackerRepository, chartExportManager) as T
                    }
                    modelClass.isAssignableFrom(TrackerViewModel::class.java) -> {
                        TrackerViewModel(trackerRepository) as T
                    }
                    else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }

    private val tableViewModel: MeasurementTableViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }
    private val searchViewModel: SearchViewModel by viewModels { viewModelFactory }
    private val shareViewModel: ShareViewModel by viewModels { viewModelFactory }
    private val chartViewModel: ChartViewModel by viewModels { viewModelFactory }
    private val trackerViewModel: TrackerViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnderPressureTheme {
                var currentScreen by remember { mutableStateOf(Screen.Table) }
                val settingsUiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
                
                val versionName = remember {
                    try {
                        packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown"
                    } catch (e: Exception) {
                        "Unknown"
                    }
                }

                var showAutoOnboarding by remember { mutableStateOf(false) }

                LaunchedEffect(settingsUiState.isLoading, settingsUiState.lastOnboardedVersion) {
                    if (!settingsUiState.isLoading && settingsUiState.lastOnboardedVersion != versionName) {
                        showAutoOnboarding = true
                    }
                }

                if (showAutoOnboarding) {
                    OnboardingDialog(
                        onDismiss = {
                            settingsViewModel.setOnboardingSeen(versionName)
                            showAutoOnboarding = false
                        }
                    )
                }

                when (currentScreen) {
                    Screen.Table -> {
                        MeasurementTableScreen(
                            viewModel = tableViewModel,
                            searchViewModel = searchViewModel,
                            shareViewModel = shareViewModel,
                            onSettingsClick = { currentScreen = Screen.Settings },
                            onChartClick = { currentScreen = Screen.Chart },
                            onTrackersClick = { currentScreen = Screen.Trackers }
                        )
                    }
                    Screen.Settings -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { currentScreen = Screen.Table }
                        )
                    }
                    Screen.Chart -> {
                        ChartScreen(
                            viewModel = chartViewModel,
                            onBack = { currentScreen = Screen.Table }
                        )
                    }
                    Screen.Trackers -> {
                        TrackerManagementScreen(
                            viewModel = trackerViewModel,
                            onBack = { currentScreen = Screen.Table }
                        )
                    }
                }
            }
        }
    }
}
