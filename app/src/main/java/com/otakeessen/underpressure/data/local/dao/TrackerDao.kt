package com.otakeessen.underpressure.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.otakeessen.underpressure.data.local.entities.TrackerDefinitionEntity
import com.otakeessen.underpressure.data.local.entities.TrackerValueEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for tracker definitions and values.
 */
@Dao
interface TrackerDao {

    // --- Tracker Definitions ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackerDefinition(tracker: TrackerDefinitionEntity): Long

    @Update
    suspend fun updateTrackerDefinition(tracker: TrackerDefinitionEntity)

    @Delete
    suspend fun deleteTrackerDefinition(tracker: TrackerDefinitionEntity)

    @Query("DELETE FROM tracker_definitions")
    suspend fun deleteAllTrackerDefinitions()

    @Query("SELECT * FROM tracker_definitions")
    fun getAllTrackerDefinitions(): Flow<List<TrackerDefinitionEntity>>

    @Query("SELECT * FROM tracker_definitions WHERE isActive = 1")
    fun getActiveTrackerDefinitions(): Flow<List<TrackerDefinitionEntity>>

    @Query("SELECT * FROM tracker_definitions WHERE id = :id")
    suspend fun getTrackerDefinitionById(id: Long): TrackerDefinitionEntity?

    // --- Tracker Values ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackerValue(value: TrackerValueEntity): Long

    @Update
    suspend fun updateTrackerValue(value: TrackerValueEntity)

    @Query("DELETE FROM tracker_values WHERE measurementId = :measurementId")
    suspend fun deleteTrackerValuesForMeasurement(measurementId: Long)

    @Query("SELECT * FROM tracker_values WHERE measurementId = :measurementId")
    fun getTrackerValuesByMeasurementId(measurementId: Long): Flow<List<TrackerValueEntity>>

    @Query("SELECT * FROM tracker_values WHERE trackerId = :trackerId")
    fun getTrackerValuesByTrackerId(trackerId: Long): Flow<List<TrackerValueEntity>>
    
    @Query("SELECT * FROM tracker_values")
    fun getAllTrackerValues(): Flow<List<TrackerValueEntity>>

    @Query("SELECT * FROM tracker_values WHERE measurementId = :measurementId AND trackerId = :trackerId")
    suspend fun getTrackerValueByMeasurementAndTracker(measurementId: Long, trackerId: Long): TrackerValueEntity?
}
