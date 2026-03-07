package com.example.underpressure.data.repository

import com.example.underpressure.data.local.dao.MeasurementDao
import com.example.underpressure.data.local.entities.MeasurementEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class MeasurementRepositoryImplTest {

    private lateinit var measurementDao: MeasurementDao
    private lateinit var repository: MeasurementRepositoryImpl

    @Before
    fun setUp() {
        measurementDao = mockk()
        repository = MeasurementRepositoryImpl(measurementDao)
    }

    @Test
    fun `saveMeasurement calls dao insert`() = runTest {
        val measurement = MeasurementEntity(date = "2024-03-01", slotIndex = 1, systolic = 120, diastolic = 80, pulse = 70)
        coEvery { measurementDao.insert(measurement) } returns 1L

        val result = repository.saveMeasurement(measurement)

        assertEquals(1L, result)
        coVerify(exactly = 1) { measurementDao.insert(measurement) }
    }

    @Test
    fun `updateMeasurement calls dao update`() = runTest {
        val measurement = MeasurementEntity(id = 1, date = "2024-03-01", slotIndex = 1, systolic = 125, diastolic = 85, pulse = 75)
        coEvery { measurementDao.update(measurement) } returns Unit

        repository.updateMeasurement(measurement)

        coVerify(exactly = 1) { measurementDao.update(measurement) }
    }

    @Test
    fun `deleteMeasurement calls dao delete`() = runTest {
        val measurement = MeasurementEntity(id = 1, date = "2024-03-01", slotIndex = 1, systolic = 120, diastolic = 80, pulse = 70)
        coEvery { measurementDao.delete(measurement) } returns Unit

        repository.deleteMeasurement(measurement)

        coVerify(exactly = 1) { measurementDao.delete(measurement) }
    }

    @Test
    fun `getMeasurementsByDate calls dao getByDate`() = runTest {
        val date = "2024-03-01"
        val measurements = listOf(MeasurementEntity(id = 1, date = date, slotIndex = 1, systolic = 120, diastolic = 80, pulse = 70))
        every { measurementDao.getByDate(date) } returns flowOf(measurements)

        repository.getMeasurementsByDate(date).collect {
            assertEquals(measurements, it)
        }

        verify(exactly = 1) { measurementDao.getByDate(date) }
    }

    @Test
    fun `getAllMeasurements calls dao getAll`() = runTest {
        val measurements = listOf(MeasurementEntity(id = 1, date = "2024-03-01", slotIndex = 1, systolic = 120, diastolic = 80, pulse = 70))
        every { measurementDao.getAll() } returns flowOf(measurements)

        repository.getAllMeasurements().collect {
            assertEquals(measurements, it)
        }

        verify(exactly = 1) { measurementDao.getAll() }
    }

    @Test
    fun `getMeasurementsByValue calls dao getByValue`() = runTest {
        val value = 120
        val measurements = listOf(MeasurementEntity(id = 1, date = "2024-03-01", slotIndex = 1, systolic = 120, diastolic = 80, pulse = 70))
        every { measurementDao.getByValue(value) } returns flowOf(measurements)

        repository.getMeasurementsByValue(value).collect {
            assertEquals(measurements, it)
        }

        verify(exactly = 1) { measurementDao.getByValue(value) }
    }
}
