package com.otakeessen.underpressure.data.export

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class TableImportManagerTest {

    private lateinit var context: Context
    private lateinit var measurementRepository: MeasurementRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var contentResolver: ContentResolver
    private lateinit var importManager: TableImportManager

    @Before
    fun setUp() {
        context = mockk()
        measurementRepository = mockk(relaxed = true)
        settingsRepository = mockk()
        contentResolver = mockk()

        every { context.contentResolver } returns contentResolver
        
        importManager = TableImportManager(context, measurementRepository, settingsRepository)
    }

    @Test
    fun `importCsv maps columns to slots by position regardless of header time`() = runBlocking {
        // CSV: Date,Slot1,Slot2,Slot3,Slot4
        // Header "07:00" will be ignored and treated as Slot 0, "12:00" as Slot 1
        val csvData = "Date,07:00,12:00,17:00,20:00\n2026-05-02,120/80,130/85,110/70,125/75"
        val uri: Uri = mockk()
        
        every { contentResolver.openInputStream(uri) } returns ByteArrayInputStream(csvData.toByteArray())
        coEvery { measurementRepository.getMeasurementsByDateSync("2026-05-02") } returns emptyList()

        val result = importManager.importCsv(uri, TableImportManager.ImportStrategy.Overwrite)

        assertEquals(4, result.successCount)
        
        // Verify slots 0, 1, 2, 3 were saved
        coVerify { measurementRepository.saveMeasurement(match { it.slotIndex == 0 && it.systolic == 120 }) }
        coVerify { measurementRepository.saveMeasurement(match { it.slotIndex == 1 && it.systolic == 130 }) }
        coVerify { measurementRepository.saveMeasurement(match { it.slotIndex == 2 && it.systolic == 110 }) }
        coVerify { measurementRepository.saveMeasurement(match { it.slotIndex == 3 && it.systolic == 125 }) }
    }
}
