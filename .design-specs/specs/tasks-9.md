# Tasks Document: Search and Navigation (Issue #9)

- [x] 1. Update `MeasurementDao` with Search Query
  - File: `app/src/main/java/com/example/underpressure/data/local/dao/MeasurementDao.kt`
  - Add a new `@Query` method `searchByValue(query: String)` using SQLite `LIKE` for partial numeric matches on systolic, diastolic, and pulse fields.
  - Purpose: Provide database-level search capability for numeric values.
  - _Leverage: app/src/main/java/com/example/underpressure/data/local/dao/MeasurementDao.kt_
  - _Requirements: Requirement 3_
  - _Prompt: Role: Android Database Developer (Room) | Task: Add a searchByValue(query: String): Flow<List<MeasurementEntity>> method to MeasurementDao. Use "SELECT * FROM measurements WHERE CAST(systolic AS TEXT) LIKE :query OR CAST(diastolic AS TEXT) LIKE :query OR CAST(pulse AS TEXT) LIKE :query ORDER BY date DESC". Ensure proper query formatting for LIKE (e.g., %query%). | Restrictions: Do not modify existing methods. Follow existing Room patterns. | Success: Query compiles and correctly filters measurements by partial numeric matches._

- [x] 2. Extend `MeasurementRepository` Interface and Implementation
  - Files: `app/src/main/java/com/example/underpressure/domain/repository/MeasurementRepository.kt`, `app/src/main/java/com/example/underpressure/data/repository/MeasurementRepositoryImpl.kt`
  - Add `searchMeasurements(query: String): Flow<List<MeasurementEntity>>` to the interface and implement it in the Impl class by calling the new DAO method.
  - Purpose: Expose search functionality to the Domain/UI layers.
  - _Leverage: MeasurementRepository.kt, MeasurementRepositoryImpl.kt_
  - _Requirements: Requirement 3_
  - _Prompt: Role: Android Developer (Clean Architecture) | Task: Update MeasurementRepository and its implementation to include searchMeasurements(query: String). Use the newly created searchByValue method from MeasurementDao. | Restrictions: Maintain consistent return types (Flow). | Success: Repository provides search results as a Flow._

- [x] 3. Create `SearchUiState` Data Class
  - File: `app/src/main/java/com/example/underpressure/ui/table/SearchUiState.kt`
  - Define a data class to hold search query, results, loading state, and error messages.
  - Purpose: Provide a structured state for the Search UI.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt_
  - _Requirements: Requirement 2, 3_
  - _Prompt: Role: Android Developer (Compose/MVVM) | Task: Create SearchUiState data class with query: String, results: List<MeasurementEntity>, isLoading: Boolean, dateError: String?, and isNoResults: Boolean. | Restrictions: Follow PascalCase for the file name and data class. | Success: Data class is well-defined and ready for use in ViewModel._

- [x] 4. Implement `SearchViewModel`
  - File: `app/src/main/java/com/example/underpressure/ui/table/SearchViewModel.kt`
  - Create a new ViewModel to handle search logic, query debouncing, and date validation.
  - Purpose: Encapsulate search business logic and state management.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt_
  - _Requirements: Requirement 2, 3_
  - _Prompt: Role: Android ViewModel Developer | Task: Implement SearchViewModel using MeasurementRepository. Include a query StateFlow, and use flatMapLatest to trigger repository searches when the query changes (numeric). Implement date validation logic using DateTimeFormatter. | Restrictions: Use Coroutines and StateFlow. Follow project's MVVM patterns. | Success: ViewModel correctly manages search state and performs debounced searches._

- [x] 5. Create `SearchResultItem` Composable
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/SearchResultItem.kt`
  - Create a small reusable component to display an individual search result (Date + Systolic/Diastolic @Pulse).
  - Purpose: Consistent UI for search results.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/components/DayRow.kt_
  - _Requirements: Requirement 3_
  - _Prompt: Role: Jetpack Compose Developer | Task: Create a @Composable SearchResultItem(measurement: MeasurementEntity, onClick: () -> Unit). Use Material 3 ListItem or Row with appropriate typography and padding. | Restrictions: Pass Modifier as first optional parameter. | Success: Composable renders measurement data clearly and handles clicks._

- [x] 6. Implement `SearchDialog` Composable
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/SearchDialog.kt`
  - Create a Material 3 dialog containing a search TextField and a LazyColumn for results.
  - Purpose: Provide the primary search interface.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/components/MeasurementEditDialog.kt_
  - _Requirements: Requirement 1, 2, 3_
  - _Prompt: Role: Jetpack Compose Developer | Task: Create @Composable SearchDialog(viewModel: SearchViewModel, onDismiss: () -> Unit, onResultClick: (String) -> Unit). Include a TextField for search input and show results using SearchResultItem in a LazyColumn. | Restrictions: Follow Material 3 design standards. | Success: Dialog displays correctly, handles input, and shows scrollable results._

- [x] 7. Integrate Search into `MeasurementTableScreen`
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`
  - Add a search icon button to the `TopAppBar` and manage the visibility of the `SearchDialog`.
  - Purpose: Provide a user-facing entry point for search.
  - _Leverage: MeasurementTableScreen.kt_
  - _Requirements: Requirement 1_
  - _Prompt: Role: Jetpack Compose Developer | Task: Add a Search Icon button to the TopAppBar actions. Use a local boolean state to show/hide the SearchDialog. Pass search navigation events to the ViewModel. | Restrictions: Do not break existing TopAppBar functionality. | Success: Search icon is visible and opens the SearchDialog._

- [x] 8. Implement Scroll Navigation in `MeasurementTableViewModel`
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Add a mechanism (e.g., a shared event or state) to signal that the `MeasurementTableScreen` should scroll to a specific date.
  - Purpose: Fulfill the "Jump to date" requirement.
  - _Leverage: MeasurementTableViewModel.kt_
  - _Requirements: Requirement 2, 3_
  - _Prompt: Role: Android Developer (MVVM/State) | Task: Add a scrollToDate(date: String) method to MeasurementTableViewModel. Use a SharedFlow or a single-shot state event to notify the UI to scroll. | Restrictions: Ensure the event is handled only once. | Success: UI receives scroll events and can react to them._

- [x] 9. Implement Auto-scrolling in `MeasurementTableScreen`
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`
  - Use `LazyListState` to scroll to the index of the selected date when a scroll event is received.
  - Purpose: Finalize the navigation logic.
  - _Leverage: MeasurementTableScreen.kt_
  - _Requirements: Requirement 2, 3_
  - _Prompt: Role: Jetpack Compose Developer | Task: Collect the scroll event from ViewModel and use lazyListState.animateScrollToItem() to navigate to the selected date. You may need to find the index of the date in uiState.items. | Restrictions: Perform scrolling within a CoroutineScope. | Success: The table automatically scrolls to the selected date._

- [x] 10. Add Search Unit Tests
  - File: `app/src/test/java/com/example/underpressure/ui/table/SearchViewModelTest.kt`
  - Write tests for query debouncing, date parsing, and result filtering.
  - Purpose: Ensure search logic is robust and reliable.
  - _Leverage: app/src/test/java/com/example/underpressure/ui/table/MeasurementTableViewModelTest.kt_
  - _Requirements: Requirement 2_
  - _Prompt: Role: Android Test Engineer | Task: Create unit tests for SearchViewModel. Mock MeasurementRepository and verify that search queries trigger the correct repository calls. Test invalid date formats. | Success: All tests pass and cover search edge cases._

- [x] 11. Add DAO Search Integration Tests
  - File: `app/src/androidTest/java/com/example/underpressure/data/local/RoomDatabaseTest.kt`
  - Update existing database tests to include the new `searchByValue` query.
  - Purpose: Verify correctness of SQLite LIKE queries.
  - _Leverage: app/src/androidTest/java/com/example/underpressure/data/local/RoomDatabaseTest.kt_
  - _Requirements: Requirement 3_
  - _Prompt: Role: Android Instrumentation Test Engineer | Task: Add a test case to RoomDatabaseTest that inserts several measurements and verifies that searchByValue returns the expected results for various partial matches (e.g., "12", "80"). | Success: Database search queries work correctly in the Android environment._
