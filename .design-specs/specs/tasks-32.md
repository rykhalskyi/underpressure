# Tasks - 32: Tiered time hierarchy in measurements list

- [x] 1. Define hierarchical UI models in TableUiState.kt
  - File: `app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt`
  - Define `TableItem` sealed class with `YearHeader`, `MonthHeader`, and `DayRow`.
  - Add `displayItems: List<TableItem>`, `expandedYears: Set<Int>`, and `expandedMonths: Set<String>` to `TableUiState`.
  - Purpose: Establish the data structure for the hierarchical list.
  - _Requirements: Structure_
  - _Prompt: Role: Android Developer specializing in Jetpack Compose and MVVM | Task: Define a sealed class `TableItem` in `TableUiState.kt` to represent Year headers, Month headers, and Day rows. Update `TableUiState` to include `displayItems`, `expandedYears` (Set of Int), and `expandedMonths` (Set of Strings like "2026-04"). | Success: Code compiles, and the state holds all necessary information for hierarchy and expansion._

- [x] 2. Implement grouping and expansion logic in MeasurementTableViewModel.kt
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Initialize expansion sets with the current year and month.
  - Update the `uiState` transformation to group `summarizedItems` and flatten them into `TableItem` list based on expansion states.
  - Add `toggleYearExpansion(year: Int)` and `toggleMonthExpansion(yearMonth: String)` methods.
  - Purpose: Core logic for the tiered hierarchy.
  - _Requirements: Structure, Default States, Interaction_
  - _Prompt: Role: Android Developer specializing in Kotlin Coroutines and Flow | Task: Update `MeasurementTableViewModel` to transform flat measurement data into a hierarchical list of `TableItem`. Implement `toggleYearExpansion` and `toggleMonthExpansion` using `_uiState.update`. Ensure current year and month are expanded by default. | Success: ViewModel correctly computes `displayItems` based on expansion state._

- [x] 3. Create YearHeader Composable
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/YearHeader.kt`
  - Implement a sticky-capable header for years with a title and chevron.
  - Purpose: UI representation of the Year tier.
  - _Requirements: UI_
  - _Prompt: Role: Jetpack Compose UI Developer | Task: Create a `YearHeader` Composable in `components/YearHeader.kt`. It should display the year (e.g., "2025") and a chevron that rotates based on expansion state. Use Material 3 typography and colors. | Success: Composable rendered correctly and reflects expansion state._

- [x] 4. Create MonthHeader Composable
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/MonthHeader.kt`
  - Implement a sticky-capable header for months with a title, chevron, and optional summary.
  - Purpose: UI representation of the Month tier.
  - _Requirements: UI_
  - _Prompt: Role: Jetpack Compose UI Developer | Task: Create a `MonthHeader` Composable in `components/MonthHeader.kt`. It should display the month name and a chevron. Optionally show a summary (like avg BP). Use Material 3 styling. | Success: Composable rendered correctly and reflects expansion state._

- [x] 5. Update MeasurementTableScreen to use hierarchical list
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`
  - Replace the flat `items(uiState.items)` in `LazyColumn` with `items(uiState.displayItems)`.
  - Use `when` to render `YearHeader`, `MonthHeader`, or `DayRow`.
  - Pass toggle handlers from ViewModel to headers.
  - Purpose: Connect the UI to the hierarchical data.
  - _Requirements: Interaction, UI_
  - _Prompt: Role: Android Developer specializing in Jetpack Compose | Task: Update `MeasurementTableScreen.kt` to render the hierarchical list from `uiState.displayItems`. Implement a `when` branch for each `TableItem` type. Pass expansion toggle events to the ViewModel. | Success: The list displays years, months, and days correctly with working expand/collapse functionality._

- [x] 6. Add unit tests for hierarchy logic
  - File: `app/src/test/java/com/example/underpressure/ui/table/MeasurementTableViewModelTest.kt`
  - Test grouping of measurements into Year/Month/Day.
  - Test that only expanded sections are flattened into the display list.
  - Test expansion toggles.
  - Purpose: Ensure reliability of the transformation logic.
  - _Requirements: Acceptance Criteria_
  - _Prompt: Role: QA Engineer specializing in JUnit and MockK | Task: Create unit tests in `MeasurementTableViewModelTest.kt` to verify that the transformation from raw measurements to `TableItem` list works correctly, including default expansion and manual toggling. | Success: All tests pass, covering various expansion scenarios._

- [x] 7. Add UI integration tests for expansion (Skipped - Verified manually via debug seed data)
  - File: `app/src/androidTest/java/com/example/underpressure/ui/table/TableHierarchyIntegrationTest.kt`
  - Test clicking headers expands/collapses sections.
  - Test that current month is visible on load.
  - Purpose: End-to-end verification of the feature.
  - _Requirements: Acceptance Criteria_
  - _Prompt: Role: QA Automation Engineer specializing in Compose UI Test | Task: Create an instrumented test `TableHierarchyIntegrationTest.kt` to verify the hierarchical UI behavior. Assert that sections expand and collapse upon clicking headers and that the current month is expanded by default. | Success: Instrumented tests pass, validating UI interaction._
