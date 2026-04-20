package com.example.underpressure.ui.measurements

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.underpressure.data.local.entities.MeasurementListEntity
import com.example.underpressure.data.local.entities.MeasurementListType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MeasurementListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MeasurementListViewModel
    private val uiStateFlow = MutableStateFlow(MeasurementListUiState())

    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
        every { viewModel.uiState } returns uiStateFlow
    }

    @Test
    fun addList_showsDialog_andTriggersAdd() {
        uiStateFlow.value = MeasurementListUiState(isLoading = false)

        composeTestRule.setContent {
            MeasurementListScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        // Click FAB
        composeTestRule.onNodeWithTag("add_list_fab").performClick()

        // Verify dialog shows
        composeTestRule.onNodeWithText("Add Measurement List").assertIsDisplayed()

        // Fill name
        composeTestRule.onNodeWithTag("list_name_input").performTextInput("Weight")

        // Select type DOUBLE (already default but let's click to be sure)
        composeTestRule.onNodeWithText("DOUBLE").performClick()

        // Confirm
        composeTestRule.onNodeWithTag("confirm_add_list").performClick()

        verify { viewModel.addList("Weight", MeasurementListType.DOUBLE) }
    }

    @Test
    fun listItems_areDisplayed() {
        val lists = listOf(
            MeasurementListEntity(id = 1, name = "Weight", type = MeasurementListType.DOUBLE),
            MeasurementListEntity(id = 2, name = "Meds", type = MeasurementListType.BOOLEAN, active = false)
        )
        uiStateFlow.value = MeasurementListUiState(lists = lists, isLoading = false)

        composeTestRule.setContent {
            MeasurementListScreen(
                viewModel = viewModel,
                onBack = {}
            )
        }

        composeTestRule.onNodeWithText("Weight").assertIsDisplayed()
        composeTestRule.onNodeWithText("DOUBLE").assertIsDisplayed()
        composeTestRule.onNodeWithText("Meds").assertIsDisplayed()
        composeTestRule.onNodeWithText("BOOLEAN").assertIsDisplayed()
    }
}
