package com.otakeessen.underpressure.data.repository

import com.otakeessen.underpressure.data.local.dao.TrackerDao
import com.otakeessen.underpressure.data.local.entities.TrackerDefinitionEntity
import com.otakeessen.underpressure.data.local.entities.TrackerValueEntity
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.domain.repository.TrackerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-based implementation of [TrackerRepository].
 */
class TrackerRepositoryImpl(
    private val trackerDao: TrackerDao
) : TrackerRepository {

    override fun getAllTrackerDefinitions(): Flow<List<TrackerDefinition>> {
        return trackerDao.getAllTrackerDefinitions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getActiveTrackerDefinitions(): Flow<List<TrackerDefinition>> {
        return trackerDao.getActiveTrackerDefinitions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getTrackerDefinitionById(id: Long): TrackerDefinition? {
        return trackerDao.getTrackerDefinitionById(id)?.toDomain()
    }

    override suspend fun saveTrackerDefinition(tracker: TrackerDefinition): Long {
        return trackerDao.insertTrackerDefinition(tracker.toEntity())
    }

    override suspend fun deleteTrackerDefinition(tracker: TrackerDefinition) {
        trackerDao.deleteTrackerDefinition(tracker.toEntity())
    }

    override fun getTrackerValuesByMeasurementId(measurementId: Long): Flow<List<TrackerValue>> {
        return trackerDao.getTrackerValuesByMeasurementId(measurementId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllTrackerValues(): Flow<List<TrackerValue>> {
        return trackerDao.getAllTrackerValues().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun saveTrackerValue(value: TrackerValue): Long {
        return trackerDao.insertTrackerValue(value.toEntity())
    }

    override suspend fun deleteTrackerValuesForMeasurement(measurementId: Long) {
        trackerDao.deleteTrackerValuesForMeasurement(measurementId)
    }

    override suspend fun getTrackerValueByMeasurementAndTracker(
        measurementId: Long,
        trackerId: Long
    ): TrackerValue? {
        return trackerDao.getTrackerValueByMeasurementAndTracker(measurementId, trackerId)?.toDomain()
    }

    // --- Mapper Extensions ---

    private fun TrackerDefinitionEntity.toDomain() = TrackerDefinition(
        id = id,
        name = name,
        type = type,
        unit = unit,
        isActive = isActive,
        showOnChart = showOnChart,
        useSecondaryAxis = useSecondaryAxis
    )

    private fun TrackerDefinition.toEntity() = TrackerDefinitionEntity(
        id = id,
        name = name,
        type = type,
        unit = unit,
        isActive = isActive,
        showOnChart = showOnChart,
        useSecondaryAxis = useSecondaryAxis
    )

    private fun TrackerValueEntity.toDomain() = TrackerValue(
        id = id,
        measurementId = measurementId,
        trackerId = trackerId,
        floatValue = floatValue,
        booleanValue = booleanValue,
        stringValue = stringValue,
        timestamp = timestamp
    )

    private fun TrackerValue.toEntity() = TrackerValueEntity(
        id = id,
        measurementId = measurementId,
        trackerId = trackerId,
        floatValue = floatValue,
        booleanValue = booleanValue,
        stringValue = stringValue,
        timestamp = timestamp
    )
}
