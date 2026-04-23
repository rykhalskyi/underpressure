package com.otakeessen.underpressure.data.local

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.otakeessen.underpressure.data.local.database.AppDatabase
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var measurementDao: com.otakeessen.underpressure.data.local.dao.MeasurementDao
    private lateinit var appSettingsDao: com.otakeessen.underpressure.data.local.dao.AppSettingsDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).allowMainThreadQueries().build()
        measurementDao = db.measurementDao()
        appSettingsDao = db.appSettingsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeMeasurementAndReadInList() = runTest {
        val measurement = MeasurementEntity(
            date = "2026-03-06",
            slotIndex = 0,
            systolic = 120,
            diastolic = 80,
            pulse = 70
        )
        measurementDao.insert(measurement)
        val allMeasurements = measurementDao.getAll().first()
        assertEquals(allMeasurements[0].systolic, 120)
    }

    @Test
    fun queryByDateReturnsCorrectMeasurements() = runTest {
        val date1 = "2026-03-06"
        val date2 = "2026-03-07"
        
        measurementDao.insert(MeasurementEntity(date = date1, slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70))
        measurementDao.insert(MeasurementEntity(date = date1, slotIndex = 1, systolic = 125, diastolic = 85, pulse = 72))
        measurementDao.insert(MeasurementEntity(date = date2, slotIndex = 0, systolic = 130, diastolic = 90, pulse = 75))

        val resultsDate1 = measurementDao.getByDate(date1).first()
        assertEquals(2, resultsDate1.size)
        
        val resultsDate2 = measurementDao.getByDate(date2).first()
        assertEquals(1, resultsDate2.size)
    }

    @Test
    fun queryByValueReturnsCorrectMeasurements() = runTest {
        measurementDao.insert(MeasurementEntity(date = "2026-03-06", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70))
        measurementDao.insert(MeasurementEntity(date = "2026-03-07", slotIndex = 0, systolic = 130, diastolic = 120, pulse = 75))

        val results = measurementDao.getByValue(120).first()
        assertEquals(2, results.size)
    }

    @Test
    fun updateMeasurementReflectsChanges() = runTest {
        val id = measurementDao.insert(MeasurementEntity(date = "2026-03-06", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70))
        val original = measurementDao.getAll().first().first { it.id == id }
        
        val updated = original.copy(systolic = 140, updatedAt = System.currentTimeMillis())
        measurementDao.update(updated)
        
        val result = measurementDao.getAll().first().first { it.id == id }
        assertEquals(140, result.systolic)
    }

    @Test
    fun deleteMeasurementRemovesFromDb() = runTest {
        val measurement = MeasurementEntity(date = "2026-03-06", slotIndex = 0, systolic = 120, diastolic = 80, pulse = 70)
        val id = measurementDao.insert(measurement)
        val entity = measurementDao.getAll().first().first { it.id == id }
        
        measurementDao.delete(entity)
        val all = measurementDao.getAll().first()
        assertEquals(0, all.size)
    }

    @Test
    fun upsertSettingsWorksForSingleton() = runTest {
        val settings = AppSettingsEntity(
            masterAlarmEnabled = true,
            slotTimes = listOf("08:00", "12:00", "20:00"),
            slotAlarmsEnabled = listOf(true, false, true)
        )
        appSettingsDao.upsert(settings)
        
        val result = appSettingsDao.getSettings().first()
        assertNotNull(result)
        assertEquals(true, result?.masterAlarmEnabled)
        assertEquals(3, result?.slotTimes?.size)
        assertEquals("08:00", result?.slotTimes?.get(0))
        
        // Update
        val updatedSettings = result!!.copy(masterAlarmEnabled = false)
        appSettingsDao.upsert(updatedSettings)
        
        val finalResult = appSettingsDao.getSettings().first()
        assertEquals(false, finalResult?.masterAlarmEnabled)
        assertEquals(1, finalResult?.id) // Ensure ID remains 1
    }

    @Test
    fun searchByValue_returnsPartialMatches() = runTest {
        val m1 = MeasurementEntity(1, "2024-03-01", 0, 120, 80, 60, 0)
        val m2 = MeasurementEntity(2, "2024-03-02", 0, 130, 85, 65, 0)
        val m3 = MeasurementEntity(3, "2024-03-03", 0, 140, 120, 70, 0)

        measurementDao.insert(m1)
        measurementDao.insert(m2)
        measurementDao.insert(m3)

        // Search for "120"
        val results = measurementDao.searchByValue("%120%").first()
        assertEquals(2, results.size)
        assertTrue(results.any { it.systolic == 120 })
        assertTrue(results.any { it.diastolic == 120 })

        // Search for "30"
        val results2 = measurementDao.searchByValue("%30%").first()
        assertEquals(1, results2.size)
        assertEquals(130, results2[0].systolic)
    }
}

