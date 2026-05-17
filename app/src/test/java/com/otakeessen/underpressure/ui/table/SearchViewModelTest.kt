package com.otakeessen.underpressure.ui.table

import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private lateinit var repository: MeasurementRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        repository = mockk()
        settingsRepository = mockk()
        every { settingsRepository.getSettings() } returns flowOf(null) // Default to null settings (defaults to ESC_ESH)
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(repository, settingsRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        val state = viewModel.resultsState.value
        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isLoading)
    }

    @Test
    fun `numeric search updates results and clears filter`() = runTest {
        viewModel.setFilter(SearchFilter.NORMAL)
        
        val query = "120"
        val mockResults = listOf(
            MeasurementEntity(1, "2024-03-01", 0, 120, 80, 60)
        )
        every { repository.searchMeasurements(query) } returns flowOf(mockResults)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.resultsState.collect { }
        }

        viewModel.updateQuery(query)
        
        // Check filter cleared immediately
        assertEquals(SearchFilter.NONE, viewModel.filter.value)
        
        advanceTimeBy(1000) // Debounce (300ms) + buffer

        val state = viewModel.resultsState.value
        assertEquals(query, state.query)
        assertEquals(mockResults, state.results)
        assertFalse(state.isLoading)
        assertFalse(state.isNoResults)
    }

    @Test
    fun `date search does not clear filter`() = runTest {
        viewModel.setFilter(SearchFilter.NORMAL)
        
        val query = "2024-03"
        every { repository.searchMeasurementsByDate(query) } returns flowOf(emptyList())

        viewModel.updateQuery(query)
        
        assertEquals(SearchFilter.NORMAL, viewModel.filter.value)
    }

    @Test
    fun `invalid date format shows error`() = runTest {
        val query = "2024-13" // Invalid month
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.resultsState.collect { }
        }

        viewModel.updateQuery(query)
        advanceTimeBy(1000) // Debounce

        val state = viewModel.resultsState.value
        assertEquals(query, state.query)
        assertNotNull(state.dateErrorRes)
        assertTrue(state.results.isEmpty())
    }

    @Test
    fun `filter only search fetches all measurements`() = runTest {
        val mockResults = listOf(
            MeasurementEntity(1, "2024-03-01", 0, 110, 70, 60), // Normal
            MeasurementEntity(2, "2024-03-02", 0, 150, 95, 60)  // Stage 1
        )
        every { repository.getAllMeasurements() } returns flowOf(mockResults)

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.resultsState.collect { }
        }

        viewModel.setFilter(SearchFilter.NORMAL)
        advanceTimeBy(1000)

        val state = viewModel.resultsState.value
        assertEquals(1, state.results.size)
        assertEquals(110, state.results[0].systolic)
    }

    @Test
    fun `filter can be toggled`() = runTest {
        viewModel.setFilter(SearchFilter.NORMAL)
        assertEquals(SearchFilter.NORMAL, viewModel.filter.value)
        
        viewModel.setFilter(SearchFilter.NORMAL)
        assertEquals(SearchFilter.NONE, viewModel.filter.value)
    }
}
