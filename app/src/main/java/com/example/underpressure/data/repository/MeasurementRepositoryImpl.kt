package com.example.underpressure.data.repository

import com.example.underpressure.data.local.dao.MeasurementDao
import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
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

    override fun getMeasurementsByValue(value: Int): Flow<List<MeasurementEntity>> {
        return measurementDao.getByValue(value)
    }
}
