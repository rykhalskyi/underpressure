# Implementation Plan - Issue 54: Improve Measurement Entry Dialog UI/UX

## Overview
This plan outlines the enhancements for the Measurement Entry Dialog to improve its UI/UX. Key improvements include refined error messaging, a better visual hierarchy in the header, and a dedicated status pill for health classification.

## Steering Alignment
- **Architecture**: MVVM with Clean Architecture. Maintain separation between domain logic (`BloodPressureValidator`) and UI (`MeasurementEditDialog`).
- **UI/UX**: Material 3 guidelines. Ensure localized strings and proper spacing.
- **State Management**: Update `MeasurementDialogState` in `TableUiState.kt` and manage it in `MeasurementTableViewModel.kt`.

## Impact

### Files to Modify
- `app/src/main/java/com/otakeessen/underpressure/domain/validation/BloodPressureValidator.kt`: Add specific error types.
- `app/src/main/res/values/strings.xml`: Add localized strings.
- `app/src/main/java/com/otakeessen/underpressure/ui/table/TableUiState.kt`: Update `MeasurementDialogState` with `slotTime`.
- `app/src/main/java/com/otakeessen/underpressure/ui/table/MeasurementTableViewModel.kt`: Populate `slotTime` in the dialog state.
- `app/src/main/java/com/otakeessen/underpressure/ui/table/components/MeasurementEditDialog.kt`: Redesign dialog UI and implement `ClassificationStatusPill`.

### Data/API Changes
- `ValidationResult.Error` will have more specific subclasses: `InvalidFormat`, `LogicError`, `RangeError`.
- `MeasurementDialogState` will include a `slotTime: String` field.

## Strategy

### 1. Domain Logic Updates
- Refactor `ValidationResult.Error` to include:
  - `InvalidFormat` (Syntax error)
  - `LogicalError` (Systolic <= Diastolic)
  - `RangeError` (Values outside human range)
- Update `BloodPressureValidator.validate()` to return these specific errors instead of the generic `IncorrectMeasurements`.

### 2. Localization
- Add the following strings to `strings.xml`:
  - `error_syntax_format`: "Invalid format. Use: 120/80 @72"
  - `error_logic_sys_dia`: "Systolic must be higher than Diastolic"
  - `error_range_out_of_human`: "Value outside human range (e.g., 40-300)"
  - Update `dialog_measurement_slot_info` to better support time inclusion.

### 3. State Management
- Add `slotTime: String = ""` to `MeasurementDialogState`.
- In `MeasurementTableViewModel.openDialog()`, fetch the slot time from settings or `now()` (for flexible mode) and update the state.

### 4. UI Redesign
- **Header**: 
  - Increase font size and weight for slot information.
  - Display as "Slot 1 (08:00)" or similar.
  - Keep date secondary but visible.
- **ClassificationStatusPill**:
  - Create a new internal `@Composable` in `MeasurementEditDialog.kt`.
  - It should be a rounded pill with a background color from `ClassificationResult.backgroundColor` and text color from `ClassificationResult.textColor`.
- **Input Field**:
  - Remove classification-based border color from `OutlinedTextField` to reserve Red strictly for errors.
  - Ensure `supportingText` shows specific error messages.
- **Layout**:
  - Increase vertical spacing between elements using `Spacer`.

## Verification
- Unit tests for `BloodPressureValidator` to verify new error types.
- UI manual testing for layout, status pill updates, and localization.
