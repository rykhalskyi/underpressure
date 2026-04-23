package com.otakeessen.underpressure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.data.export.ChartExportManager
import com.otakeessen.underpressure.data.export.TableExportManager
import com.otakeessen.underpressure.data.local.database.AppDatabase
import com.otakeessen.underpressure.data.repository.MeasurementRepositoryImpl
import com.otakeessen.underpressure.data.repository.SettingsRepositoryImpl
import com.otakeessen.underpressure.ui.chart.ChartScreen
import com.otakeessen.underpressure.ui.chart.ChartViewModel
import com.otakeessen.underpressure.ui.settings.SettingsScreen
import com.otakeessen.underpressure.ui.settings.SettingsViewModel
import com.otakeessen.underpressure.ui.table.MeasurementTableScreen
import com.otakeessen.underpressure.ui.table.MeasurementTableViewModel
import com.otakeessen.underpressure.ui.table.SearchViewModel
import com.otakeessen.underpressure.ui.table.ShareViewModel
import com.otakeessen.underpressure.ui.theme.UnderPressureTheme

enum class Screen {
    Table,
    Settings,
    Chart
}

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val settingsRepository = SettingsRepositoryImpl(database.appSettingsDao())
                val measurementRepository = MeasurementRepositoryImpl(database.measurementDao())
                val alarmScheduler = AlarmScheduler(applicationContext)
                
                return when {
                    modelClass.isAssignableFrom(MeasurementTableViewModel::class.java) -> {
                        MeasurementTableViewModel(measurementRepository, settingsRepository, alarmScheduler = alarmScheduler) as T
                    }
                    modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                        SettingsViewModel(settingsRepository, alarmScheduler) as T
                    }
                    modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                        SearchViewModel(measurementRepository) as T
                    }
                    modelClass.isAssignableFrom(ShareViewModel::class.java) -> {
                        val exportManager = TableExportManager(applicationContext, measurementRepository, settingsRepository)
                        ShareViewModel(exportManager) as T
                    }
                    modelClass.isAssignableFrom(ChartViewModel::class.java) -> {
                        val chartExportManager = ChartExportManager(applicationContext)
                        ChartViewModel(measurementRepository, settingsRepository, chartExportManager) as T
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnderPressureTheme {
                var currentScreen by remember { mutableStateOf(Screen.Table) }

                when (currentScreen) {
                    Screen.Table -> {
                        MeasurementTableScreen(
                            viewModel = tableViewModel,
                            searchViewModel = searchViewModel,
                            shareViewModel = shareViewModel,
                            onSettingsClick = { currentScreen = Screen.Settings },
                            onChartClick = { currentScreen = Screen.Chart }
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
                }
            }
        }
    }
}

