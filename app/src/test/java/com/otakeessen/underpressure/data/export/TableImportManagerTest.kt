package com.otakeessen.underpressure.data.export

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.export.TrackerMappingAction
import com.otakeessen.underpressure.domain.export.TrackerMatchStatus
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.time.ZoneId
import java.time.ZonedDateTime

class TableImportManagerTest {

    private val context: Context = mockk()
    private val measurementRepository: MeasurementRepository = mockk()
    private val settingsRepository: SettingsRepository = mockk()
    private val trackerRepository: TrackerRepository = mockk()
    private val contentResolver: ContentResolver = mockk()
    private val uri: Uri = mockk()

    private lateinit var importManager: TableImportManager

    @Before
    fun setup() {
        importManager = TableImportManager(context, measurementRepository, settingsRepository, trackerRepository)
        every { context.contentResolver } returns contentResolver
    }

    @Test
    fun `importCsv saves tracker values linked to measurements`() = runTest {
        // Arrange
        val csvContent = """
            Date,Slot 1,Anytime,[Tracker] Weight (kg)
            2026-05-15,120/80,130/85 (14:30),75.5 (14:30)
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(csvContent.toByteArray())
        every { contentResolver.openInputStream(uri) } returns inputStream
        
        val settings = AppSettingsEntity(slotTimes = listOf("08:00"))
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { measurementRepository.getMeasurementsByDateSync("2026-05-15") } returns emptyList()
        coEvery { measurementRepository.saveMeasurement(any()) } returns 101L andThen 102L // 101 for Slot 1, 102 for Anytime

        val trackerDef = TrackerDefinition(id = 1, name = "Weight", unit = "kg", type = TrackerType.FLOAT)
        coEvery { trackerRepository.getTrackerDefinitionById(1) } returns trackerDef
        coEvery { trackerRepository.getTrackerValueByMeasurementAndTracker(any(), any()) } returns null
        coEvery { trackerRepository.saveTrackerValue(any()) } returns 1L

        val mapping = mapOf("[Tracker] Weight (kg)" to TrackerMappingAction.MapToExisting(1))

        // Act
        val result = importManager.importCsv(uri, TableImportManager.ImportStrategy.Skip, mapping)

        // Assert
        assertEquals(2, result.successCount)
        assertEquals(1, result.trackerValuesCount)

        // Verify tracker value is linked to measurement 102 (Anytime 14:30) because of timestamp match
        coVerify { 
            trackerRepository.saveTrackerValue(match { 
                it.measurementId == 102L && it.trackerId == 1L && it.floatValue == 75.5
            }) 
        }
    }

    @Test
    fun `discoverTrackers correctly identifies new, matched, and conflicted trackers`() = runTest {
        // Arrange
        val csvHeader = "Date,Slot 1,[Tracker] Weight (kg),[Tracker] Mood,[Tracker] Temp (C)\n"
        val inputStream = ByteArrayInputStream(csvHeader.toByteArray())
        every { contentResolver.openInputStream(uri) } returns inputStream
        
        val existingTrackers = listOf(
            TrackerDefinition(id = 1, name = "Weight", unit = "kg", type = TrackerType.FLOAT), // Exact match
            TrackerDefinition(id = 2, name = "Mood", unit = "score", type = TrackerType.FLOAT), // Conflict (unit)
        )
        every { trackerRepository.getAllTrackerDefinitions() } returns flowOf(existingTrackers)

        // Act
        val result = importManager.discoverTrackers(uri)

        // Assert
        assertEquals(3, result.discoveredTrackers.size)
        assertEquals(5, result.totalColumns)

        // Weight (kg) -> Exact Match
        val weight = result.discoveredTrackers.find { it.extractedName == "Weight" }!!
        assertEquals(TrackerMatchStatus.EXACT_MATCH, weight.matchStatus)
        assertEquals("kg", weight.unit)

        // Mood -> Conflict (CSV has no unit, DB has "score")
        val mood = result.discoveredTrackers.find { it.extractedName == "Mood" }!!
        assertEquals(TrackerMatchStatus.CONFLICT, mood.matchStatus)
        assertEquals(null, mood.unit)

        // Temp (C) -> New
        val temp = result.discoveredTrackers.find { it.extractedName == "Temp" }!!
        assertEquals(TrackerMatchStatus.NEW, temp.matchStatus)
        assertEquals("C", temp.unit)
    }

    @Test
    fun `importCsv correctly parses Anytime column with multiple readings and timestamps`() = runTest {
        val csvContent = """
            Date,07:00,Anytime
            2026-05-15,120/80,130/85@70 (14:30); 125/82 (19:00)
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(csvContent.toByteArray())
        every { contentResolver.openInputStream(uri) } returns inputStream
        
        val settings = AppSettingsEntity(slotTimes = listOf("07:00"))
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { measurementRepository.getMeasurementsByDateSync("2026-05-15") } returns emptyList()
        coEvery { measurementRepository.saveMeasurement(any()) } returns 1L

        val result = importManager.importCsv(uri, TableImportManager.ImportStrategy.Skip)

        assertEquals(3, result.successCount)
        assertEquals(3, result.totalCount)

        // Verify scheduled reading
        coVerify { 
            measurementRepository.saveMeasurement(match { 
                it.date == "2026-05-15" && it.slotIndex == 0 && it.systolic == 120 && it.diastolic == 80 
            }) 
        }

        // Verify anytime readings with timestamps
        val expectedTime1 = ZonedDateTime.of(2026, 5, 15, 14, 30, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expectedTime2 = ZonedDateTime.of(2026, 5, 15, 19, 0, 0, 0, ZoneId.systemDefault()).toInstant().toEpochMilli()

        coVerify { 
            measurementRepository.saveMeasurement(match { 
                it.date == "2026-05-15" && it.slotIndex == -1 && it.systolic == 130 && it.diastolic == 85 && it.pulse == 70 && it.timestamp == expectedTime1
            }) 
        }
        coVerify { 
            measurementRepository.saveMeasurement(match { 
                it.date == "2026-05-15" && it.slotIndex == -1 && it.systolic == 125 && it.diastolic == 82 && it.pulse == 0 && it.timestamp == expectedTime2
            }) 
        }
    }

    @Test
    fun `importCsv skips Anytime readings without timestamps`() = runTest {
        val csvContent = """
            Date,07:00,Anytime
            2026-05-15,120/80,130/85
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(csvContent.toByteArray())
        every { contentResolver.openInputStream(uri) } returns inputStream
        
        val settings = AppSettingsEntity(slotTimes = listOf("07:00"))
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { measurementRepository.getMeasurementsByDateSync("2026-05-15") } returns emptyList()
        coEvery { measurementRepository.saveMeasurement(any()) } returns 1L

        val result = importManager.importCsv(uri, TableImportManager.ImportStrategy.Skip)

        // Only the scheduled reading (120/80) should be imported. The Anytime one (130/85) is skipped.
        assertEquals(1, result.successCount)
        assertEquals(1, result.totalCount)
        
        coVerify(exactly = 1) { measurementRepository.saveMeasurement(any()) }
        coVerify { 
            measurementRepository.saveMeasurement(match { 
                it.date == "2026-05-15" && it.slotIndex == 0 && it.systolic == 120
            }) 
        }
    }

    @Test
    fun `importCsv handles Slot X headers independently of app slot times`() = runTest {
        // App has "08:00" as Slot 1, but CSV says "Slot 1" (which might have been "07:00" before)
        val csvContent = """
            Date,Slot 1,Anytime
            2026-05-15,120/80,130/85 (14:30)
        """.trimIndent()
        
        val inputStream = ByteArrayInputStream(csvContent.toByteArray())
        every { contentResolver.openInputStream(uri) } returns inputStream
        
        val settings = AppSettingsEntity(slotTimes = listOf("08:00")) // Different time
        coEvery { settingsRepository.getSettingsSync() } returns settings
        coEvery { measurementRepository.getMeasurementsByDateSync("2026-05-15") } returns emptyList()
        coEvery { measurementRepository.saveMeasurement(any()) } returns 1L

        val result = importManager.importCsv(uri, TableImportManager.ImportStrategy.Skip)

        assertEquals(2, result.successCount)
        
        coVerify { 
            measurementRepository.saveMeasurement(match { 
                it.date == "2026-05-15" && it.slotIndex == 0 && it.systolic == 120
            }) 
        }
        
        coVerify { 
            measurementRepository.saveMeasurement(match { 
                it.date == "2026-05-15" && it.slotIndex == -1 && it.systolic == 130
            }) 
        }
    }
}
