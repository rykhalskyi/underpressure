package com.example.underpressure

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
import com.example.underpressure.alarm.AlarmScheduler
import com.example.underpressure.data.export.TableExportManager
import com.example.underpressure.data.local.database.AppDatabase
import com.example.underpressure.data.repository.MeasurementRepositoryImpl
import com.example.underpressure.data.repository.SettingsRepositoryImpl
import com.example.underpressure.ui.settings.SettingsScreen
import com.example.underpressure.ui.settings.SettingsViewModel
import com.example.underpressure.ui.table.MeasurementTableScreen
import com.example.underpressure.ui.table.MeasurementTableViewModel
import com.example.underpressure.ui.table.SearchViewModel
import com.example.underpressure.ui.table.ShareViewModel
import com.example.underpressure.ui.theme.UnderPressureTheme

class MainActivity : ComponentActivity() {

    private val viewModelFactory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val settingsRepository = SettingsRepositoryImpl(database.appSettingsDao())
                val measurementRepository = MeasurementRepositoryImpl(database.measurementDao())
                val alarmScheduler = AlarmScheduler(applicationContext)
                
                return if (modelClass.isAssignableFrom(MeasurementTableViewModel::class.java)) {
                    MeasurementTableViewModel(measurementRepository, settingsRepository, alarmScheduler = alarmScheduler) as T
                } else if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    SettingsViewModel(settingsRepository, alarmScheduler) as T
                } else if (modelClass.isAssignableFrom(SearchViewModel::class.java)) {
                    SearchViewModel(measurementRepository) as T
                } else if (modelClass.isAssignableFrom(ShareViewModel::class.java)) {
                    val exportManager = TableExportManager(applicationContext, measurementRepository, settingsRepository)
                    ShareViewModel(exportManager) as T
                } else {
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }

    private val tableViewModel: MeasurementTableViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }
    private val searchViewModel: SearchViewModel by viewModels { viewModelFactory }
    private val shareViewModel: ShareViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnderPressureTheme {
                var isSettingsOpen by remember { mutableStateOf(false) }

                if (isSettingsOpen) {
                    SettingsScreen(
                        viewModel = settingsViewModel,
                        onBack = { isSettingsOpen = false }
                    )
                } else {
                    MeasurementTableScreen(
                        viewModel = tableViewModel,
                        searchViewModel = searchViewModel,
                        shareViewModel = shareViewModel,
                        onSettingsClick = { isSettingsOpen = true }
                    )
                }
            }
        }
    }
}
