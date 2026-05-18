package com.otakeessen.underpressure.data.repository

import com.otakeessen.underpressure.data.local.dao.MeasurementDao
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import kotlinx.coroutines.flow.Flow

/**
 * Room-based implementation of [MeasurementRepository].
 *
 * @property measurementDao Data Access Object for blood pressure measurements.
 */
class MeasurementRepositoryImpl(
    private val measurementDao: MeasurementDao
) : MeasurementRepository {

    override suspend fun saveMeasurement(measurement: MeasurementEntity): Long {
        return measurementDao.insert(measurement)
    }

    override suspend fun updateMeasurement(measurement: MeasurementEntity) {
        measurementDao.update(measurement)
    }

    override suspend fun deleteMeasurement(measurement: MeasurementEntity) {
        measurementDao.delete(measurement)
    }

    override fun getMeasurementsByDate(date: String): Flow<List<MeasurementEntity>> {
        return measurementDao.getByDate(date)
    }

    override suspend fun getMeasurementsByDateSync(date: String): List<MeasurementEntity> {
        return measurementDao.getByDateSync(date)
    }

    override fun getAllMeasurements(): Flow<List<MeasurementEntity>> {
        return measurementDao.getAll()
    }

    override suspend fun getAllMeasurementsSync(): List<MeasurementEntity> {
        return measurementDao.getAllSync()
    }

    override suspend fun getMeasurementByIdSync(id: Long): MeasurementEntity? {
        return measurementDao.getByIdSync(id)
    }

    override fun searchMeasurementsComplex(digits: List<String>): Flow<List<MeasurementEntity>> {
        val s = digits.getOrNull(0)?.let { "%$it%" } ?: "%"
        val d = digits.getOrNull(1)?.let { "%$it%" } ?: "%"
        val p = digits.getOrNull(2)?.let { "%$it%" } ?: "%"
        
        // If s is "%", it means digits was empty, should be handled by caller but safe here
        return measurementDao.searchByComplexValue(s, d, p)
    }

    override fun searchMeasurementsByDate(dateQuery: String): Flow<List<MeasurementEntity>> {
        val formattedQuery = "%$dateQuery%"
        return measurementDao.searchByDate(formattedQuery)
    }

    override fun searchMeasurements(query: String): Flow<List<MeasurementEntity>> {
        val formattedQuery = "%$query%"
        return measurementDao.searchByValue(formattedQuery)
    }

    override fun getMeasurementsByValue(value: Int): Flow<List<MeasurementEntity>> {
        return measurementDao.getByValue(value)
    }

    override suspend fun getMinDate(): String? {
        return measurementDao.getMinDate()
    }

    override suspend fun getMaxDate(): String? {
        return measurementDao.getMaxDate()
    }
}

