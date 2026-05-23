package com.otakeessen.underpressure.domain.repository

import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerValue
import kotlinx.coroutines.flow.Flow

/**
 * Interface for tracker-related data operations.
 */
interface TrackerRepository {

    // --- Tracker Definitions ---

    fun getAllTrackerDefinitions(): Flow<List<TrackerDefinition>>

    fun getActiveTrackerDefinitions(): Flow<List<TrackerDefinition>>

    suspend fun getTrackerDefinitionById(id: Long): TrackerDefinition?

    suspend fun saveTrackerDefinition(tracker: TrackerDefinition): Long

    suspend fun deleteTrackerDefinition(tracker: TrackerDefinition)

    // --- Tracker Values ---

    fun getTrackerValuesByMeasurementId(measurementId: Long): Flow<List<TrackerValue>>
    
    fun getAllTrackerValues(): Flow<List<TrackerValue>>

    suspend fun saveTrackerValue(value: TrackerValue): Long

    suspend fun deleteTrackerValuesForMeasurement(measurementId: Long)
    
    suspend fun getTrackerValueByMeasurementAndTracker(measurementId: Long, trackerId: Long): TrackerValue?
}
