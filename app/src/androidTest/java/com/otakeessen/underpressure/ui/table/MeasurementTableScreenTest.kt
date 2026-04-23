package com.otakeessen.underpressure.ui.table

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.otakeessen.underpressure.R
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MeasurementTableScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MeasurementTableViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var shareViewModel: ShareViewModel
    
    private val uiStateFlow = MutableStateFlow(TableUiState(isLoading = true))
    private val searchUiStateFlow = MutableStateFlow(SearchUiState())
    private val searchQueryFlow = MutableStateFlow("")

    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
        searchViewModel = mockk(relaxed = true)
        shareViewModel = mockk(relaxed = true)
        
        every { viewModel.uiState } returns uiStateFlow
        every { viewModel.scrollToDateEvent } returns MutableStateFlow("") // Mocked flow for LaunchedEffect
        every { searchViewModel.uiState } returns searchUiStateFlow
        every { searchViewModel.query } returns searchQueryFlow
        every { shareViewModel.uiState } returns MutableStateFlow(ShareUiState())
        every { shareViewModel.shareEvents } returns MutableStateFlow(ShareViewModel.ShareEvent.ShareText(""))
    }

    @Test
    fun fab_isDisplayed_andTriggersClick() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cd = context.getString(R.string.cd_add_measurement)
        
        uiStateFlow.value = TableUiState(
            isLoading = false,
            isFabEnabled = true
        )

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                searchViewModel = searchViewModel,
                shareViewModel = shareViewModel,
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        composeTestRule.onNodeWithContentDescription(cd).assertIsDisplayed().performClick()

        verify { viewModel.onFabClicked() }
    }

    @Test
    fun tableHeaders_areDisplayed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dateHeader = context.getString(R.string.header_date)
        
        uiStateFlow.value = TableUiState(
            isLoading = false, 
            slotHeaders = listOf("Morning", "Evening"),
            items = emptyList()
        )
        
        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                searchViewModel = searchViewModel,
                shareViewModel = shareViewModel,
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        composeTestRule.onNodeWithText(dateHeader).assertIsDisplayed()
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
                searchViewModel = searchViewModel,
                shareViewModel = shareViewModel,
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        composeTestRule.onNodeWithText(date).assertIsDisplayed()
        // Format is Sys/Dia@Pulse (no space now, or matched exactly)
        composeTestRule.onNodeWithText("120/80@70").assertIsDisplayed()
    }

    @Test
    fun cellClick_triggersViewModel() {
        val date = "2026-03-07"
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val emptyVal = context.getString(R.string.empty_value)
        
        uiStateFlow.value = TableUiState(
            isLoading = false,
            slotHeaders = listOf("Slot 1"),
            items = listOf(
                DayMeasurementSummary(date = date, slots = emptyMap(), isToday = true)
            )
        )

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                searchViewModel = searchViewModel,
                shareViewModel = shareViewModel,
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        // Click on the empty cell
        composeTestRule.onNodeWithText(emptyVal).performClick()

        verify { viewModel.onCellClicked(date, 0) }
    }

    @Test
    fun dialog_isDisplayed_whenOpen() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val title = context.getString(R.string.dialog_title_add)
        val info = context.getString(R.string.dialog_measurement_slot_info, "2026-03-07", 1)
        
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
                searchViewModel = searchViewModel,
                shareViewModel = shareViewModel,
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(info).assertIsDisplayed()
    }

    @Test
    fun dialogSave_callsViewModel_whenValid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val label = context.getString(R.string.label_measurement_format)
        val save = context.getString(R.string.button_save)
        
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
                searchViewModel = searchViewModel,
                shareViewModel = shareViewModel,
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        val input = "120/80 @72"
        composeTestRule.onNodeWithText(label).performTextInput(input)
        
        composeTestRule.onNodeWithText(save).assertIsEnabled().performClick()

        verify { viewModel.onSaveMeasurement(input) }
    }
}

