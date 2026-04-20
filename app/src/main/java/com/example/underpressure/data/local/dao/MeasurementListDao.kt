package com.example.underpressure.data.local.dao

import androidx.room.*
import com.example.underpressure.data.local.entities.MeasurementListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementListDao {
    @Query("SELECT * FROM measurement_lists ORDER BY id ASC")
    fun getAllLists(): Flow<List<MeasurementListEntity>>

    @Query("SELECT * FROM measurement_lists WHERE active = 1")
    fun getActiveLists(): Flow<List<MeasurementListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: MeasurementListEntity): Long

    @Update
    suspend fun updateList(list: MeasurementListEntity)

    @Delete
    suspend fun deleteList(list: MeasurementListEntity)
}
