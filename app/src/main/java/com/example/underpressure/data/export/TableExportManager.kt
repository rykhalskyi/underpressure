package com.example.underpressure.data.export

import android.content.Context
import com.example.underpressure.data.local.entities.AppSettingsEntity
import com.example.underpressure.domain.export.TableFormatter
import com.example.underpressure.domain.repository.MeasurementRepository
import com.example.underpressure.domain.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Manages the retrieval and formatting of data for export.
 */
class TableExportManager(
    private val context: Context,
    private val measurementRepository: MeasurementRepository,
    private val settingsRepository: SettingsRepository,
    private val tableFormatter: TableFormatter = TableFormatter(),
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val filenameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    /**
     * Saves CSV content to a file in the app's cache directory.
     *
     * @param content The CSV string content.
     * @return The saved File object.
     */
    suspend fun saveCsvToCache(content: String): File = withContext(Dispatchers.IO) {
        val timestamp = LocalDateTime.now().format(filenameFormatter)
        val filename = "table_export_$timestamp.csv"
        
        // Use the cache directory configured in file_paths.xml (path=".")
        // context.cacheDir maps to the root of the cache directory
        val file = File(context.cacheDir, filename)
        
        file.writeText(content)
        file
    }

    /**
     * Generates a formatted ASCII table of measurements.
     */
    suspend fun generateAsciiTable(
        from: LocalDate?,
        to: LocalDate?,
    ): String = withContext(Dispatchers.IO) {
        val (headers, rows, dateRange) = prepareExportData(from, to)
        tableFormatter.formatAsciiTable(headers, rows, dateRange)
    }

    /**
     * Generates a formatted CSV string of measurements.
     */
    suspend fun generateCsvContent(
        from: LocalDate?,
        to: LocalDate?,
    ): String = withContext(Dispatchers.IO) {
        val (headers, rows, _) = prepareExportData(from, to)
        tableFormatter.formatCsv(headers, rows)
    }

    private suspend fun prepareExportData(
        from: LocalDate?,
        to: LocalDate?,
    ): Triple<List<String>, List<List<String>>, String> {
        val allMeasurements = measurementRepository.getAllMeasurementsSync()
        val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()

        // Filter measurements by date range
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val filteredMeasurements = allMeasurements.filter { measurement ->
            val measureDate = LocalDate.parse(measurement.date, dateFormatter)
            val isAfterFrom = from == null || !measureDate.isBefore(from)
            val isBeforeTo = to == null || !measureDate.isAfter(to)
            isAfterFrom && isBeforeTo
        }

        // Determine active slots and headers
        val allTimes = settings.slotTimes
        val activeFlags = settings.slotActiveFlags

        // Map original index to a pair of (Active Index, Header Time)
        val activeSlotsMap = activeFlags
            .mapIndexedNotNull { index, isActive ->
                if (isActive) index to allTimes.getOrElse(index) { "" } else null
            }

        val headers = listOf("Date") + activeSlotsMap.map { it.second }

        // Group by date and build rows
        val groupedByDate = filteredMeasurements.groupBy { it.date }

        // Sort dates descending (or ascending? usually logs are desc, but export might be asc.
        // Requirements example shows 2026-03-10 then 11. Let's do Ascending for export.)
        val sortedDates = groupedByDate.keys.sorted()

        val rows = mutableListOf<List<String>>()
        sortedDates.forEach { date ->
            val dailyMeasurements = groupedByDate[date] ?: emptyList()

            val rowValues = mutableListOf<String>()
            rowValues.add(date)

            activeSlotsMap.forEach { (originalIndex, _) ->
                val measurement = dailyMeasurements.find { it.slotIndex == originalIndex }
                val cellValue = measurement?.let {
                    "${it.systolic}/${it.diastolic}@${it.pulse}"
                } ?: ""
                rowValues.add(cellValue)
            }
            rows.add(rowValues)
        }

        // Generate Date Range String
        val rangeStart = from?.format(dateFormatter) ?: sortedDates.firstOrNull() ?: "Start"
        val rangeEnd = to?.format(dateFormatter) ?: sortedDates.lastOrNull() ?: "End"
        val dateRange = "$rangeStart → $rangeEnd"

        return Triple(headers, rows, dateRange)
    }
}
