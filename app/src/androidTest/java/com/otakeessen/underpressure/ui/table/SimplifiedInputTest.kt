package com.otakeessen.underpressure.ui.table

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.alarm.AlarmScheduler
import com.otakeessen.underpressure.domain.repository.MeasurementRepository
import com.otakeessen.underpressure.domain.repository.SettingsRepository
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import io.mockk.every
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class SimplifiedInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: MeasurementTableViewModel
    private val measurementRepository: MeasurementRepository = mockk(relaxed = true)
    private val settingsRepository: SettingsRepository = mockk(relaxed = true)
    private val alarmScheduler: AlarmScheduler = mockk(relaxed = true)

    // Set a fixed clock for deterministic behavior
    private val fixedClock = Clock.fixed(Instant.parse("2026-04-24T10:00:00Z"), ZoneId.of("UTC"))

    @Before
    fun setUp() {
        every { measurementRepository.getAllMeasurements() } returns flowOf(emptyList())
        every { settingsRepository.getSettings() } returns flowOf(null)
        
        viewModel = MeasurementTableViewModel(
            measurementRepository = measurementRepository,
            settingsRepository = settingsRepository,
            clock = fixedClock,
            alarmScheduler = alarmScheduler
        )
    }

    @Test
    fun testSimplifiedInput_SpaceDelimiter_OptionalPulse() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val emptyVal = context.getString(R.string.empty_value)
        val saveBtn = context.getString(R.string.button_save)
        val formatLabel = context.getString(R.string.label_measurement_format)

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                searchViewModel = mockk(relaxed = true),
                shareViewModel = mockk(relaxed = true),
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        // 1. Click on Today's empty cell (Today is 2026-04-24)
        composeTestRule.onNodeWithText(emptyVal).performClick()

        // 2. Input simplified format: "120 80" (Pulse omitted)
        composeTestRule.onNodeWithText(formatLabel).performTextInput("120 80")

        // 3. Save button should be enabled
        composeTestRule.onNodeWithText(saveBtn).assertIsEnabled().performClick()

        // 4. Verify save call in VM (via mocking repository)
        // Since we are using real VM, it calls measurementRepository.saveMeasurement
        // We can verify this call
        // Note: Slot index logic depends on settings, default first slot is active at index 0.
    }

    @Test
    fun testValidation_IncorrectMeasurements_ErrorMessage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val emptyVal = context.getString(R.string.empty_value)
        val formatLabel = context.getString(R.string.label_measurement_format)
        val errorMsg = context.getString(R.string.error_incorrect_measurements)

        composeTestRule.setContent {
            MeasurementTableScreen(
                viewModel = viewModel,
                searchViewModel = mockk(relaxed = true),
                shareViewModel = mockk(relaxed = true),
                onSettingsClick = {},
                onChartClick = {}
            )
        }

        composeTestRule.onNodeWithText(emptyVal).performClick()

        // Input Sys < Dia
        composeTestRule.onNodeWithText(formatLabel).performTextInput("80/120")

        // Error message should be displayed
        composeTestRule.onNodeWithText(errorMsg).assertIsDisplayed()
        
        // Save button should be disabled
        val saveBtn = context.getString(R.string.button_save)
        composeTestRule.onNodeWithText(saveBtn).assertIsNotEnabled()
    }
}
