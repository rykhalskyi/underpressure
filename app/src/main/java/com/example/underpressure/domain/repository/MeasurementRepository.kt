package com.example.underpressure.domain.repository

import com.example.underpressure.data.local.entities.MeasurementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Interface for blood pressure measurement data operations.
 */
interface MeasurementRepository {
    /**
     * Saves a new blood pressure measurement.
     * @return The ID of the saved measurement.
     */
    suspend fun saveMeasurement(measurement: MeasurementEntity): Long

    /**
     * Updates an existing blood pressure measurement.
     */
    suspend fun updateMeasurement(measurement: MeasurementEntity)

    /**
     * Deletes a blood pressure measurement.
     */
    suspend fun deleteMeasurement(measurement: MeasurementEntity)

    /**
     * Retrieves measurements for a specific date.
     * @param date The date in YYYY-MM-DD format.
     */
    fun getMeasurementsByDate(date: String): Flow<List<MeasurementEntity>>

    /**
     * Retrieves measurements for a specific date (one-shot).
     * @param date The date in YYYY-MM-DD format.
     */
    suspend fun getMeasurementsByDateSync(date: String): List<MeasurementEntity>

    /**
     * Retrieves all recorded blood pressure measurements.
     */
    fun getAllMeasurements(): Flow<List<MeasurementEntity>>

    /**
     * Retrieves all recorded blood pressure measurements (one-shot).
     */
    suspend fun getAllMeasurementsSync(): List<MeasurementEntity>

    /**
     * Searches for measurements by partial numeric value matches.
     */
    fun searchMeasurements(query: String): Flow<List<MeasurementEntity>>

    /**
     * Retrieves measurements where any value (systolic, diastolic, or pulse) matches the given value.
     */
    fun getMeasurementsByValue(value: Int): Flow<List<MeasurementEntity>>

    /**
     * Retrieves the earliest measurement date in the database.
     */
    suspend fun getMinDate(): String?

    /**
     * Retrieves the latest measurement date in the database.
     */
    suspend fun getMaxDate(): String?
}
