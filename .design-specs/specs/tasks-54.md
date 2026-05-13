# Atomic Tasks - Issue 54: Improve Measurement Entry Dialog UI/UX

## Task 1. Update Domain Validation Logic
- **File**: `app/src/main/java/com/otakeessen/underpressure/domain/validation/BloodPressureValidator.kt`
- **Description**: Refactor `ValidationResult.Error` to include specific error types (`InvalidFormat`, `LogicalError`, `RangeError`) and update `BloodPressureValidator.validate()` to return them.
- **Leverage**: `BloodPressureValidator.kt`
- **Prompt**: 
  Role: Kotlin Developer specializing in Domain Logic.
  Task: Update `BloodPressureValidator.kt`. 
  1) Update `ValidationResult.Error` sealed class with: `InvalidFormat`, `LogicalError`, `RangeError`. 
  2) In `validate(input: String)`, return `LogicalError` when `sys <= dia`.
  3) Return `RangeError` when values (sys, dia, pulse) are outside human range.
  4) Ensure `InvalidFormat` is returned for regex mismatch.
  Restrictions: Pure Kotlin, no Android dependencies.
  Success: `validate()` returns specific error types for different failure scenarios.

## Task 2. Add Localized Strings
- **File**: `app/src/main/res/values/strings.xml`
- **Description**: Add new strings for refined error messages and update the slot info string.
- **Leverage**: `strings.xml`
- **Prompt**:
  Role: Android Developer.
  Task: Add the following strings to `strings.xml`:
  - `error_syntax_format`: "Invalid format. Use: 120/80 @72"
  - `error_logic_sys_dia`: "Systolic must be higher than Diastolic"
  - `error_range_out_of_human`: "Value outside human range (e.g., 40-300)"
  - Update `dialog_measurement_slot_info` to `%1$s - Slot %2$d (%3$s)` where %3$s is the time.
  Success: All required strings are present in `strings.xml`.

## Task 3. Update Dialog State Management
- **Files**: `app/src/main/java/com/otakeessen/underpressure/ui/table/TableUiState.kt`, `app/src/main/java/com/otakeessen/underpressure/ui/table/MeasurementTableViewModel.kt`
- **Description**: Add `slotTime` to `MeasurementDialogState` and populate it in `MeasurementTableViewModel`.
- **Leverage**: `TableUiState.kt`, `MeasurementTableViewModel.kt`
- **Prompt**:
  Role: Android MVVM Developer.
  Task: 
  1) Add `val slotTime: String = ""` to `MeasurementDialogState` in `TableUiState.kt`.
  2) In `MeasurementTableViewModel.kt`, update `openDialog` to fetch the slot time. For scheduled slots, get it from `settings.slotTimes`. For anytime readings, use `now()`.
  3) Pass the fetched `slotTime` to `_dialogState.update`.
  Success: `MeasurementDialogState` contains the correct slot time when the dialog opens.

## Task 4. Implement ClassificationStatusPill
- **File**: `app/src/main/java/com/otakeessen/underpressure/ui/table/components/MeasurementEditDialog.kt`
- **Description**: Create the `ClassificationStatusPill` component and integrate it into the dialog.
- **Leverage**: `MeasurementEditDialog.kt`, `BloodPressureLevel.kt`
- **Prompt**:
  Role: Jetpack Compose Developer.
  Task: 
  1) In `MeasurementEditDialog.kt`, implement a private `@Composable ClassificationStatusPill(classification: ClassificationResult, text: String)`.
  2) Use a `Surface` with rounded corners, `classification.backgroundColor` for background, and `classification.textColor` for text.
  3) Integrate this pill into the dialog layout, ensuring it updates as the user types a valid measurement.
  Success: A visual pill appears when the input is valid, reflecting the health classification.

## Task 5. Redesign Dialog Layout and Spacing
- **File**: `app/src/main/java/com/otakeessen/underpressure/ui/table/components/MeasurementEditDialog.kt`
- **Description**: Improve the visual hierarchy and spacing of the dialog as per requirements.
- **Leverage**: `MeasurementEditDialog.kt`
- **Prompt**:
  Role: Jetpack Compose UI Developer.
  Task: 
  1) Update the header: Use `MaterialTheme.typography.titleLarge` and `FontWeight.Bold` for slot info. Use the updated `dialog_measurement_slot_info` string.
  2) Show date as secondary info (e.g., `bodyMedium` and `onSurfaceVariant` color).
  3) Increase vertical spacing between header, legend, pill, and input field using `Spacer` (8dp-16dp).
  4) Update `OutlinedTextField`: remove classification-based border color; reserve Red for `isError`. Use the new localized error strings in `supportingText`.
  Success: Dialog has improved hierarchy, spacing, and clear error messaging.

## Task 6. Update Unit Tests
- **File**: `app/src/test/java/com/otakeessen/underpressure/domain/validation/BloodPressureValidatorTest.kt`
- **Description**: Verify the new specific error types in the validator.
- **Leverage**: `BloodPressureValidatorTest.kt`
- **Prompt**:
  Role: Android Test Engineer.
  Task: Update `BloodPressureValidatorTest.kt` to check for `LogicalError` (e.g., "80/120") and `RangeError` (e.g., "500/80").
  Success: Tests accurately verify all error types.
