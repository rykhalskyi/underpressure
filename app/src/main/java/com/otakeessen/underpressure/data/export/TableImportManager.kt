package com.otakeessen.underpressure.data.export

import android.content.Context
import android.net.Uri
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.domain.export.DiscoveredTracker
import com.otakeessen.underpressure.domain.export.TrackerDiscoveryResult
import com.otakeessen.underpressure.domain.export.TrackerMappingAction
import com.otakeessen.underpressure.domain.export.TrackerMatchStatus
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
    private val trackerRepository: TrackerRepository,
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val trackerPrefix = "[Tracker] "

    sealed class ImportStrategy {
        object Overwrite : ImportStrategy()
        object Skip : ImportStrategy()
    }

    data class ImportResult(
        val successCount: Int,
        val totalCount: Int,
        val trackerValuesCount: Int = 0,
        val error: String? = null
    )

    /**
     * Scans the CSV header to identify custom trackers and match them with existing definitions.
     */
    suspend fun discoverTrackers(uri: Uri): TrackerDiscoveryResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext TrackerDiscoveryResult(emptyList(), 0)
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine() ?: return@withContext TrackerDiscoveryResult(emptyList(), 0)
            
            val headers = headerLine.split(",")
            val allTrackers = trackerRepository.getAllTrackerDefinitions().first()
            
            val discovered = headers.mapIndexedNotNull { index, header ->
                val trimmedHeader = header.trim()
                if (trimmedHeader.startsWith(trackerPrefix)) {
                    val content = trimmedHeader.removePrefix(trackerPrefix)
                    val (name, unit) = parseTrackerHeader(content)
                    
                    val existing = allTrackers.find { it.name.equals(name, ignoreCase = true) }
                    val status = when {
                        existing == null -> TrackerMatchStatus.NEW
                        existing.unit == unit -> TrackerMatchStatus.EXACT_MATCH
                        else -> TrackerMatchStatus.CONFLICT
                    }
                    
                    DiscoveredTracker(
                        columnIndex = index,
                        headerName = trimmedHeader,
                        extractedName = name,
                        unit = unit,
                        matchStatus = status,
                        existingDefinition = existing
                    )
                } else null
            }
            
            TrackerDiscoveryResult(discovered, headers.size)
        } catch (e: Exception) {
            TrackerDiscoveryResult(emptyList(), 0)
        }
    }

    private fun parseTrackerHeader(content: String): Pair<String, String?> {
        // Format: "Name (Unit)" or just "Name"
        return if (content.contains(" (") && content.endsWith(")")) {
            val name = content.substringBeforeLast(" (").trim()
            val unit = content.substringAfterLast(" (").removeSuffix(")").trim()
            name to unit
        } else {
            content.trim() to null
        }
    }

    /**
     * Imports measurements and custom tracker data from a CSV file.
     * 
     * @param uri The URI of the CSV file.
     * @param strategy The strategy to use when a measurement for the same date and slot already exists.
     * @param trackerMapping Mapping actions for custom tracker columns identified during discovery.
     * @return Result of the import operation.
     */
    suspend fun importCsv(
        uri: Uri, 
        strategy: ImportStrategy,
        trackerMapping: Map<String, TrackerMappingAction> = emptyMap()
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return@withContext ImportResult(0, 0, error = "Could not open file")
            
            val reader = BufferedReader(InputStreamReader(inputStream))
            val headerLine = reader.readLine() ?: return@withContext ImportResult(0, 0, error = "File is empty")
            
            val headers = headerLine.split(",")
            if (headers.isEmpty() || headers[0] != "Date") {
                return@withContext ImportResult(0, 0, error = "Invalid CSV format: first column must be 'Date'")
            }

            // Get current settings to map slot times to indices
            val settings = settingsRepository.getSettingsSync() ?: return@withContext ImportResult(0, 0, error = "Could not retrieve settings")
            val slotTimes = settings.slotTimes

            // Map CSV column index to slot index in the app
            val columnToSlotMap = mutableMapOf<Int, Int>()
            var anytimeColumnIndex = -1
            
            // Map CSV column index to Tracker mapping action
            val trackerColumnToMapping = mutableMapOf<Int, TrackerMappingAction>()
            
            for (i in 1 until headers.size) {
                val header = headers[i].trim()
                
                // Trackers
                if (header.startsWith(trackerPrefix)) {
                    trackerMapping[header]?.let { action ->
                        if (action !is TrackerMappingAction.Skip) {
                            trackerColumnToMapping[i] = action
                        }
                    }
                    continue
                }

                // Anytime
                if (header == "Anytime") {
                    anytimeColumnIndex = i
                    continue
                }
                
                // Scheduled Slots
                // 1. Try "Slot X" mapping (X is 1-based index)
                if (header.startsWith("Slot ")) {
                    val slotNum = header.removePrefix("Slot ").takeWhile { it.isDigit() }.toIntOrNull()
                    if (slotNum != null && slotNum in 1..slotTimes.size) {
                        columnToSlotMap[i] = slotNum - 1
                        continue
                    }
                }

                // 2. Fallback to matching by Time (for backward compatibility)
                val slotIndex = slotTimes.indexOf(header)
                if (slotIndex != -1) {
                    columnToSlotMap[i] = slotIndex
                }
            }

            if (columnToSlotMap.isEmpty() && anytimeColumnIndex == -1 && trackerColumnToMapping.isEmpty()) {
                return@withContext ImportResult(0, 0, error = "No matching data columns found in the CSV header")
            }

            var successCount = 0
            var trackerValuesCount = 0
            var totalProcessed = 0
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            
            // Resolve tracker IDs for the session (to avoid re-creating trackers multiple times)
            val resolvedTrackerIds = mutableMapOf<TrackerMappingAction, Long>()

            // Read data rows
            var line: String? = reader.readLine()
            while (line != null) {
                val columns = line.split(",")
                if (columns.isNotEmpty()) {
                    val dateStr = columns[0].trim()
                    try {
                        // Validate date
                        val localDate = LocalDate.parse(dateStr, dateFormatter)
                        
                        // Map of time string (HH:mm) to measurement ID for this row
                        val timeToMeasurementId = mutableMapOf<String, Long>()

                        // Get existing measurements for this date to handle collisions
                        val existingMeasurements = measurementRepository.getMeasurementsByDateSync(dateStr)

                        // 1. Process Scheduled slots
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

                                    val finalId = if (existing == null) {
                                        val newId = measurementRepository.saveMeasurement(entity)
                                        successCount++
                                        newId
                                    } else if (strategy == ImportStrategy.Overwrite) {
                                        measurementRepository.updateMeasurement(entity)
                                        successCount++
                                        existing.id
                                    } else {
                                        existing.id
                                    }
                                    
                                    slotTimes.getOrNull(slotIndex)?.let { timeToMeasurementId[it] = finalId }
                                }
                            }
                        }

                        // 2. Process Anytime slots
                        if (anytimeColumnIndex != -1 && anytimeColumnIndex < columns.size) {
                            val anytimeValue = columns[anytimeColumnIndex].trim()
                            if (anytimeValue.isNotBlank() && anytimeValue != "—") {
                                val readings = anytimeValue.split(";")
                                readings.forEach { reading ->
                                    val trimmedReading = reading.trim()
                                    if (trimmedReading.isNotBlank()) {
                                        val parseResult = parseAnytimeValue(trimmedReading)
                                        val timeStr = parseResult.timeStr
                                        
                                        if (timeStr != null) {
                                            totalProcessed++
                                            val systolic = parseResult.systolic
                                            val diastolic = parseResult.diastolic
                                            val pulse = parseResult.pulse
                                            
                                            val localTime = java.time.LocalTime.parse(timeStr, timeFormatter)
                                            val timestamp = localDate.atTime(localTime)
                                                .atZone(java.time.ZoneId.systemDefault())
                                                .toInstant()
                                                .toEpochMilli()

                                            val existing = existingMeasurements.find { 
                                                it.slotIndex == -1 && it.timestamp == timestamp
                                            }

                                            val entity = MeasurementEntity(
                                                id = existing?.id ?: 0,
                                                date = dateStr,
                                                slotIndex = -1,
                                                systolic = systolic,
                                                diastolic = diastolic,
                                                pulse = pulse,
                                                isFlexible = true,
                                                timestamp = timestamp,
                                                updatedAt = System.currentTimeMillis()
                                            )

                                            val finalId = if (existing == null) {
                                                val newId = measurementRepository.saveMeasurement(entity)
                                                successCount++
                                                newId
                                            } else if (strategy == ImportStrategy.Overwrite) {
                                                measurementRepository.updateMeasurement(entity)
                                                successCount++
                                                existing.id
                                            } else {
                                                existing.id
                                            }
                                            
                                            timeToMeasurementId[timeStr] = finalId
                                        }
                                    }
                                }
                            }
                        }
                        
                        // 3. Process Tracker columns
                        trackerColumnToMapping.forEach { (colIndex, mapping) ->
                            if (colIndex < columns.size) {
                                val cellValue = columns[colIndex].trim()
                                if (cellValue.isNotBlank()) {
                                    val trackerId = resolvedTrackerIds.getOrPut(mapping) {
                                        when (mapping) {
                                            is TrackerMappingAction.MapToExisting -> mapping.trackerId
                                            is TrackerMappingAction.CreateNew -> {
                                                trackerRepository.saveTrackerDefinition(
                                                    TrackerDefinition(
                                                        name = mapping.name,
                                                        type = mapping.type,
                                                        unit = mapping.unit
                                                    )
                                                )
                                            }
                                            TrackerMappingAction.Skip -> -1L
                                        }
                                    }
                                    
                                    if (trackerId != -1L) {
                                        val trackerEntries = cellValue.split(";")
                                        trackerEntries.forEach { entry ->
                                            val (value, time) = parseTrackerCellValue(entry.trim())
                                            
                                            val measurementId = when {
                                                time != null -> timeToMeasurementId[time]
                                                timeToMeasurementId.size == 1 -> timeToMeasurementId.values.first()
                                                else -> null
                                            }
                                            
                                            if (measurementId != null) {
                                                val existingValue = trackerRepository.getTrackerValueByMeasurementAndTracker(measurementId, trackerId)
                                                
                                                val type = trackerRepository.getTrackerDefinitionById(trackerId)?.type
                                                val trackerValue = TrackerValue(
                                                    id = existingValue?.id ?: 0,
                                                    measurementId = measurementId,
                                                    trackerId = trackerId,
                                                    floatValue = if (type == TrackerType.FLOAT) value.toDoubleOrNull() else null,
                                                    booleanValue = if (type == TrackerType.BOOLEAN) value.lowercase() == "true" else null,
                                                    stringValue = if (type == TrackerType.STRING) value else null,
                                                    timestamp = time?.let {
                                                        java.time.LocalTime.parse(it, timeFormatter)
                                                            .atDate(localDate)
                                                            .atZone(java.time.ZoneId.systemDefault())
                                                            .toInstant()
                                                            .toEpochMilli()
                                                    } ?: System.currentTimeMillis()
                                                )

                                                if (existingValue == null) {
                                                    trackerRepository.saveTrackerValue(trackerValue)
                                                    trackerValuesCount++
                                                } else if (strategy == ImportStrategy.Overwrite) {
                                                    trackerRepository.saveTrackerValue(trackerValue)
                                                    trackerValuesCount++
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Skip invalid rows
                    }
                }
                line = reader.readLine()
            }
            
            ImportResult(successCount, totalProcessed, trackerValuesCount)
        } catch (e: Exception) {
            ImportResult(0, 0, error = e.message)
        }
    }

    private fun parseTrackerCellValue(entry: String): Pair<String, String?> {
        // Formats: "75.5 (08:30)" or "75.5"
        return if (entry.contains(" (") && entry.endsWith(")")) {
            val value = entry.substringBeforeLast(" (").trim()
            val time = entry.substringAfterLast(" (").removeSuffix(")").trim()
            value to time
        } else {
            entry to null
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

    private data class AnytimeParseResult(
        val systolic: Int,
        val diastolic: Int,
        val pulse: Int,
        val timeStr: String?
    )

    private fun parseAnytimeValue(value: String): AnytimeParseResult {
        return try {
            var workingValue = value
            var timeStr: String? = null
            
            if (workingValue.contains("(") && workingValue.contains(")")) {
                val timePart = workingValue.substringAfter("(").substringBefore(")")
                timeStr = timePart.trim()
                workingValue = workingValue.substringBefore("(").trim()
                
                val (sys, dia, pulse) = parseMeasurementValue(workingValue)
                AnytimeParseResult(sys, dia, pulse, timeStr)
            } else {
                AnytimeParseResult(0, 0, 0, null)
            }
        } catch (e: Exception) {
            AnytimeParseResult(0, 0, 0, null)
        }
    }
}
