package com.example.underpressure.data.local.dao

import androidx.room.*
import com.example.underpressure.data.local.entities.MeasurementEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementEntryDao {
    @Query("SELECT * FROM measurement_entries WHERE date = :date AND slotIndex = :slotIndex")
    fun getEntriesForSlot(date: String, slotIndex: Int): Flow<List<MeasurementEntryEntity>>

    @Query("SELECT * FROM measurement_entries WHERE date = :date")
    fun getEntriesForDate(date: String): Flow<List<MeasurementEntryEntity>>

    @Query("SELECT * FROM measurement_entries")
    fun getAllEntries(): Flow<List<MeasurementEntryEntity>>

    @Query("SELECT * FROM measurement_entries WHERE listId = :listId")
    fun getEntriesForList(listId: Long): Flow<List<MeasurementEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: MeasurementEntryEntity)

    @Update
    suspend fun updateEntry(entry: MeasurementEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: MeasurementEntryEntity)
}
