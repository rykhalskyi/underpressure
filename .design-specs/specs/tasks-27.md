# Tasks Document - Issue #27: Share Table Data as ASCII Message or CSV Export

- [x] 1. Create TableFormatter utility in domain layer
  - File: app/src/main/java/com/example/underpressure/domain/export/TableFormatter.kt
  - Implement `formatAsciiTable(headers: List<String>, rows: List<List<String>>, dateRange: String): String`
  - Implement `formatCsv(headers: List<String>, rows: List<List<String>>): String`
  - Purpose: Pure logic for formatting data into ASCII and CSV strings
  - _Leverage: String.padEnd() for alignment, requirements for 80-char width_
  - _Requirements: Requirement 1, Requirement 2_
  - _Prompt: Role: Kotlin Developer specializing in string manipulation | Task: Create a TableFormatter utility with functions to generate aligned ASCII tables and CSV strings. ASCII table must use monospaced padding, handle missing values with "—", and keep width < 80 chars. CSV must be standard comma-separated. | Restrictions: Pure Kotlin only, no Android dependencies, follow codestyle.md trailing commas | Success: Functions return correctly formatted strings matching requirements._

- [x] 2. Create TableFormatter unit tests
  - File: app/src/test/java/com/example/underpressure/domain/export/TableFormatterTest.kt
  - Test ASCII alignment with various column counts (1-4)
  - Test CSV formatting with special characters in values
  - Test missing value handling ("—")
  - Purpose: Ensure formatting logic is robust and matches visual requirements
  - _Leverage: JUnit 4_
  - _Requirements: Requirement 1, Requirement 2_
  - _Prompt: Role: QA Engineer | Task: Write unit tests for TableFormatter. Verify ASCII padding, header alignment, date range inclusion, and CSV correctness. Include edge cases like empty rows or 4 active slots. | Success: All tests pass, covering all formatting rules._

- [x] 3. Create TableExportManager in data layer
  - File: app/src/main/java/com/example/underpressure/data/export/TableExportManager.kt
  - Implement logic to fetch measurements from `MeasurementRepository`
  - Implement logic to fetch slot times from `SettingsRepository`
  - Coordinate with `TableFormatter` to produce final output
  - Purpose: Orchestrate data retrieval and transformation for export
  - _Leverage: MeasurementRepository, SettingsRepository_
  - _Requirements: Requirement 1, Requirement 2, Requirement 3_
  - _Prompt: Role: Android Developer | Task: Implement TableExportManager to fetch data for a given date range, filter for active slots only, and use TableFormatter to generate export strings. Use slot times as headers. | Restrictions: Use Coroutines for repository calls, follow structure.md for data layer | Success: Manager correctly aggregates data and returns formatted strings/files._

- [x] 4. Implement CSV File saving logic in TableExportManager
  - File: app/src/main/java/com/example/underpressure/data/export/TableExportManager.kt (continue from task 3)
  - Implement `saveCsvToCache(content: String): File`
  - Use naming pattern: `table_export_YYYYMMDD_HHMMSS.csv`
  - Purpose: Persist CSV data to app cache for sharing
  - _Leverage: context.cacheDir, java.time.LocalDateTime_
  - _Requirements: Requirement 2.2_
  - _Prompt: Role: Android Developer | Task: Add file saving logic to TableExportManager. Save CSV content to the application's cache directory with the specified timestamped filename. | Restrictions: Ensure proper exception handling for I/O operations | Success: CSV file is created in cache with correct name and content._

- [x] 5. Register FileProvider in AndroidManifest.xml
  - File: app/src/main/AndroidManifest.xml
  - Add `<provider>` entry for `androidx.core.content.FileProvider`
  - Link to `res/xml/file_paths.xml`
  - Purpose: Enable secure file sharing with external apps
  - _Leverage: app/src/main/res/xml/file_paths.xml_
  - _Requirements: Requirement 2.3_
  - _Prompt: Role: Android Developer | Task: Register a FileProvider in the manifest to allow sharing files from the cache directory. Use the existing file_paths.xml configuration. | Restrictions: Use unique authority string (e.g., "${applicationId}.fileprovider") | Success: Manifest updated correctly, FileProvider is active._

- [x] 6. Create ShareUiState and ShareViewModel
  - File: app/src/main/java/com/example/underpressure/ui/table/ShareUiState.kt
  - File: app/src/main/java/com/example/underpressure/ui/table/ShareViewModel.kt
  - Implement date range validation (From <= To)
  - Manage dialog visibility and processing state
  - Purpose: Manage UI state and logic for the Share Dialog
  - _Leverage: ViewModel, StateFlow, TableExportManager_
  - _Requirements: Requirement 3_
  - _Prompt: Role: Android UI Developer | Task: Create ShareViewModel and ShareUiState. Implement date range selection logic and validation. Provide methods to trigger ASCII share and CSV export via TableExportManager. | Restrictions: Follow codestyle.md State Management guidelines (MutableStateFlow.update) | Success: ViewModel manages state correctly, validates dates, and triggers export._

- [x] 7. Update MainActivity ViewModel Factory
  - File: app/src/main/java/com/example/underpressure/MainActivity.kt
  - Inject `TableExportManager` into `ShareViewModel`
  - Register `ShareViewModel` in `viewModelFactory`
  - Purpose: Enable dependency injection for the new ViewModel
  - _Leverage: existing ViewModelProvider.Factory in MainActivity_
  - _Requirements: Internal Architecture_
  - _Prompt: Role: Android Developer | Task: Update the ViewModel factory in MainActivity to support ShareViewModel. Instantiate TableExportManager with necessary repositories and pass it to the ViewModel. | Success: ShareViewModel can be retrieved using by viewModels()._

- [x] 8. Implement ShareDialog Composable
  - File: app/src/main/java/com/example/underpressure/ui/table/components/ShareDialog.kt
  - Create Material 3 dialog with DateRangePicker or two DatePickers
  - Add buttons for "Share as Message" and "Export CSV"
  - Purpose: User interface for configuring and triggering export
  - _Leverage: Material 3 DatePicker, ShareViewModel_
  - _Requirements: Requirement 3, Usability_
  - _Prompt: Role: Jetpack Compose Expert | Task: Implement the ShareDialog using Material 3 components. Include fields for From and To dates and buttons for the two export formats. Show validation errors if From > To. | Restrictions: Follow codestyle.md Modifier parameter placement | Success: Dialog looks and behaves according to Material 3 standards._

- [x] 9. Integrate Share Button in MeasurementTableScreen
  - File: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt
  - Add Share icon to TopAppBar
  - Trigger ShareDialog visibility via ViewModel
  - Purpose: Entry point for the sharing feature
  - _Leverage: Icons.Default.Share, TopAppBar actions_
  - _Requirements: Requirement 1_
  - _Prompt: Role: Android UI Developer | Task: Add a share button to the top app bar of the measurement table screen. Clicking it should open the ShareDialog. | Success: Share button is visible and functional._

- [x] 10. Implement Android Share Intents in ShareViewModel
  - File: app/src/main/java/com/example/underpressure/ui/table/ShareViewModel.kt (continue from task 6)
  - Implement `Intent.ACTION_SEND` for text/plain (ASCII)
  - Implement `Intent.ACTION_SEND` for CSV using `FileProvider.getUriForFile`
  - Purpose: Finalize sharing by opening the Android Share Sheet
  - _Leverage: Intent.createChooser, FileProvider_
  - _Requirements: Requirement 1.7, Requirement 2.5_
  - _Prompt: Role: Android Developer | Task: Implement the intent-triggering logic in ShareViewModel. Use ACTION_SEND with the appropriate MIME types and URI permissions for CSV files. | Success: Android share sheet opens with the correct data/file._
