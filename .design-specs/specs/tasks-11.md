# Tasks Document - Issue #11: Settings Screen (Slot Times)

- [x] 1. Update `AppSettingsEntity` to include `slotActiveFlags`
  - File: `app/src/main/java/com/example/underpressure/data/local/entities/AppSettingsEntity.kt`
  - Add `val slotActiveFlags: List<Boolean> = listOf(true, false, false, false)` to the data class.
  - Update default values for `slotTimes` and `slotAlarmsEnabled` to ensure 4 slots are always present.
  - Purpose: Support enabling/disabling of individual measurement slots as per requirements.
  - _Leverage: AppSettingsEntity.kt_
  - _Requirements: 1.2, 1.3, 1.5_
  - _Prompt: Role: Android Developer specializing in Room Persistence | Task: Update AppSettingsEntity.kt to include slotActiveFlags and set appropriate default values for 4 slots following requirement 1.1 and 1.5. | Restrictions: Maintain backward compatibility if possible, ensure id remains 1. | Success: Entity compiles and Room handles the migration (or destructive migration if acceptable in this stage)._

- [x] 2. Update `SettingsRepository` and its implementation
  - Files: `app/src/main/java/com/example/underpressure/domain/repository/SettingsRepository.kt`, `app/src/main/java/com/example/underpressure/data/repository/SettingsRepositoryImpl.kt`
  - Ensure repository correctly handles the updated `AppSettingsEntity`.
  - Purpose: Provide access to the new active flags in the settings data.
  - _Leverage: SettingsRepository.kt, SettingsRepositoryImpl.kt_
  - _Requirements: 2.1_
  - _Prompt: Role: Android Developer specializing in Repository Pattern | Task: Verify and update SettingsRepository and its implementation to support the updated AppSettingsEntity. | Restrictions: Follow existing clean architecture patterns. | Success: Repository correctly saves and retrieves settings with active flags._

- [x] 3. Create `SettingsUiState` and `SlotConfig` data classes
  - File: `app/src/main/java/com/example/underpressure/ui/settings/SettingsUiState.kt`
  - Define `SettingsUiState` and `SlotConfig` as outlined in the design document.
  - Purpose: Provide a structured state for the Settings Screen.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt (as pattern)_
  - _Requirements: 1.1, 1.2_
  - _Prompt: Role: Android UI Developer | Task: Create SettingsUiState.kt and SlotConfig data classes to represent the UI state of the settings screen according to the design doc. | Restrictions: Follow project naming conventions and structure. | Success: Data classes defined and ready for use in ViewModel._

- [x] 4. Implement `SettingsViewModel`
  - File: `app/src/main/java/com/example/underpressure/ui/settings/SettingsViewModel.kt`
  - Implement logic to load settings, update slot times, and update active states.
  - Ensure Slot 1 cannot be disabled.
  - Purpose: Manage Settings Screen state and handle user interactions.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt, SettingsRepository.kt_
  - _Requirements: 1.1, 1.2, 1.3, 1.5, 1.6, 2.1_
  - _Prompt: Role: Android ViewModel Developer | Task: Implement SettingsViewModel.kt to manage UI state and persistence using SettingsRepository. Ensure Slot 1 remains always active. | Restrictions: Use StateFlow and collect settings from repository. | Success: ViewModel correctly updates state and persists changes to repository._

- [x] 5. Implement `SettingsViewModelTest`
  - File: `app/src/test/java/com/example/underpressure/ui/settings/SettingsViewModelTest.kt`
  - Write unit tests for loading settings and updating slot configurations.
  - Test the "Slot 1 always active" constraint.
  - Purpose: Ensure reliability of settings logic.
  - _Leverage: app/src/test/java/com/example/underpressure/ui/table/MeasurementTableViewModelTest.kt_
  - _Requirements: 1.3, 2.1_
  - _Prompt: Role: Android QA Engineer | Task: Create unit tests for SettingsViewModel to verify state updates, persistence calls, and business rules like Slot 1 activity. | Restrictions: Use Mockito or similar for repository mocking. | Success: All tests pass, covering core settings logic._

- [x] 6. Create `SlotRow` and `TimePickerDialog` components
  - File: `app/src/main/java/com/example/underpressure/ui/settings/components/SettingsComponents.kt`
  - Implement reusable Composables for individual slot rows and the time selection dialog.
  - Purpose: Build the building blocks for the Settings Screen.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/components/DayRow.kt (as pattern), Material 3 TimePicker_
  - _Requirements: 1.2, 1.6_
  - _Prompt: Role: Jetpack Compose UI Developer | Task: Implement SlotRow and a TimePickerDialog using Material 3 components in SettingsComponents.kt. | Restrictions: Follow Material 3 design guidelines and project codestyle. | Success: Components are visually correct and handle interactions properly._

- [x] 7. Implement `SettingsScreen` Composable
  - File: `app/src/main/java/com/example/underpressure/ui/settings/SettingsScreen.kt`
  - Assemble the Settings Screen using the ViewModel and components.
  - Purpose: Provide the user interface for slot configuration.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt_
  - _Requirements: 1.1, 1.2, 3.1_
  - _Prompt: Role: Jetpack Compose UI Developer | Task: Create the SettingsScreen.kt Composable that displays the list of slots and handles navigation back. | Restrictions: Use Scaffold and TopAppBar. | Success: Screen renders correctly and interacts with ViewModel._

- [x] 8. Add Settings Button to `MeasurementTableScreen` and update `MainActivity`
  - Files: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`, `app/src/main/java/com/example/underpressure/MainActivity.kt`
  - Add a `TopAppBar` with a settings icon to `MeasurementTableScreen`.
  - Update `MainActivity` to handle navigation between the Table and Settings screens.
  - Purpose: Enable user access to the settings screen.
  - _Leverage: app/src/main/java/com/example/underpressure/MainActivity.kt_
  - _Requirements: 3.1_
  - _Prompt: Role: Android Developer | Task: Add a settings icon to the TopAppBar in MeasurementTableScreen and implement navigation to SettingsScreen in MainActivity. | Restrictions: Maintain consistent UI with existing theme. | Success: User can navigate to Settings and back._

- [x] 9. Verify dynamic Table Header updates
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Ensure the `MeasurementTableViewModel` correctly reacts to settings changes and updates `slotHeaders`.
  - Purpose: Ensure UI consistency across screens.
  - _Leverage: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt_
  - _Requirements: 2.3_
  - _Prompt: Role: Android Developer | Task: Verify that MeasurementTableViewModel correctly updates slot headers when settings change in the database. | Restrictions: No major refactoring if already supported. | Success: Main table headers update immediately after settings are changed._
