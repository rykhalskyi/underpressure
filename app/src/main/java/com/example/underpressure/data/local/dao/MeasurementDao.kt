package com.example.underpressure.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.underpressure.data.local.entities.MeasurementEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for blood pressure measurements.
 */
@Dao
interface MeasurementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: MeasurementEntity): Long

    @Update
    suspend fun update(measurement: MeasurementEntity)

    @Delete
    suspend fun delete(measurement: MeasurementEntity)

    @Query("SELECT * FROM measurements WHERE date = :date ORDER BY createdAt DESC")
    fun getByDate(date: String): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE date = :date ORDER BY createdAt DESC")
    suspend fun getByDateSync(date: String): List<MeasurementEntity>

    @Query("SELECT * FROM measurements ORDER BY date DESC, createdAt DESC")
    fun getAll(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE systolic = :value OR diastolic = :value OR pulse = :value")
    fun getByValue(value: Int): Flow<List<MeasurementEntity>>
}
