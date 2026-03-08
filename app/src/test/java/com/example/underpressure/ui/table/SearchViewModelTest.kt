package com.example.underpressure.ui.table

import com.example.underpressure.data.local.entities.MeasurementEntity
import com.example.underpressure.domain.repository.MeasurementRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @Mock
    private lateinit var repository: MeasurementRepository
    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `numeric search updates results`() = runTest {
        val query = "120"
        val mockResults = listOf(
            MeasurementEntity(1, "2024-03-01", 0, 120, 80, 60, 0)
        )
        `when`(repository.searchMeasurements(query)).thenReturn(flowOf(mockResults))

        viewModel.updateQuery(query)
        advanceTimeBy(400) // Debounce

        val state = viewModel.uiState.value
        assertEquals(query, state.query)
        assertEquals(mockResults, state.results)
        assertFalse(state.isLoading)
        assertFalse(state.isNoResults)
    }

    @Test
    fun `invalid date format shows error`() = runTest {
        val query = "2024-03"
        viewModel.updateQuery(query)
        advanceTimeBy(400) // Debounce

        val state = viewModel.uiState.value
        assertEquals(query, state.query)
        assertNotNull(state.dateError)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `valid date format shows no error`() = runTest {
        val query = "2024-03-01"
        viewModel.updateQuery(query)
        advanceTimeBy(400) // Debounce

        val state = viewModel.uiState.value
        assertEquals(query, state.query)
        assertEquals(null, state.dateError)
    }
}
