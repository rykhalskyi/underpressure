package com.otakeessen.underpressure.data.export

import android.content.Context
import com.otakeessen.underpressure.data.local.entities.AppSettingsEntity
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.domain.export.TableFormatter
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    private val trackerRepository: TrackerRepository,
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

    /**
     * Retrieves the range of dates for which measurements exist.
     */
    suspend fun getDateRange(): Pair<LocalDate?, LocalDate?> = withContext(Dispatchers.IO) {
        val minDateStr = measurementRepository.getMinDate()
        val maxDateStr = measurementRepository.getMaxDate()
        
        val minDate = minDateStr?.let { LocalDate.parse(it, dateFormatter) }
        val maxDate = maxDateStr?.let { LocalDate.parse(it, dateFormatter) }
        
        minDate to maxDate
    }

    private suspend fun prepareExportData(
        from: LocalDate?,
        to: LocalDate?,
    ): Triple<List<String>, List<List<String>>, String> {
        val allMeasurements = measurementRepository.getAllMeasurementsSync()
        val settings = settingsRepository.getSettingsSync() ?: AppSettingsEntity()
        
        val allTrackerDefinitions = trackerRepository.getAllTrackerDefinitions().first()
        val allTrackerValues = trackerRepository.getAllTrackerValues().first()

        // Filter measurements by date range
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val filteredMeasurements = allMeasurements.filter { measurement ->
            val measureDate = LocalDate.parse(measurement.date, dateFormatter)
            val isAfterFrom = from == null || !measureDate.isBefore(from)
            val isBeforeTo = to == null || !measureDate.isAfter(to)
            isAfterFrom && isBeforeTo
        }

        // Determine which trackers have values in the filtered range
        val filteredMeasurementIds = filteredMeasurements.map { it.id }.toSet()
        val trackersInRange = allTrackerValues
            .filter { it.measurementId in filteredMeasurementIds }
            .map { it.trackerId }
            .toSet()
        
        val activeTrackerDefinitions = allTrackerDefinitions.filter { it.id in trackersInRange }

        // Determine active slots and headers
        val allTimes = settings.slotTimes
        val activeFlags = settings.slotActiveFlags

        // Map original index to a pair of (Active Index, Header Time)
        val activeSlotsMap = activeFlags
            .mapIndexedNotNull { index, isActive ->
                if (isActive) index to allTimes.getOrElse(index) { "" } else null
            }

        val bpHeaders = listOf("Date") + activeSlotsMap.map { "Slot ${it.first + 1}" } + listOf("Anytime")
        val trackerHeaders = activeTrackerDefinitions.map { tracker ->
            val unitPart = if (!tracker.unit.isNullOrBlank()) " (${tracker.unit})" else ""
            "[Tracker] ${tracker.name}$unitPart [${tracker.type}]"
        }
        val headers = bpHeaders + trackerHeaders

        // Group by date and build rows
        val groupedByDate = filteredMeasurements.groupBy { it.date }

        // Sort dates ascending for export
        val sortedDates = groupedByDate.keys.sorted()
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        val rows = mutableListOf<List<String>>()
        sortedDates.forEach { date ->
            val dailyMeasurements = groupedByDate[date] ?: emptyList()
            val dailyMeasurementIds = dailyMeasurements.map { it.id }.toSet()

            val rowValues = mutableListOf<String>()
            rowValues.add(date)

            // Scheduled slots
            activeSlotsMap.forEach { (originalIndex, _) ->
                val measurement = dailyMeasurements.find { it.slotIndex == originalIndex }
                val cellValue = measurement?.let {
                    if (it.pulse > 0) "${it.systolic}/${it.diastolic}@${it.pulse}"
                    else "${it.systolic}/${it.diastolic}"
                } ?: ""
                rowValues.add(cellValue)
            }

            // Anytime slots
            val anytimeReadings = dailyMeasurements.filter { it.slotIndex == -1 }
                .sortedBy { it.timestamp }
                .map { reading ->
                    val bp = if (reading.pulse > 0) "${reading.systolic}/${reading.diastolic}@${reading.pulse}"
                             else "${reading.systolic}/${reading.diastolic}"
                    
                    // Time is now mandatory for Anytime readings. Use timestamp if > 0, else fallback to createdAt.
                    val effectiveTimestamp = if (reading.timestamp > 0) reading.timestamp else reading.createdAt
                    val localTime = java.time.Instant.ofEpochMilli(effectiveTimestamp)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                    val time = " (${localTime.format(timeFormatter)})"
                    
                    "$bp$time"
                }
            rowValues.add(anytimeReadings.joinToString("; "))

            // Custom Trackers
            activeTrackerDefinitions.forEach { tracker ->
                val trackerValues = allTrackerValues.filter { 
                    it.trackerId == tracker.id && it.measurementId in dailyMeasurementIds 
                }.sortedBy { it.id } // Stable order, though ideally we'd have timestamps on tracker values too

                val cellValue = trackerValues.joinToString("; ") { value ->
                    val displayValue = value.floatValue?.toString() 
                        ?: value.booleanValue?.let { if (it) "True" else "False" }
                        ?: value.stringValue ?: ""
                    
                    val measurement = dailyMeasurements.find { it.id == value.measurementId }
                    val timePart = measurement?.let { m ->
                        val effectiveTimestamp = if (m.timestamp > 0) m.timestamp else m.createdAt
                        val localTime = java.time.Instant.ofEpochMilli(effectiveTimestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalTime()
                        " (${localTime.format(timeFormatter)})"
                    } ?: ""
                    
                    "$displayValue$timePart"
                }
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


