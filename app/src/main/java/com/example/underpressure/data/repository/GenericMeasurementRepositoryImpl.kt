package com.example.underpressure.data.repository

import com.example.underpressure.data.local.dao.MeasurementEntryDao
import com.example.underpressure.data.local.dao.MeasurementListDao
import com.example.underpressure.data.local.entities.MeasurementEntryEntity
import com.example.underpressure.data.local.entities.MeasurementListEntity
import com.example.underpressure.domain.repository.GenericMeasurementRepository
import kotlinx.coroutines.flow.Flow

class GenericMeasurementRepositoryImpl(
    private val listDao: MeasurementListDao,
    private val entryDao: MeasurementEntryDao
) : GenericMeasurementRepository {

    override fun getAllLists(): Flow<List<MeasurementListEntity>> = listDao.getAllLists()

    override fun getActiveLists(): Flow<List<MeasurementListEntity>> = listDao.getActiveLists()

    override suspend fun saveList(list: MeasurementListEntity): Long {
        return if (list.id == 0L) {
            listDao.insertList(list)
        } else {
            listDao.updateList(list)
            list.id
        }
    }

    override suspend fun deleteList(list: MeasurementListEntity) = listDao.deleteList(list)

    override fun getEntriesForSlot(date: String, slotIndex: Int): Flow<List<MeasurementEntryEntity>> {
        return entryDao.getEntriesForSlot(date, slotIndex)
    }

    override fun getEntriesForDate(date: String): Flow<List<MeasurementEntryEntity>> {
        return entryDao.getEntriesForDate(date)
    }

    override fun getAllEntries(): Flow<List<MeasurementEntryEntity>> = entryDao.getAllEntries()

    override suspend fun saveEntry(entry: MeasurementEntryEntity) {
        entryDao.insertEntry(entry)
    }

    override suspend fun deleteEntry(entry: MeasurementEntryEntity) {
        entryDao.deleteEntry(entry)
    }
}
