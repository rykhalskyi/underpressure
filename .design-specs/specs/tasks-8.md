# Tasks Document - Issue 8: Cell Click + Edit Dialog

- [x] 1. Create BloodPressureValidator in domain/validation/BloodPressureValidator.kt
  - File: app/src/main/java/com/example/underpressure/domain/validation/BloodPressureValidator.kt
  - Implement a utility class to parse and validate "SYS/DIA @PULSE" format.
  - Purpose: Ensure data integrity before persistence.
  - _Leverage: Requirements 2.1, 2.2_
  - _Prompt: Role: Kotlin Developer with expertise in Regex and Domain Validation | Task: Create a BloodPressureValidator class with a `validate(input: String): ValidationResult` method. It should use Regex to verify the format `(\d+)/(\d+)\s*@\s*(\d+)`. | Restrictions: No Android dependencies (pure Kotlin), follow project naming conventions. | Success: Validator correctly identifies valid and invalid strings with clear error messages._

- [x] 2. Create unit tests for BloodPressureValidator
  - File: app/src/test/java/com/example/underpressure/domain/validation/BloodPressureValidatorTest.kt
  - Write tests for various input scenarios (valid, missing parts, non-numeric, extra spaces).
  - Purpose: Verify validation logic correctness.
  - _Leverage: Requirements 2.1, 2.2_
  - _Prompt: Role: QA Engineer specializing in JUnit 4 | Task: Implement comprehensive unit tests for BloodPressureValidator covering edge cases like "120/80 @72", "120/80", "abc/80 @72", and empty strings. | Restrictions: Use JUnit 4. | Success: 100% pass rate for all validation scenarios._

- [x] 3. Define MeasurementDialogState and update TableUiState
  - File: app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt
  - Add `MeasurementDialogState` data class and include it in `TableUiState`.
  - Purpose: Provide state container for the Edit Dialog.
  - _Leverage: Design Section "Data Models"_
  - _Prompt: Role: Android Developer specializing in State Management | Task: Define `MeasurementDialogState` with fields: `isOpen: Boolean`, `date: String`, `slotIndex: Int`, `initialValue: String`, `existingMeasurementId: Long?`. Add `dialogState: MeasurementDialogState = MeasurementDialogState()` to `TableUiState`. | Restrictions: Maintain immutability of data classes. | Success: UI State successfully holds dialog-related data._

- [x] 4. Update MeasurementTableViewModel with dialog logic
  - File: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt
  - Add methods `onCellClicked`, `onDialogDismiss`, and `onSaveMeasurement`.
  - Purpose: Handle interaction events and orchestrate data persistence.
  - _Leverage: Design Section "Components and Interfaces", Requirements 1, 3_
  - _Prompt: Role: Android ViewModel Expert | Task: Implement `onCellClicked` to populate `dialogState`, `onDialogDismiss` to reset it, and `onSaveMeasurement` to parse input via `BloodPressureValidator` and call `measurementRepository.saveMeasurement` or `updateMeasurement`. | Restrictions: Use `viewModelScope` for repository calls, update state reactively. | Success: ViewModel correctly manages dialog visibility and handles data saving._

- [x] 5. Create MeasurementEditDialog Composable
  - File: app/src/main/java/com/example/underpressure/ui/table/components/MeasurementEditDialog.kt
  - Implement a Material 3 `AlertDialog` with a `TextField` and "Save"/"Cancel" buttons.
  - Purpose: Provide the UI for measurement entry.
  - _Leverage: Requirements 1.2, 1.3, 2, 3.3_
  - _Prompt: Role: Jetpack Compose UI Developer | Task: Create a stateless `MeasurementEditDialog` Composable. It should show a `TextField` with a placeholder "120/80 @72", validate input on the fly, and show error states. | Restrictions: Follow Material 3 guidelines, use `stringResource` for all text. | Success: Dialog appears with correct initial data and provides real-time feedback._

- [x] 6. Update TableCell and DayRow to handle clicks
  - File: app/src/main/java/com/example/underpressure/ui/table/components/DayRow.kt
  - Add `onCellClick: (slotIndex: Int) -> Unit` callback to `DayRow` and `TableCell`.
  - Purpose: Enable user interaction with table cells.
  - _Leverage: Requirement 1.1_
  - _Prompt: Role: Jetpack Compose UI Developer | Task: Modify `DayRow` to accept an `onCellClick` lambda. Apply `Modifier.clickable` to each `TableCell` (except the date label) and pass the `slotIndex`. | Restrictions: Ensure the clickable area is appropriate for touch targets. | Success: Clicking a cell triggers the callback with the correct index._

- [x] 7. Integrate Dialog into MeasurementTableScreen
  - File: app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt
  - Display `MeasurementEditDialog` when `uiState.dialogState.isOpen` is true.
  - Purpose: Complete the UI wiring for the feature.
  - _Leverage: Requirements 1, 3.2_
  - _Prompt: Role: Android Developer | Task: Update `MeasurementTableScreen` to observe `uiState.dialogState` and show `MeasurementEditDialog`. Pass ViewModel methods to the dialog's callbacks. | Restrictions: Ensure no logic is placed in the Composable itself. | Success: User can open, use, and close the dialog from the main screen._

- [x] 8. Add UI test for Cell Click and Dialog Behavior
  - File: app/src/androidTest/java/com/example/underpressure/ui/table/MeasurementTableScreenTest.kt
  - Write an instrumented test to simulate cell click, input, and save.
  - Purpose: Ensure end-to-end functionality of the feature.
  - _Leverage: Requirements (All), Design Section "Testing Strategy"_
  - _Prompt: Role: Android Test Engineer (Compose) | Task: Implement a Compose UI test that: 1. Finds a cell and clicks it. 2. Asserts dialog is displayed. 3. Enters valid text. 4. Clicks Save. 5. Asserts dialog is gone and table is updated. | Restrictions: Use `createComposeRule` or `createAndroidComposeRule`. | Success: UI test passes consistently._
