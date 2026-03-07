# Tasks Document - Issue #7: Table UI Layout

- [x] 1. Create `DayMeasurementSummary` data model in `ui` layer
  - File: `app/src/main/java/com/example/underpressure/ui/table/DayMeasurementSummary.kt`
  - Define a data class for summarized daily measurements as per design
  - Include fields: `date: String`, `systolic: Int?`, `diastolic: Int?`, `pulse: Int?`, `isToday: Boolean`
  - Purpose: Provide a clean data structure for the table UI rows
  - _Leverage: com.example.underpressure.data.local.entities.MeasurementEntity_
  - _Requirements: 1.1_
  - _Prompt: Role: Kotlin Developer | Task: Create the DayMeasurementSummary data class in the com.example.underpressure.ui.table package. It should hold the summarized data for a single day's measurement as defined in design-7.md. | Success: Data class created with correct types and package._

- [x] 2. Define `TableUiState` in `ui` layer
  - File: `app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt`
  - Implement the UI state data class containing `isLoading`, `items` (List of DayMeasurementSummary), and `error`
  - Purpose: Manage the state of the Table Screen reactively
  - _Leverage: Patterns from .design-specs/codestyle.md (State Management section)_
  - _Requirements: 2.1_
  - _Prompt: Role: Android Developer | Task: Create TableUiState data class in com.example.underpressure.ui.table following the design-7.md specification. Ensure it supports loading and error states. | Success: UI state class correctly implements specified fields._

- [x] 3. Create `MeasurementTableViewModel`
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Implement ViewModel extending `androidx.lifecycle.ViewModel`
  - Inject `MeasurementRepository` via constructor
  - Expose `uiState: StateFlow<TableUiState>`
  - Purpose: Orchestrate data from repository to the UI state
  - _Leverage: com.example.underpressure.domain.repository.MeasurementRepository_
  - _Requirements: 1.1, 2.2_
  - _Prompt: Role: Android ViewModel Specialist | Task: Implement MeasurementTableViewModel. It should collect measurements from MeasurementRepository, transform them into DayMeasurementSummary items, and update the TableUiState. Handle the "isToday" logic using the current system date. | Success: ViewModel correctly transforms repository data into UI state._

- [x] 4. Create `DayRow` Composable component
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/DayRow.kt`
  - Implement a stateless Composable for a table row
  - Accept `DayMeasurementSummary` and `Modifier` as parameters
  - Use `Row` with 4 weighted columns for alignment
  - Purpose: Render a single day's data in the table
  - _Leverage: .design-specs/codestyle.md (Jetpack Compose: Modifier Parameter Placement)_
  - _Requirements: 1.2, 1.3, 1.4, 1.5_
  - _Prompt: Role: Jetpack Compose Expert | Task: Create the DayRow Composable. Use Row with 4 columns. Highlight the row if isToday is true. Use placeholder "-" for null values. Follow the Modifier parameter convention from codestyle.md. | Success: Component renders correctly with weights and conditional highlighting._

- [x] 5. Create `TableHeader` Composable component
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/TableHeader.kt`
  - Implement a Composable for the table column headers
  - Labels: Date, Systolic, Diastolic, Pulse
  - Purpose: Provide column context for the measurement values
  - _Leverage: app/src/main/res/values/strings.xml for header labels_
  - _Requirements: 1.2_
  - _Prompt: Role: Frontend Developer | Task: Create TableHeader Composable with 4 columns matching the DayRow weights. Use string resources for labels. | Success: Header aligns with DayRow columns._

- [x] 6. Create `MeasurementTableScreen` root Composable
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`
  - Combine `TableHeader` and a `LazyColumn` of `DayRow` items
  - Collect `uiState` from `MeasurementTableViewModel` using `collectAsStateWithLifecycle()`
  - Purpose: The main entry point for the Table UI feature
  - _Leverage: .design-specs/tech.md (Jetpack Compose section)_
  - _Requirements: 1.1, 2.1_
  - _Prompt: Role: Android UI Developer | Task: Implement MeasurementTableScreen. It should observe the ViewModel's state and render a LazyColumn for performance. Show a loading indicator if isLoading is true. | Success: Screen displays the list of measurements with smooth scrolling._

- [x] 7. Add unit tests for `MeasurementTableViewModel`
  - File: `app/src/test/java/com/example/underpressure/ui/table/MeasurementTableViewModelTest.kt`
  - Mock `MeasurementRepository` to return sample data
  - Verify mapping from `MeasurementEntity` to `DayMeasurementSummary`
  - Purpose: Ensure data transformation logic is correct and "Today" logic works
  - _Leverage: app/src/test/java/com/example/underpressure/data/repository/MeasurementRepositoryImplTest.kt patterns_
  - _Requirements: 2.2_
  - _Prompt: Role: QA Engineer | Task: Write unit tests for MeasurementTableViewModel using JUnit 4 and MockK/Mockito. Test empty state, multiple measurements for one day (taking latest), and "isToday" flag. | Success: All tests pass, covering data transformation and edge cases._

- [x] 8. Add UI test for `MeasurementTableScreen`
  - File: `app/src/androidTest/java/com/example/underpressure/ui/table/MeasurementTableScreenTest.kt`
  - Create a Compose UI test to verify the table rendering
  - Verify that headers and rows are displayed
  - Purpose: Validate UI integrity and accessibility
  - _Leverage: app/src/androidTest/java/com/example/underpressure/ExampleInstrumentedTest.kt_
  - _Requirements: 1.2_
  - _Prompt: Role: UI Automation Engineer | Task: Create a Compose UI test that sets a mock state and verifies that the Date, Systolic, Diastolic, and Pulse columns are rendered. | Success: UI test confirms expected layout rendering._
