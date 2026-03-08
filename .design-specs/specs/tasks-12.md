# Tasks Document - Issue #12: Per-Slot Alarm Implementation

- [x] 1. Add Android Permissions and Receiver Declarations
  - File: `app/src/main/AndroidManifest.xml`
  - Add `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, and `POST_NOTIFICATIONS` permissions.
  - Declare `AlarmReceiver` and `BootReceiver` (to be created) within the `<application>` tag.
  - Purpose: Enable the application to schedule alarms, show notifications, and receive boot events.
  - _Leverage: Existing AndroidManifest.xml structure_
  - _Requirements: 1.1, 3.1_
  - _Prompt: Role: Android Developer | Task: Update AndroidManifest.xml with required permissions (SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED, POST_NOTIFICATIONS) and declare BroadcastReceivers for AlarmReceiver and BootReceiver following requirements 1.1 and 3.1. | Restrictions: Maintain existing activity declarations, follow standard manifest structure. | Success: Manifest compiles, permissions are correctly requested, receivers are properly registered._

- [x] 2. Create AlarmScheduler Utility
  - File: `app/src/main/java/com/example/underpressure/alarm/AlarmScheduler.kt`
  - Implement a class to handle scheduling and canceling alarms using `AlarmManager`.
  - Add logic to calculate the next trigger time for daily repeating alarms.
  - Purpose: Encapsulate alarm management logic for reuse across the app.
  - _Leverage: AppSettingsEntity for time and alarm state_
  - _Requirements: 1.1, 1.3, 1.5_
  - _Prompt: Role: Android Developer | Task: Create AlarmScheduler.kt in a new alarm package. Implement methods to schedule daily alarms for measurement slots based on AppSettingsEntity. Ensure robust handling of PendingIntents and AlarmManager APIs following requirement 1.1. | Restrictions: Follow project package structure, use standard AlarmManager patterns, avoid memory leaks with PendingIntents. | Success: Scheduler can correctly calculate next alarm times and interface with AlarmManager._

- [x] 3. Implement AlarmReceiver for Notifications
  - File: `app/src/main/java/com/example/underpressure/receiver/AlarmReceiver.kt`
  - Implement a `BroadcastReceiver` that triggers a notification when an alarm fires.
  - Set up a Notification Channel and build a notification that opens `MainActivity` on click.
  - Purpose: Handle the visual and audible alert for the user.
  - _Leverage: MainActivity as target intent_
  - _Requirements: 1.2, 1.4, 2.1, 2.2, 2.3_
  - _Prompt: Role: Android Developer | Task: Create AlarmReceiver.kt in a new receiver package. Implement onReceive to show a notification with a title and message specific to the measurement slot. Configure the notification to open MainActivity and handle sound/vibration per requirement 2.1. | Restrictions: Follow Material 3 notification standards, ensure proper channel creation for Android 8.0+. | Success: Notification appears when the receiver is triggered, correctly identifies the slot, and opens the app._

- [x] 4. Update SettingsViewModel for Alarm Toggling
  - File: `app/src/main/java/com/example/underpressure/ui/settings/SettingsViewModel.kt`
  - Add `updateSlotAlarmEnabled(index: Int, isEnabled: Boolean)` method.
  - Integrate `AlarmScheduler` to trigger `updateAlarms()` after saving settings.
  - Purpose: Allow users to toggle alarms and ensure they are scheduled immediately.
  - _Leverage: SettingsRepository, currentSettings in ViewModel_
  - _Requirements: 1.1, 1.3, 1.5_
  - _Prompt: Role: Android ViewModel Specialist | Task: Update SettingsViewModel.kt to include updateSlotAlarmEnabled logic. Ensure that any change to slot times or alarm states triggers a call to AlarmScheduler.updateAlarms() following requirement 1.5. | Restrictions: Maintain MVVM patterns, handle repository operations asynchronously in viewModelScope. | Success: ViewModel correctly updates the data layer and triggers alarm rescheduling._

- [x] 5. Update UI Components for Alarm Toggles
  - File: `app/src/main/java/com/example/underpressure/ui/settings/components/SettingsComponents.kt`
  - Modify `SlotRow` to include a second toggle for "Remind me" (alarms).
  - Update `SettingsScreen.kt` to pass the new callback to `SlotRow`.
  - Purpose: Provide the user interface for enabling/disabling alarms per slot.
  - _Leverage: Existing SlotRow and SettingsScreen implementation_
  - _Requirements: 1.1_
  - _Prompt: Role: Jetpack Compose Developer | Task: Update SlotRow in SettingsComponents.kt to add an alarm toggle alongside the existing active toggle. Ensure proper spacing and Material 3 styling following project codestyle.md. | Restrictions: Follow Modifier parameter placement, maintain consistent UI layout with existing components. | Success: UI correctly displays alarm toggles for each slot and responds to user interaction._

- [x] 6. Implement BootReceiver for Alarm Persistence
  - File: `app/src/main/java/com/example/underpressure/receiver/BootReceiver.kt`
  - Implement a `BroadcastReceiver` that listens for `ACTION_BOOT_COMPLETED`.
  - Retrieve settings from `SettingsRepository` and call `AlarmScheduler.updateAlarms()`.
  - Purpose: Ensure alarms are rescheduled automatically after a device reboot.
  - _Leverage: SettingsRepository, AlarmScheduler_
  - _Requirements: 3.1_
  - _Prompt: Role: Android Developer | Task: Create BootReceiver.kt. Implement logic to load settings asynchronously using SettingsRepository and reschedule all active alarms via AlarmScheduler following requirement 3.1. | Restrictions: Handle background execution constraints (use a CoroutineScope or similar if needed), ensure the receiver is exported in manifest. | Success: Alarms are correctly rescheduled upon system boot._

- [x] 7. Create Alarm System Tests
  - File: `app/src/test/java/com/example/underpressure/alarm/AlarmSchedulerTest.kt`
  - File: `app/src/androidTest/java/com/example/underpressure/alarm/AlarmIntegrationTest.kt`
  - Write unit tests for time calculations and instrumented tests for notification appearance.
  - Purpose: Verify the reliability and correctness of the alarm system.
  - _Leverage: JUnit 4, UIAutomator_
  - _Requirements: AC 3 (Instrumented test)_
  - _Prompt: Role: QA Engineer | Task: Create unit tests for AlarmScheduler and instrumented tests using UIAutomator to verify that a notification is displayed when an alarm is triggered, following the acceptance criteria in requirements-12.md. | Restrictions: Mock AlarmManager where appropriate, ensure tests are independent of system time. | Success: All tests pass, verifying both logic and UI behavior._
