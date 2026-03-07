package com.example.underpressure.ui.table

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.underpressure.domain.repository.MeasurementRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MeasurementTableScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var repository: MeasurementRepository
    private lateinit var viewModel: MeasurementTableViewModel
    private val uiStateFlow = MutableStateFlow(TableUiState(isLoading = true))

    @Before
    fun setUp() {
        repository = mockk()
        every { repository.getAllMeasurements() } returns MutableStateFlow(emptyList())
        viewModel = mockk()
        every { viewModel.uiState } returns uiStateFlow
    }

    @Test
    fun tableHeaders_areDisplayed() {
        uiStateFlow.value = TableUiState(isLoading = false, items = emptyList())
        
        composeTestRule.setContent {
            MeasurementTableScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText("Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sys").assertIsDisplayed()
        composeTestRule.onNodeWithText("Dia").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pulse").assertIsDisplayed()
    }

    @Test
    fun measurementRow_isDisplayed() {
        val date = "2024-03-01"
        uiStateFlow.value = TableUiState(
            isLoading = false,
            items = listOf(
                DayMeasurementSummary(date = date, systolic = 120, diastolic = 80, pulse = 70)
            )
        )

        composeTestRule.setContent {
            MeasurementTableScreen(viewModel = viewModel)
        }

        composeTestRule.onNodeWithText(date).assertIsDisplayed()
        composeTestRule.onNodeWithText("120").assertIsDisplayed()
    }

    @Test
    fun loadingIndicator_isDisplayed_whenLoading() {
        uiStateFlow.value = TableUiState(isLoading = true)

        composeTestRule.setContent {
            MeasurementTableScreen(viewModel = viewModel)
        }

        // CircularProgressIndicator doesn't have text, but we can check it exists by its semantics if needed
        // For simplicity, we verify that headers are NOT displayed yet if we handle it that way
        composeTestRule.onNodeWithText("Date").assertDoesNotExist()
    }
}
