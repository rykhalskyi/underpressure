# Tasks Document - Issue #18: Add Measurements Action Button

- [x] 1. Update TableUiState with FAB properties
  - File: `app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt`
  - Add `isFabEnabled: Boolean` and `fabTargetSlotIndex: Int?` to the `TableUiState` data class.
  - Purpose: Provide the UI with information to render and handle the FAB state.
  - _Leverage: `app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt`_
  - _Requirements: 1.2, 1.3_
  - _Prompt: Role: Android Developer specializing in Jetpack Compose and state management | Task: Update the `TableUiState` data class in `app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt` to include `isFabEnabled: Boolean = false` and `fabTargetSlotIndex: Int? = null` following requirements 1.2 and 1.3. | Restrictions: Do not modify existing properties, maintain default values. | Success: `TableUiState` updated and compiles correctly._

- [x] 2. Implement FAB eligibility logic in MeasurementTableViewModel
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Implement logic within the `uiState` combine block to calculate FAB eligibility.
  - Logic: Check active slots, current time (±15 min), and whether today's measurement already exists for those slots.
  - Handle "closest in the past" rule for multiple overlaps.
  - Purpose: Encapsulate the business logic for the action button's state.
  - _Leverage: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`_
  - _Requirements: 1.2, 1.3, 1.4, 2.1, 2.2, 2.3_
  - _Prompt: Role: Kotlin Developer with expertise in reactive programming and business logic | Task: Implement FAB eligibility logic in `MeasurementTableViewModel.kt`'s `uiState` flow. The logic must determine if an active slot is within ±15 minutes of `LocalTime.now()`, is currently empty for today, and identify the correct target slot (closest in past if multiple overlap) as per requirements 1.2, 1.3, 1.4, 2.1, 2.2, and 2.3. | Restrictions: Use `java.time.LocalTime` for comparisons, ensure logic is efficient within the flow. | Success: `uiState` correctly emits `isFabEnabled` and `fabTargetSlotIndex` based on current time and data._

- [x] 3. Add onFabClicked handler to MeasurementTableViewModel
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Add a public function `onFabClicked()` that triggers the measurement dialog for today's date and the `fabTargetSlotIndex`.
  - Reuse `onCellClicked` logic or extract common dialog opening logic.
  - Purpose: Provide an entry point for the FAB click event.
  - _Leverage: `onCellClicked` in `MeasurementTableViewModel.kt`_
  - _Requirements: 1.5, 3.1_
  - _Prompt: Role: Android Developer with expertise in MVVM pattern | Task: Add `onFabClicked()` to `MeasurementTableViewModel.kt`. This function should open the `MeasurementEditDialog` for the current date and the identified `fabTargetSlotIndex` from the state, following requirements 1.5 and 3.1. | Restrictions: Ensure the dialog state is updated correctly, handle potential null for `fabTargetSlotIndex` gracefully. | Success: `onFabClicked` correctly updates `_dialogState` to open the dialog for the target slot._

- [x] 4. Integrate FAB into MeasurementTableScreen
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`
  - Update the `Scaffold` to include the `floatingActionButton` parameter.
  - Use Material 3 `FloatingActionButton` with a `+` icon (`Icons.Default.Add`).
  - Bind the button's enabled state and click handler to the ViewModel via `uiState`.
  - Purpose: Add the visual component of the feature to the UI.
  - _Leverage: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`_
  - _Requirements: 1.1, 1.5_
  - _Prompt: Role: Jetpack Compose Developer specializing in Material 3 | Task: Integrate a `FloatingActionButton` into the `Scaffold` of `MeasurementTableScreen.kt` following requirements 1.1 and 1.5. Use `Icons.Default.Add`, bind `enabled = uiState.isFabEnabled`, and `onClick = viewModel::onFabClicked`. | Restrictions: Follow Material 3 design guidelines, ensure proper padding/placement. | Success: FAB is visible on the screen and correctly reflects the enabled state from `uiState`._

- [x] 5. Create unit tests for FAB logic in MeasurementTableViewModelTest
  - File: `app/src/test/java/com/example/underpressure/ui/table/MeasurementTableViewModelTest.kt`
  - Write tests covering various scenarios: empty slots within/outside window, filled slots, multiple overlaps.
  - Use mocked repositories and controlled time.
  - Purpose: Ensure the business logic is correct and reliable.
  - _Leverage: `app/src/test/java/com/example/underpressure/ui/table/MeasurementTableViewModelTest.kt`_
  - _Requirements: 1.2, 1.3, 1.4_
  - _Prompt: Role: QA Engineer specializing in unit testing Android ViewModels | Task: Add unit tests to `MeasurementTableViewModelTest.kt` covering FAB eligibility logic as defined in requirements 1.2, 1.3, and 1.4. Test scenarios should include: window ±15 min, filled vs empty slots, and selection of closest-in-past slot. | Restrictions: Use JUnit 4 and MockK (or similar if already in use), ensure tests are isolated. | Success: All tests pass, covering edge cases for slot selection logic._

- [x] 6. Add UI tests for FAB state
  - File: `app/src/androidTest/java/com/example/underpressure/ui/table/MeasurementTableScreenTest.kt`
  - Write instrumented tests to verify the FAB's enabled/disabled state in the UI.
  - Purpose: Verify the end-to-end integration and visibility.
  - _Leverage: `app/src/androidTest/java/com/example/underpressure/ui/table/MeasurementTableScreenTest.kt`_
  - _Requirements: 1.1, 1.2, 1.3_
  - _Prompt: Role: QA Automation Engineer with expertise in Compose UI Testing | Task: Create UI tests in `MeasurementTableScreenTest.kt` to verify that the Floating Action Button is displayed and correctly enabled/disabled based on the provided state, following requirements 1.1, 1.2, and 1.3. | Restrictions: Use `ComposeTestRule`, follow project's UI testing patterns. | Success: UI tests reliably verify FAB presence and state._
