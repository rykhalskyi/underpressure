# Tasks Document - Issue #25: Configurable Blood Pressure Chart Activity with Sharing feature

- [x] 1. Add MPAndroidChart dependency to Gradle
  - File: gradle/libs.versions.toml, app/build.gradle.kts
  - Add `mpandroidchart` version and library to version catalog and app module
  - Purpose: Provide charting capabilities
  - _Leverage: Existing version catalog pattern_
  - _Requirements: NFR (Dependency Management)_
  - _Prompt: Role: Android Build Engineer | Task: Add MPAndroidChart dependency (v3.1.0) to gradle/libs.versions.toml and app/build.gradle.kts | Restrictions: Follow project dependency organization conventions | Success: Gradle syncs successfully, MPAndroidChart classes are available_

- [x] 2. Create Chart Data Models and Enums
  - File: app/src/main/java/com/example/underpressure/ui/chart/MeasurementType.kt, app/src/main/java/com/example/underpressure/ui/chart/ChartUiState.kt
  - Define `MeasurementType` enum (SYS, DIA, PULSE) and `ChartUiState` data class
  - Purpose: Establish type safety and UI state structure for the chart
  - _Leverage: ui/table/TableUiState.kt_
  - _Requirements: 2.1, 2.2_
  - _Prompt: Role: Kotlin Developer | Task: Create MeasurementType enum and ChartUiState data class in com.example.underpressure.ui.chart following the design doc | Restrictions: Use Kotlin data classes and enums, follow project naming conventions | Success: Models compile and represent all required state properties_

- [x] 3. Implement ChartExportManager for PNG export
  - File: app/src/main/java/com/example/underpressure/data/export/ChartExportManager.kt
  - Create a class to handle Bitmap saving as PNG to `cacheDir`
  - Purpose: Enable chart image sharing
  - _Leverage: data/export/TableExportManager.kt (saveCsvToCache pattern)_
  - _Requirements: 3.2, 3.3_
  - _Prompt: Role: Android Developer | Task: Implement ChartExportManager in com.example.underpressure.data.export to save a Bitmap as a PNG file in cacheDir | Restrictions: Use Dispatchers.IO, use existing FileProvider authority | Success: PNG file is saved correctly in cache, returns valid File object_

- [x] 4. Create ChartViewModel with data processing logic
  - File: app/src/main/java/com/example/underpressure/ui/chart/ChartViewModel.kt
  - Implement ViewModel to fetch measurements and transform them into `MPAndroidChart` `LineData`
  - Purpose: Handle business logic for filtering and data preparation
  - _Leverage: ui/table/MeasurementTableViewModel.kt, ui/table/ShareViewModel.kt (date range logic)_
  - _Requirements: 1.1, 1.2, 2.1, 2.2_
  - _Prompt: Role: Android Developer (MVVM) | Task: Implement ChartViewModel with StateFlow for ChartUiState, including filtering logic for slots, types, and dates | Restrictions: Use constructor injection for repositories and export manager, follow thread-safe update patterns | Success: ViewModel correctly filters data and exposes valid LineData_

- [x] 5. Implement ChartViewModel unit tests
  - File: app/src/test/java/com/example/underpressure/ui/chart/ChartViewModelTest.kt
  - Write tests for data filtering, slot selection, and state updates
  - Purpose: Ensure reliability of chart logic
  - _Leverage: Existing unit tests in app/src/test/_
  - _Requirements: 1.1, 2.2_
  - _Prompt: Role: QA Engineer | Task: Create JUnit/MockK tests for ChartViewModel covering slot/type/date filtering and default configuration | Restrictions: Use MockK for repository mocks, cover edge cases (no data) | Success: All tests pass, 80%+ coverage on ViewModel logic_

- [x] 6. Create BloodPressureChart Composable wrapper
  - File: app/src/main/java/com/example/underpressure/ui/chart/components/BloodPressureChart.kt
  - Wrap `LineChart` from `MPAndroidChart` in an `AndroidView`
  - Purpose: Render the chart within Jetpack Compose
  - _Leverage: Jetpack Compose AndroidView pattern_
  - _Requirements: 1.1, 1.3, 2.3_
  - _Prompt: Role: Android UI Developer | Task: Create a Composable that wraps MPAndroidChart's LineChart using AndroidView, applying specified line styles (thick, normal, dashed) and slot colors | Restrictions: Follow Material 3 theme colors, ensure responsive layout | Success: Chart renders correctly with provided LineData and styling_

- [x] 7. Create ChartConfigurationSheet Composable
  - File: app/src/main/java/com/example/underpressure/ui/chart/components/ChartConfigurationSheet.kt
  - Implement a ModalBottomSheet for slot, type, and date range selection
  - Purpose: Provide a configuration interface for the chart
  - _Leverage: Material 3 ModalBottomSheet, ui/settings/ components_
  - _Requirements: 2.1, 2.2_
  - _Prompt: Role: Android UI Developer | Task: Create a Composable configuration sheet using Material 3 ModalBottomSheet with checkboxes for slots/types and date pickers | Restrictions: Use existing theme components, pass events up to ViewModel | Success: Sheet opens/closes correctly, reflects and updates UI state_

- [x] 8. Implement ChartScreen and Navigation
  - File: app/src/main/java/com/example/underpressure/ui/chart/ChartScreen.kt, app/src/main/java/com/example/underpressure/MainActivity.kt
  - Assemble `BloodPressureChart`, `ChartConfigurationSheet`, and buttons; add navigation to MainActivity
  - Purpose: Complete the Chart Activity/Screen and integrate it into the app
  - _Leverage: ui/table/MeasurementTableScreen.kt structure_
  - _Requirements: 1.1, 2.1, 3.1_
  - _Prompt: Role: Android Developer | Task: Assemble the ChartScreen and add navigation from MainActivity (e.g., a chart icon in top bar) | Restrictions: Follow existing screen organization and navigation patterns | Success: Users can navigate to ChartScreen, view the chart, configure it, and trigger sharing_

- [x] 9. Final Integration and Share Intent handling
  - File: app/src/main/java/com/example/underpressure/ui/chart/ChartScreen.kt
  - Implement the "Share" button click handler to trigger export and start `ACTION_SEND` intent
  - Purpose: Finalize the sharing feature
  - _Leverage: ui/table/MeasurementTableScreen.kt (share event handling patterns)_
  - _Requirements: 3.1, 3.2, 3.3_
  - _Prompt: Role: Android Developer | Task: Implement sharing logic in ChartScreen to capture chart bitmap, call export manager, and launch ACTION_SEND intent via FileProvider | Restrictions: Ensure proper runtime permissions and FileProvider setup | Success: Share sheet opens with chart image attached as PNG_
