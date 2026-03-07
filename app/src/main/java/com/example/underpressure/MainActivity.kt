package com.example.underpressure

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.underpressure.data.local.database.AppDatabase
import com.example.underpressure.data.repository.MeasurementRepositoryImpl
import com.example.underpressure.ui.table.MeasurementTableScreen
import com.example.underpressure.ui.table.MeasurementTableViewModel
import com.example.underpressure.ui.theme.UnderPressureTheme

class MainActivity : ComponentActivity() {

    /**
     * Factory for creating ViewModels with dependencies.
     * In a real app, this would be handled by Hilt/Dagger.
     */
    private val viewModelFactory by lazy {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = MeasurementRepositoryImpl(database.measurementDao())
                return MeasurementTableViewModel(repository) as T
            }
        }
    }

    private val viewModel: MeasurementTableViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UnderPressureTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MeasurementTableScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
