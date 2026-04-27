package com.otakeessen.underpressure.data.export

import android.content.Context
import android.net.Uri
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Manages the parsing and importing of measurement data from CSV files.
 */
class TableImportManager(
    private val context: Context,
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    sealed class ImportStrategy {
        object Overwrite : ImportStrategy()
        object Skip : ImportStrategy()
    }

    data class ImportResult(
        val successCount: Int,
        val totalCount: Int,
        val error: String? = null
    )

    /**
     * Imports measurements from a CSV file.
     * 
     * @param uri The URI of the CSV file.
     * @param strategy The strategy to use when a measurement for the same date and slot already exists.
     * @return Result of the import operation.
     */
    suspend fun importCsv(uri: Uri, strategy: ImportStrategy): ImportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext ImportResult(0, 0, "Could not open file")
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine() ?: return@withContext ImportResult(0, 0, "File is empty")
            
            val headers = headerLine.split(",")
            if (headers.isEmpty() || headers[0] != "Date") {
                return@withContext ImportResult(0, 0, "Invalid CSV format: first column must be 'Date'")
            }

            // Get current settings to map slot times to indices
            val settings = settingsRepository.getSettingsSync() ?: return@withContext ImportResult(0, 0, "Could not retrieve settings")
            val slotTimes = settings.slotTimes

            // Map CSV column index to slot index in the app
            val columnToSlotMap = mutableMapOf<Int, Int>()
            for (i in 1 until headers.size) {
                val headerTime = headers[i].trim()
                val slotIndex = slotTimes.indexOf(headerTime)
                if (slotIndex != -1) {
                    columnToSlotMap[i] = slotIndex
                }
            }

            if (columnToSlotMap.isEmpty()) {
                return@withContext ImportResult(0, 0, "No matching time slots found in the CSV header")
            }

            var successCount = 0
            var totalProcessed = 0
            
            // Read data rows
            var line: String? = reader.readLine()
            while (line != null) {
                val columns = line.split(",")
                if (columns.isNotEmpty()) {
                    val dateStr = columns[0].trim()
                    try {
                        // Validate date
                        LocalDate.parse(dateStr, dateFormatter)
                        
                        // Get existing measurements for this date to handle collisions
                        val existingMeasurements = measurementRepository.getMeasurementsByDateSync(dateStr)

                        columnToSlotMap.forEach { (colIndex, slotIndex) ->
                            if (colIndex < columns.size) {
                                val value = columns[colIndex].trim()
                                if (value.isNotBlank() && value != "—") {
                                    totalProcessed++
                                    val (systolic, diastolic, pulse) = parseMeasurementValue(value)
                                    
                                    val existing = existingMeasurements.find { it.slotIndex == slotIndex }
                                    
                                    val entity = MeasurementEntity(
                                        id = existing?.id ?: 0,
                                        date = dateStr,
                                        slotIndex = slotIndex,
                                        systolic = systolic,
                                        diastolic = diastolic,
                                        pulse = pulse,
                                        updatedAt = System.currentTimeMillis()
                                    )

                                    if (existing == null) {
                                        measurementRepository.saveMeasurement(entity)
                                        successCount++
                                    } else if (strategy == ImportStrategy.Overwrite) {
                                        measurementRepository.updateMeasurement(entity)
                                        successCount++
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip invalid rows or handle error
                    }
                }
                line = reader.readLine()
            }
            
            ImportResult(successCount, totalProcessed)
        } catch (e: Exception) {
            ImportResult(0, 0, e.message)
        }
    }

    private fun parseMeasurementValue(value: String): Triple<Int, Int, Int> {
        // Formats: "120/80", "120/80@70"
        return try {
            if (value.contains("@")) {
                val parts = value.split("@")
                val bpParts = parts[0].split("/")
                Triple(bpParts[0].toInt(), bpParts[1].toInt(), parts[1].toInt())
            } else {
                val bpParts = value.split("/")
                Triple(bpParts[0].toInt(), bpParts[1].toInt(), 0)
            }
        } catch (e: Exception) {
            Triple(0, 0, 0)
        }
    }
}
