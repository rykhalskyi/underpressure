package com.example.underpressure.ui.table

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MeasurementTableScreenTest {

    @getRule
    @JvmField
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MeasurementTableViewModel
    private val uiStateFlow = MutableStateFlow(TableUiState(isLoading = true))

    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
        every { viewModel.uiState } returns uiStateFlow
    }

    @Test
    fun tableHeaders_areDisplayed() {
        uiStateFlow.value = TableUiState(
            isLoading = false, 
            slotHeaders = listOf("Morning", "Evening"),
            items = emptyList()
        )
        
        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Date").assertIsDisplayed()
        composeTestRule.onNodeWithText("Morning").assertIsDisplayed()
        composeTestRule.onNodeWithText("Evening").assertIsDisplayed()
    }

    @Test
    fun measurementRow_isDisplayed() {
        val date = "2026-03-07"
        uiStateFlow.value = TableUiState(
            isLoading = false,
            slotHeaders = listOf("Slot 1"),
            items = listOf(
                DayMeasurementSummary(
                    date = date, 
                    slots = mapOf(0 to SlotData(120, 80, 70))
                )
            )
        )

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText(date).assertIsDisplayed()
        composeTestRule.onNodeWithText("120/80/70").assertIsDisplayed()
    }

    @Test
    fun cellClick_triggersViewModel() {
        val date = "2026-03-07"
        uiStateFlow.value = TableUiState(
            isLoading = false,
            slotHeaders = listOf("Slot 1"),
            items = listOf(
                DayMeasurementSummary(date = date, slots = emptyMap())
            )
        )

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                onSettingsClick = {}
            )
        }

        // Click on the empty cell (represented by "-")
        composeTestRule.onNodeWithText("-").performClick()

        verify { viewModel.onCellClicked(date, 0) }
    }

    @Test
    fun dialog_isDisplayed_whenOpen() {
        uiStateFlow.value = TableUiState(
            isLoading = false,
            dialogState = MeasurementDialogState(
                isOpen = true,
                date = "2026-03-07",
                slotIndex = 0,
                initialValue = ""
            )
        )

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                onSettingsClick = {}
            )
        }

        composeTestRule.onNodeWithText("Add Measurement").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-03-07 - Slot 1").assertIsDisplayed()
    }

    @Test
    fun dialogSave_callsViewModel_whenValid() {
        uiStateFlow.value = TableUiState(
            isLoading = false,
            dialogState = MeasurementDialogState(
                isOpen = true,
                date = "2026-03-07",
                slotIndex = 0
            )
        )

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                onSettingsClick = {}
            )
        }

        val input = "120/80 @72"
        composeTestRule.onNodeWithText("SYS/DIA @PULSE").performTextInput(input)
        
        composeTestRule.onNodeWithText("Save").assertIsEnabled().performClick()

        verify { viewModel.onSaveMeasurement(input) }
    }
}
