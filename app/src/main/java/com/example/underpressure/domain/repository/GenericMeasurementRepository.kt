package com.example.underpressure.domain.repository

import com.example.underpressure.data.local.entities.MeasurementEntryEntity
import com.example.underpressure.data.local.entities.MeasurementListEntity
import kotlinx.coroutines.flow.Flow

interface GenericMeasurementRepository {
    fun getAllLists(): Flow<List<MeasurementListEntity>>
    fun getActiveLists(): Flow<List<MeasurementListEntity>>
    suspend fun saveList(list: MeasurementListEntity): Long
    suspend fun deleteList(list: MeasurementListEntity)
    
    fun getEntriesForSlot(date: String, slotIndex: Int): Flow<List<MeasurementEntryEntity>>
    fun getEntriesForDate(date: String): Flow<List<MeasurementEntryEntity>>
    fun getAllEntries(): Flow<List<MeasurementEntryEntity>>
    suspend fun saveEntry(entry: MeasurementEntryEntity)
    suspend fun deleteEntry(entry: MeasurementEntryEntity)
}
