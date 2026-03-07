package com.example.underpressure.data.repository

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.underpressure.data.local.database.AppDatabase
import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RepositoryIntegrationTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var measurementRepository: MeasurementRepository
    private lateinit var settingsRepository: SettingsRepository

    @Before
    fun createRepos() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        
        measurementRepository = MeasurementRepositoryImpl(db.measurementDao())
        settingsRepository = SettingsRepositoryImpl(db.appSettingsDao())
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun saveAndRetrieveMeasurement() = runTest {
        val measurement = MeasurementEntity(
            date = "2026-03-07",
            slotIndex = 0,
            systolic = 120,
            diastolic = 80,
            pulse = 70
        )
        val id = measurementRepository.saveMeasurement(measurement)
        
        val allMeasurements = measurementRepository.getAllMeasurements().first()
        assertEquals(1, allMeasurements.size)
        assertEquals(id, allMeasurements[0].id)
        assertEquals(120, allMeasurements[0].systolic)
    }

    @Test
    fun updateAndRetrieveMeasurement() = runTest {
        val measurement = MeasurementEntity(
            date = "2026-03-07",
            slotIndex = 0,
            systolic = 120,
            diastolic = 80,
            pulse = 70
        )
        val id = measurementRepository.saveMeasurement(measurement)
        val original = measurementRepository.getAllMeasurements().first().first { it.id == id }
        
        val updated = original.copy(systolic = 130)
        measurementRepository.updateMeasurement(updated)
        
        val result = measurementRepository.getAllMeasurements().first().first { it.id == id }
        assertEquals(130, result.systolic)
    }

    @Test
    fun deleteMeasurement() = runTest {
        val measurement = MeasurementEntity(
            date = "2026-03-07",
            slotIndex = 0,
            systolic = 120,
            diastolic = 80,
            pulse = 70
        )
        val id = measurementRepository.saveMeasurement(measurement)
        val entity = measurementRepository.getAllMeasurements().first().first { it.id == id }
        
        measurementRepository.deleteMeasurement(entity)
        
        val all = measurementRepository.getAllMeasurements().first()
        assertEquals(0, all.size)
    }

    @Test
    fun getMeasurementsByDate() = runTest {
        val date = "2026-03-07"
        measurementRepository.saveMeasurement(MeasurementEntity(date = date, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70))
        measurementRepository.saveMeasurement(MeasurementEntity(date = "2026-03-08", slotIndex = 0, systolic = 130, diastolic = 85, pulse = 72))
        
        val results = measurementRepository.getMeasurementsByDate(date).first()
        assertEquals(1, results.size)
        assertEquals(date, results[0].date)
    }

    @Test
    fun getMeasurementsByValue() = runTest {
        measurementRepository.saveMeasurement(MeasurementEntity(date = "2026-03-07", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70))
        measurementRepository.saveMeasurement(MeasurementEntity(date = "2026-03-08", slotIndex = 0, systolic = 130, diastolic = 120, pulse = 75))
        
        val results = measurementRepository.getMeasurementsByValue(120).first()
        assertEquals(2, results.size)
    }

    @Test
    fun saveAndRetrieveSettings() = runTest {
        val settings = AppSettingsEntity(
            masterAlarmEnabled = true,
            slotTimes = listOf("09:00", "21:00"),
            slotAlarmsEnabled = listOf(true, true)
        )
        settingsRepository.saveSettings(settings)
        
        val result = settingsRepository.getSettings().first()
        assertNotNull(result)
        assertEquals(true, result?.masterAlarmEnabled)
        assertEquals("09:00", result?.slotTimes?.get(0))
    }
}
