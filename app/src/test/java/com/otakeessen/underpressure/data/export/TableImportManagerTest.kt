package com.otakeessen.underpressure.data.export

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
    private val contentResolver: ContentResolver = mockk()
    private val uri: Uri = mockk()

    private lateinit var importManager: TableImportManager

    @Before
    fun setup() {
        importManager = TableImportManager(context, measurementRepository, settingsRepository)
        every { context.contentResolver } returns contentResolver
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
