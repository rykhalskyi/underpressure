# Tasks Document - Global Alarm Toggle

- [x] 1. Update UI State Models
  - File: `app/src/main/java/com/example/underpressure/ui/settings/SettingsUiState.kt`
  - File: `app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt`
  - Add `isMasterAlarmEnabled: Boolean = false` to both `SettingsUiState` and `TableUiState` data classes.
  - Purpose: Allow ViewModels to expose the global alarm state to the UI.
  - _Leverage: Existing data class structures in both files._
  - _Requirements: 1.1, 2.1_
  - _Prompt: Role: Android Developer | Task: Add isMasterAlarmEnabled: Boolean field to SettingsUiState and TableUiState classes following requirements 1.1 and 2.1 | Restrictions: Maintain default value as false, ensure no breaking changes to existing constructors | Success: Both UI state classes include the new field and compile._

- [x] 2. Update AlarmScheduler Logic
  - File: `app/src/main/java/com/example/underpressure/alarm/AlarmScheduler.kt`
  - Modify `updateAlarms(settings: AppSettingsEntity)` to check `settings.masterAlarmEnabled`.
  - If `masterAlarmEnabled` is false, cancel all 4 slot alarms regardless of their individual settings.
  - Purpose: Ensure the master toggle controls the actual system alarms.
  - _Leverage: `cancelAlarm` method and loop structure in `AlarmScheduler.kt`._
  - _Requirements: 1.2, 1.3, 3.3_
  - _Prompt: Role: Android System Developer | Task: Update updateAlarms in AlarmScheduler.kt to respect the masterAlarmEnabled flag from AppSettingsEntity following requirements 1.2 and 1.3 | Restrictions: Do not change scheduleAlarm or cancelAlarm signatures | Success: Logic correctly cancels all alarms when master toggle is off._

- [x] 3. Update SettingsViewModel
  - File: `app/src/main/java/com/example/underpressure/ui/settings/SettingsViewModel.kt`
  - Update `loadSettings()` to map `masterAlarmEnabled` from the entity to `SettingsUiState`.
  - Implement `updateMasterAlarmEnabled(isEnabled: Boolean)` method that saves the updated `AppSettingsEntity` via `settingsRepository`.
  - Purpose: Provide the business logic for the master toggle in the Settings screen.
  - _Leverage: `saveSettings` and `loadSettings` patterns in `SettingsViewModel.kt`._
  - _Requirements: 1.4_
  - _Prompt: Role: Android ViewModel Specialist | Task: Update SettingsViewModel to handle masterAlarmEnabled state following requirement 1.4, leveraging existing saveSettings flow | Restrictions: Ensure alarmScheduler.updateAlarms is called after saving | Success: ViewModel correctly updates repository and scheduler when master toggle changes._

- [x] 4. Create GlobalAlarmRow Component
  - File: `app/src/main/java/com/example/underpressure/ui/settings/components/SettingsComponents.kt`
  - Implement a new Composable `GlobalAlarmRow(enabled: Boolean, onCheckedChange: (Boolean) -> Unit)`
  - Use a standard `Row` with a `Text` label ("Global Alarm Reminders") and the existing `Switch` component.
  - Purpose: Reusable UI component for the master toggle.
  - _Leverage: `Switch` and `SlotRow` patterns in `SettingsComponents.kt`._
  - _Requirements: 1.1_
  - _Prompt: Role: Jetpack Compose Developer | Task: Create GlobalAlarmRow component in SettingsComponents.kt following requirement 1.1, reusing the existing Switch extension | Restrictions: Match the styling and padding of SlotRow | Success: Component renders correctly with expected styling._

- [x] 5. Integrate Global Toggle into SettingsScreen
  - File: `app/src/main/java/com/example/underpressure/ui/settings/SettingsScreen.kt`
  - Add a `HorizontalDivider` after the `itemsIndexed` block in `LazyColumn`.
  - Add the `GlobalAlarmRow` below the divider, connecting it to `viewModel.uiState` and `viewModel.updateMasterAlarmEnabled`.
  - Purpose: Complete the UI implementation for Requirement 1.
  - _Leverage: `LazyColumn` and `HorizontalDivider` in `SettingsScreen.kt`._
  - _Requirements: 1.1_
  - _Prompt: Role: UI Developer | Task: Integrate GlobalAlarmRow and HorizontalDivider into SettingsScreen.kt following requirement 1.1 | Restrictions: Ensure the toggle is placed after the individual slot list | Success: Settings screen displays the master toggle correctly._

- [x] 6. Update MeasurementTableViewModel
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Update the `combine` block to extract `masterAlarmEnabled` from `settings` and update `TableUiState`.
  - Implement `toggleMasterAlarm()` which fetches current settings and flips the `masterAlarmEnabled` flag.
  - Purpose: Provide the business logic for the quick toggle on the main screen.
  - _Leverage: `settingsRepository.getSettings()` flow and `settingsRepository.saveSettings()` in `MeasurementTableViewModel.kt`._
  - _Requirements: 2.4_
  - _Prompt: Role: Android ViewModel Specialist | Task: Implement toggleMasterAlarm and update uiState mapping in MeasurementTableViewModel.kt following requirement 2.4 | Restrictions: Maintain reactive flow patterns using combine | Success: ViewModel correctly manages master alarm state for the main screen._

- [x] 7. Integrate Quick Toggle into MeasurementTableScreen
  - File: `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`
  - Add an `IconButton` to the `actions` in `TopAppBar`.
  - Use `Icons.Default.NotificationsActive` (or similar) when enabled, and `Icons.Default.NotificationsOff` when disabled.
  - Connect the button to `viewModel.toggleMasterAlarm()`.
  - Purpose: Complete the UI implementation for Requirement 2.
  - _Leverage: `TopAppBar` `actions` list in `MeasurementTableScreen.kt`._
  - _Requirements: 2.1, 2.2, 2.3_
  - _Prompt: Role: UI Developer | Task: Add master alarm toggle icon to TopAppBar in MeasurementTableScreen.kt following requirements 2.1, 2.2, and 2.3 | Restrictions: Use Material 3 standard icons | Success: Main screen displays the toggle icon with correct state-based iconography._

- [x] 8. Update Unit Tests
  - File: `app/src/test/java/com/example/underpressure/alarm/AlarmSchedulerTest.kt`
  - Add test case: `updateAlarms cancels all when masterAlarmEnabled is false`.
  - Add test case: `updateAlarms schedules active slots when masterAlarmEnabled is true`.
  - Purpose: Ensure logic correctness and prevent regressions.
  - _Leverage: Existing MockK setup in `AlarmSchedulerTest.kt`._
  - _Requirements: 3.1, 3.2, 3.3_
  - _Prompt: Role: QA Engineer | Task: Add unit tests to AlarmSchedulerTest.kt covering master toggle logic according to requirements 3.1, 3.2, and 3.3 | Restrictions: Use existing MockK framework | Success: All new tests pass and cover the master toggle logic._
