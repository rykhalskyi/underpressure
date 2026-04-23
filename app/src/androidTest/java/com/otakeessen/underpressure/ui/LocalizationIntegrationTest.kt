package com.otakeessen.underpressure.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.ui.table.MeasurementTableScreen
import com.otakeessen.underpressure.ui.table.MeasurementTableViewModel
import com.otakeessen.underpressure.ui.table.SearchUiState
import com.otakeessen.underpressure.ui.table.SearchViewModel
import com.otakeessen.underpressure.ui.table.ShareViewModel
import com.otakeessen.underpressure.ui.table.TableUiState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Instrumented test to verify that localization resource resolution works correctly in UI.
 */
class LocalizationIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MeasurementTableViewModel
    private lateinit var searchViewModel: SearchViewModel
    private lateinit var shareViewModel: ShareViewModel
    
    private val uiStateFlow = MutableStateFlow(TableUiState())
    private val searchUiStateFlow = MutableStateFlow(SearchUiState())
    private val searchQueryFlow = MutableStateFlow("")

    @Before
    fun setUp() {
        viewModel = mockk(relaxed = true)
        searchViewModel = mockk(relaxed = true)
        shareViewModel = mockk(relaxed = true)
        
        every { viewModel.uiState } returns uiStateFlow
        every { viewModel.scrollToDateEvent } returns MutableStateFlow("")
        every { searchViewModel.uiState } returns searchUiStateFlow
        every { searchViewModel.query } returns searchQueryFlow
        every { shareViewModel.uiState } returns MutableStateFlow(com.otakeessen.underpressure.ui.table.ShareUiState())
        every { shareViewModel.shareEvents } returns MutableStateFlow(ShareViewModel.ShareEvent.ShareText(""))
    }

    @Test
    fun measurementTableScreen_displaysLocalizedStrings() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val appName = context.getString(R.string.app_name)
        val addMeasurementCd = context.getString(R.string.cd_add_measurement)
        val searchCd = context.getString(R.string.cd_search)

        uiStateFlow.value = TableUiState(isLoading = false, isFabEnabled = true)

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                searchViewModel = searchViewModel,
                shareViewModel = shareViewModel,
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        // Verify Top Bar Title
        composeTestRule.onNodeWithText(appName).assertIsDisplayed()

        // Verify FAB Content Description
        composeTestRule.onNodeWithContentDescription(addMeasurementCd).assertIsDisplayed()

        // Verify Action Icon Content Description
        composeTestRule.onNodeWithContentDescription(searchCd).assertIsDisplayed()
    }
    
    @Test
    fun tableHeader_displaysLocalizedDateHeader() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dateHeader = context.getString(R.string.header_date)

        uiStateFlow.value = TableUiState(isLoading = false, slotHeaders = listOf("M", "E"))

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
    }
}

