package com.example.underpressure.ui.measurements

import com.example.underpressure.data.local.entities.MeasurementListEntity
import com.example.underpressure.data.local.entities.MeasurementListType
import com.example.underpressure.domain.repository.GenericMeasurementRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MeasurementListViewModelTest {

    private val repository: GenericMeasurementRepository = mockk()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: MeasurementListViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { repository.getAllLists() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState emits lists from repository`() = runTest {
        val lists = listOf(
            MeasurementListEntity(id = 1, name = "Weight", type = MeasurementListType.DOUBLE)
        )
        coEvery { repository.getAllLists() } returns flowOf(lists)
        
        viewModel = MeasurementListViewModel(repository)
        
        val state = viewModel.uiState.first()
        assertEquals(lists, state.lists)
        assertFalse(state.isLoading)
    }

    @Test
    fun `addList calls repository`() = runTest {
        coEvery { repository.saveList(any()) } returns 1L
        viewModel = MeasurementListViewModel(repository)

        viewModel.addList("Sugar", MeasurementListType.DOUBLE)

        coVerify { repository.saveList(match { it.name == "Sugar" && it.type == MeasurementListType.DOUBLE }) }
    }

    @Test
    fun `toggleListActive updates repository`() = runTest {
        val list = MeasurementListEntity(id = 1, name = "Weight", type = MeasurementListType.DOUBLE, active = true)
        coEvery { repository.saveList(any()) } returns 1L
        viewModel = MeasurementListViewModel(repository)

        viewModel.toggleListActive(list)

        coVerify { repository.saveList(match { it.id == 1L && !it.active }) }
    }

    @Test
    fun `deleteList calls repository`() = runTest {
        val list = MeasurementListEntity(id = 1, name = "Weight", type = MeasurementListType.DOUBLE)
        coEvery { repository.deleteList(any()) } returns Unit
        viewModel = MeasurementListViewModel(repository)

        viewModel.deleteList(list)

        coVerify { repository.deleteList(list) }
    }
}
