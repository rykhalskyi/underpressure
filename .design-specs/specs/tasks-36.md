ye# Tasks - 36: Simplify user input

- [ ] 1. Update ValidationResult and BloodPressureValidator
  - File: `app/src/main/java/com/otakeessen/underpressure/domain/validation/BloodPressureValidator.kt`
  - Update `ValidationResult` sealed class to include specific error types: `EmptyInput`, `InvalidFormat`, `IncorrectMeasurements`, `InvalidNumbers`.
  - Update regex to: `^(\d{2,3})[/\s,]+(\d{2,3})(?:[/\s,@]+(\d{2,3}))?\s*$`.
  - Implement range validation: Sys (40-300), Dia (20-200), Pulse (30-300).
  - Implement logic check: Systolic must be greater than Diastolic.
  - Make pulse optional in `ValidationResult.Success`.
  - Purpose: Provide robust and flexible validation for various input formats.
  - _Requirements: 1, 2, 3_
  - _Prompt: Role: Kotlin Developer specializing in Domain Logic and Regex | Task: Update BloodPressureValidator.kt to support new formats and optional pulse. 1) Update ValidationResult sealed class with error types: EmptyInput, InvalidFormat, IncorrectMeasurements, InvalidNumbers. 2) Update regex to `^(\d{2,3})[/\s,]+(\d{2,3})(?:[/\s,@]+(\d{2,3}))?\s*$`. 3) Implement range checks: Sys(40-300), Dia(20-200), Pulse(30-300). 4) Add check: systolic > diastolic. 5) Return Success(sys, dia, pulse?) where pulse is 0 if missing. | Restrictions: Keep logic in pure Kotlin, do not use Android dependencies. | Success: All input patterns (spaces, commas, slashes) parse correctly; range and logic errors return specific Error types; Success contains pulse or 0._

- [ ] 2. Add localized error messages
  - Files: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`, `app/src/main/res/values-uk/strings.xml`
  - Add string resources for `error_incorrect_measurements` (Sys > Dia or range violation).
  - Add string resources for other specific validation errors if needed.
  - Purpose: Provide clear, localized feedback to the user when input rules are violated.
  - _Requirements: 2_
  - _Prompt: Role: Android Developer specializing in Internationalization | Task: Add localized strings for the new validation rules in strings.xml (English), values-de/strings.xml (German), and values-uk/strings.xml (Ukrainian). Key: `error_incorrect_measurements`. EN: "Incorrect measurements", DE: "Fehlerhafte Messwerte", UK: "Некоректні вимірювання". | Restrictions: Use existing string naming conventions. | Success: Strings are correctly added to all three files and accessible via R.string._

- [ ] 3. Update MeasurementEditDialog for detailed error feedback
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/table/components/MeasurementEditDialog.kt`
  - Map `ValidationResult.Error` types to localized string resources.
  - Update `supportingText` to display the specific error message.
  - Purpose: Improve UX by showing why the input is invalid.
  - _Leverage: BloodPressureValidator.kt_
  - _Requirements: 2_
  - _Prompt: Role: Jetpack Compose Developer | Task: Update MeasurementEditDialog.kt to show specific error messages. Use a when expression on ValidationResult.Error to pick the correct R.string: InvalidFormat -> R.string.error_invalid_format, IncorrectMeasurements -> R.string.error_incorrect_measurements, etc. Update the OutlinedTextField's supportingText to show this message. | Restrictions: Maintain Material 3 styling. | Success: User sees specific error messages (e.g., "Incorrect measurements") when validation fails._

- [ ] 4. Handle optional pulse in MeasurementTableViewModel
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Update `openDialog` to format the `initialValue` without `@pulse` if pulse is 0.
  - Ensure `onSaveMeasurement` handles the optional pulse from `ValidationResult.Success`.
  - Purpose: Seamlessly transition between displayed text and stored data for optional pulse.
  - _Leverage: BloodPressureValidator.kt_
  - _Requirements: 3_
  - _Prompt: Role: Android ViewModel Developer | Task: Modify MeasurementTableViewModel.kt to handle optional pulse. In `openDialog`, if `existing.pulse == 0`, the `initialValue` should be formatted as "${it.systolic}/${it.diastolic}" (omitting pulse). In `onSaveMeasurement`, ensure the pulse from validator is correctly passed to the MeasurementEntity (using 0 if null/missing). | Restrictions: Do not change database schema. | Success: Editing a measurement without a pulse shows only "SYS/DIA" in the dialog; saving works correctly._

- [ ] 5. Update DayRow and SearchResultItem UI
  - Files: `app/src/main/java/com/otakeessen/underpressure/ui/table/components/DayRow.kt`, `app/src/main/java/com/otakeessen/underpressure/ui/table/components/SearchResultItem.kt`
  - In `DayRow.kt`, update cell text logic to omit `@pulse` or `\n@pulse` if `pulse == 0`.
  - In `SearchResultItem.kt`, update the detail text to hide pulse if it's 0 (requires a new localized string or string logic).
  - Purpose: Ensure pulse is only shown in the UI when it has been recorded.
  - _Requirements: 3_
  - _Prompt: Role: Jetpack Compose Developer | Task: Update DayRow and SearchResultItem to hide pulse when it's 0. In DayRow, if `it.pulse > 0`, use the current format; otherwise, use just `"${it.systolic}/${it.diastolic}"`. In SearchResultItem, if `measurement.pulse == 0`, show only "Systolic: X, Diastolic: Y" (might need a new string resource or manual concatenation). | Restrictions: Maintain consistent typography. | Success: Measurements with 0 pulse are displayed without the @ sign or pulse value._

- [ ] 6. Skip zero-pulse points in Blood Pressure Chart
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/chart/ChartViewModel.kt`
  - Filter `slotMeasurements` to exclude entries where `pulse == 0` when generating `entries` for `MeasurementType.PULSE`.
  - Purpose: Prevent the chart from plotting incorrect "zero" pulse values.
  - _Requirements: 3_
  - _Prompt: Role: Data Visualization Developer | Task: Update ChartViewModel.kt to skip pulse points where value is 0. In the dataset generation loop, when `type == MeasurementType.PULSE`, filter `slotMeasurements` to only include those where `m.pulse > 0` before creating `Entry` objects. | Restrictions: Only filter pulse data, do not filter systolic/diastolic. | Success: Charts no longer show pulse points for measurements where pulse was not recorded._

- [ ] 7. Update TableExportManager for optional pulse
  - File: `app/src/main/java/com/otakeessen/underpressure/data/export/TableExportManager.kt`
  - In `prepareExportData`, update cell value formatting to omit `@pulse` if `pulse == 0`.
  - Purpose: Ensure exported files (CSV/ASCII) are clean and accurate when pulse is missing.
  - _Requirements: 3_
  - _Prompt: Role: Data Export Specialist | Task: Update TableExportManager.kt to handle optional pulse in exports. In `prepareExportData`, change the `cellValue` logic: if `it.pulse > 0` use `"${it.systolic}/${it.diastolic}@${it.pulse}"`, otherwise use `"${it.systolic}/${it.diastolic}"`. | Restrictions: Maintain consistency between ASCII and CSV exports. | Success: Exported files correctly show measurements with or without pulse based on data._

- [ ] 8. Update and expand Unit Tests
  - File: `app/src/test/java/com/otakeessen/underpressure/domain/validation/BloodPressureValidatorTest.kt`
  - Add test cases for new formats: `120 80 70`, `120,80,70`, `120/80/70`, `120 80`.
  - Add test cases for range violations and Sys <= Dia.
  - Add test cases for optional pulse.
  - Purpose: Ensure reliability of the new parsing and validation logic.
  - _Leverage: BloodPressureValidator.kt_
  - _Requirements: 1, 2, 3_
  - _Prompt: Role: QA Engineer specializing in Unit Testing | Task: Expand BloodPressureValidatorTest.kt to cover all new requirements. Add tests for: 1) Various delimiters (space, comma, slash). 2) Optional pulse (only 2 numbers). 3) Boundary values for all ranges. 4) Logic check (systolic <= diastolic returns Error). | Restrictions: Use JUnit and MockK if needed. | Success: All tests pass, covering positive and negative scenarios for new features._
