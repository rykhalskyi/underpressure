package com.otakeessen.underpressure.data.export

import android.content.Context
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TableExportManagerTest {

    private val context: Context = mockk()
    private val measurementRepository: MeasurementRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val trackerRepository: TrackerRepository = mockk()
    
    private lateinit var exportManager: TableExportManager

    @Before
    fun setup() {
        exportManager = TableExportManager(context, measurementRepository, settingsRepository, trackerRepository)
    }

    @Test
    fun `generateCsvContent includes tracker columns and values`() = runTest {
        // Arrange
        val date = "2026-05-15"
        val timestamp = LocalDateTime.of(2026, 5, 15, 8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val measurement = MeasurementEntity(
            id = 1,
            date = date,
            slotIndex = 0,
            systolic = 120,
            diastolic = 80,
            pulse = 70,
            timestamp = timestamp,
            createdAt = timestamp
        )
        
        val trackerDef = TrackerDefinition(id = 1, name = "Weight", unit = "kg", type = TrackerType.FLOAT)
        val trackerValue = TrackerValue(id = 1, trackerId = 1, measurementId = 1, floatValue = 75.5)

        coEvery { measurementRepository.getAllMeasurementsSync() } returns listOf(measurement)
        coEvery { settingsRepository.getSettingsSync() } returns AppSettingsEntity(
            slotTimes = listOf("08:00"),
            slotActiveFlags = listOf(true)
        )
        every { trackerRepository.getAllTrackerDefinitions() } returns flowOf(listOf(trackerDef))
        every { trackerRepository.getAllTrackerValues() } returns flowOf(listOf(trackerValue))

        // Act
        val csv = exportManager.generateCsvContent(null, null)
        println("CSV Output:\n$csv")

        // Assert
        val lines = csv.split("\n").filter { it.isNotBlank() }
        assertEquals(2, lines.size)
        
        // Header: Date, Slot 1, Anytime, [Tracker] Weight (kg)
        assertEquals("Date,Slot 1,Anytime,[Tracker] Weight (kg)", lines[0].trim())
        
        // Row: 2026-05-15, 120/80@70, "", 75.5 (08:00)
        assertTrue(lines[1].contains("2026-05-15"))
        assertTrue(lines[1].contains("120/80@70"))
        assertTrue(lines[1].contains("75.5 (08:00)"))
    }

    @Test
    fun `generateCsvContent handles multiple tracker values per day`() = runTest {
        // Arrange
        val date = "2026-05-15"
        val t1 = LocalDateTime.of(2026, 5, 15, 8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val t2 = LocalDateTime.of(2026, 5, 15, 14, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val m1 = MeasurementEntity(
            id = 1,
            date = date,
            slotIndex = 0,
            systolic = 120,
            diastolic = 80,
            pulse = 0,
            timestamp = t1,
            createdAt = t1
        )
        val m2 = MeasurementEntity(
            id = 2,
            date = date,
            slotIndex = -1,
            systolic = 130,
            diastolic = 85,
            pulse = 0,
            timestamp = t2,
            createdAt = t2
        )
        
        val trackerDef = TrackerDefinition(id = 1, name = "Mood", unit = "", type = TrackerType.STRING)
        val v1 = TrackerValue(id = 1, trackerId = 1, measurementId = 1, stringValue = "Good")
        val v2 = TrackerValue(id = 2, trackerId = 1, measurementId = 2, stringValue = "Tired")

        coEvery { measurementRepository.getAllMeasurementsSync() } returns listOf(m1, m2)
        coEvery { settingsRepository.getSettingsSync() } returns AppSettingsEntity(
            slotTimes = listOf("08:00"),
            slotActiveFlags = listOf(true)
        )
        every { trackerRepository.getAllTrackerDefinitions() } returns flowOf(listOf(trackerDef))
        every { trackerRepository.getAllTrackerValues() } returns flowOf(listOf(v1, v2))

        // Act
        val csv = exportManager.generateCsvContent(null, null)
        println("CSV Output:\n$csv")

        // Assert
        val lines = csv.split("\n").filter { it.isNotBlank() }
        // Row should have "Good (08:00); Tired (14:30)" in the tracker column
        assertTrue(lines[1].contains("Good (08:00); Tired (14:30)"))
    }

    @Test
    fun `generateCsvContent excludes trackers without values in range`() = runTest {
        // Arrange
        val t1 = LocalDateTime.of(2026, 5, 15, 8, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val m1 = MeasurementEntity(
            id = 1, 
            date = "2026-05-15", 
            slotIndex = 0, 
            systolic = 120, 
            diastolic = 80, 
            pulse = 0,
            timestamp = t1,
            createdAt = t1
        )
        val trackerDef1 = TrackerDefinition(id = 1, name = "Used", unit = "", type = TrackerType.BOOLEAN)
        val trackerDef2 = TrackerDefinition(id = 2, name = "Unused", unit = "", type = TrackerType.BOOLEAN)
        val trackerValue = TrackerValue(id = 1, trackerId = 1, measurementId = 1, booleanValue = true)

        coEvery { measurementRepository.getAllMeasurementsSync() } returns listOf(m1)
        coEvery { settingsRepository.getSettingsSync() } returns AppSettingsEntity(
            slotTimes = listOf("08:00"),
            slotActiveFlags = listOf(true)
        )
        every { trackerRepository.getAllTrackerDefinitions() } returns flowOf(listOf(trackerDef1, trackerDef2))
        every { trackerRepository.getAllTrackerValues() } returns flowOf(listOf(trackerValue))

        // Act
        val csv = exportManager.generateCsvContent(null, null)
        println("CSV Output:\n$csv")

        // Assert
        val headers = csv.split("\n")[0]
        assertTrue(headers.contains("[Tracker] Used"))
        assertTrue(!headers.contains("[Tracker] Unused"))
    }
}
