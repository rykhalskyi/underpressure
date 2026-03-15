# Tasks Document - Localization Setup

- [x] 1. Consolidate default strings in strings.xml
  - File: app/src/main/res/values/strings.xml
  - Extract all hardcoded strings identified in UI files into `strings.xml`
  - Add accessibility content descriptions (e.g., `cd_search`, `cd_share`, `cd_settings`, `cd_add`)
  - Organize strings with XML comments by screen/feature
  - Purpose: Centralize all user-facing text for localization
  - _Leverage: app/src/main/res/values/strings.xml_
  - _Requirements: 1.1_
  - _Prompt: Role: Android Developer specializing in resource management | Task: Extract all hardcoded strings from MeasurementTableScreen.kt and SettingsScreen.kt into app/src/main/res/values/strings.xml following requirement 1.1 | Restrictions: Use snake_case for resource IDs, add comments to group strings, do not change existing IDs | Success: All UI-facing strings are in strings.xml, no hardcoded strings remain in layouts (if any), lint passes_

- [x] 2. Create German translation resource
  - File: app/src/main/res/values-de/strings.xml
  - Create directory `values-de` and a new `strings.xml` file
  - Provide accurate German translations for all keys defined in the default `strings.xml`
  - Purpose: Support German-speaking users
  - _Leverage: app/src/main/res/values/strings.xml_
  - _Requirements: 2.1, 2.3_
  - _Prompt: Role: Localization Specialist (DE) | Task: Create a German translation file app/src/main/res/values-de/strings.xml containing translations for all keys in the default strings.xml following requirements 2.1 and 2.3 | Restrictions: Maintain identical resource IDs, preserve string placeholders (%1$s), ensure grammatical correctness | Success: values-de/strings.xml exists, contains all keys from default strings.xml, and passes Android lint_

- [x] 3. Create Ukrainian translation resource
  - File: app/src/main/res/values-uk/strings.xml
  - Create directory `values-uk` and a new `strings.xml` file
  - Provide accurate Ukrainian translations for all keys defined in the default `strings.xml`
  - Purpose: Support Ukrainian-speaking users
  - _Leverage: app/src/main/res/values/strings.xml_
  - _Requirements: 2.2, 2.3_
  - _Prompt: Role: Localization Specialist (UK) | Task: Create a Ukrainian translation file app/src/main/res/values-uk/strings.xml containing translations for all keys in the default strings.xml following requirements 2.2 and 2.3 | Restrictions: Maintain identical resource IDs, preserve string placeholders (%1$s), ensure grammatical correctness | Success: values-uk/strings.xml exists, contains all keys from default strings.xml, and passes Android lint_

- [x] 4. Refactor MeasurementTableScreen for localization
  - File: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt
  - Replace hardcoded strings in `TopAppBar` title, `Icon` content descriptions, and error messages with `stringResource()`
  - Update intent chooser titles for sharing and CSV export to use localized strings
  - Purpose: Enable localization in the main table screen
  - _Leverage: app/src/main/res/values/strings.xml_
  - _Requirements: 1.3, 3.1, 3.2_
  - _Prompt: Role: Jetpack Compose Developer | Task: Refactor MeasurementTableScreen.kt to use stringResource() for all user-facing text and intent titles following requirements 1.3, 3.1, and 3.2 | Restrictions: Do not change UI logic, ensure all Icons have localized contentDescriptions, handle nullability for error strings | Success: No hardcoded strings in MeasurementTableScreen.kt, UI renders correctly with resource IDs_

- [x] 5. Refactor SettingsScreen for localization
  - File: app/src/main/java/com/example/underpressure/ui/settings/SettingsScreen.kt
  - Replace hardcoded strings in `TopAppBar`, Section Headers, and `ExactAlarmWarning` with `stringResource()`
  - Ensure "GRANT" button text is localized
  - Purpose: Enable localization in the settings screen
  - _Leverage: app/src/main/res/values/strings.xml_
  - _Requirements: 1.3, 3.2_
  - _Prompt: Role: Jetpack Compose Developer | Task: Refactor SettingsScreen.kt to use stringResource() for all user-facing text following requirements 1.3 and 3.2 | Restrictions: Maintain existing component structure, ensure accessibility labels are applied | Success: No hardcoded strings in SettingsScreen.kt, UI renders correctly with resource IDs_

- [x] 6. Refactor SettingsComponents for localization
  - File: app/src/main/java/com/example/underpressure/ui/settings/components/SettingsComponents.kt
  - Replace any remaining hardcoded strings in `SlotRow`, `GlobalAlarmRow`, or `TimePickerDialog`
  - Purpose: Complete localization of settings-related components
  - _Leverage: app/src/main/res/values/strings.xml_
  - _Requirements: 1.3_
  - _Prompt: Role: Jetpack Compose Developer | Task: Refactor SettingsComponents.kt to use stringResource() for any remaining hardcoded text following requirement 1.3 | Restrictions: Ensure placeholder usage in strings.xml is correct for dynamic values | Success: All components in SettingsComponents.kt are fully localized_

- [x] 7. Localize Background Notifications
  - File: app/src/main/java/com/example/underpressure/receiver/AlarmReceiver.kt
  - Update notification building logic to use `context.getString(R.string...)` for channel name, title, and message
  - Purpose: Ensure system-level notifications are localized
  - _Leverage: app/src/main/res/values/strings.xml_
  - _Requirements: 3.3_
  - _Prompt: Role: Android System Developer | Task: Update AlarmReceiver.kt to use localized strings from R.string for notification channel, title, and message following requirement 3.3 | Restrictions: Must use context.getString(), ensure placeholder %1$d for slot index is preserved | Success: Notifications appear in the system language of the user_

- [x] 8. Add Localization Integration Test
  - File: app/src/androidTest/java/com/example/underpressure/ui/LocalizationIntegrationTest.kt
  - Create a new instrumented test that verifies UI text presence using `hasText(stringResource(...))`
  - Purpose: Verify localization framework works across different locales
  - _Leverage: app/src/androidTest/java/com/example/underpressure/ExampleInstrumentedTest.kt_
  - _Requirements: Testing Strategy_
  - _Prompt: Role: QA Automation Engineer (Android) | Task: Create app/src/androidTest/java/com/example/underpressure/ui/LocalizationIntegrationTest.kt to verify that UI elements display correct localized strings following the design's testing strategy | Restrictions: Focus on key elements like screen titles and FAB content descriptions, use Compose UI Test framework | Success: Test passes and confirms resource resolution works for the default locale_
