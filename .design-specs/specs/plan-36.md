# Implementation Plan - 36: Simplify user input

## Overview
Simplify measurement input by supporting multiple formats (spaces, commas, slashes) and making the pulse value optional. Added validation rules for realistic ranges and logical consistency (systolic > diastolic).

## Steering Document Alignment

### Technical Standards (tech.md)
- Adheres to MVVM and Clean Architecture by centralizing validation in the Domain layer.
- Uses localized strings for all user-facing error messages.

### Coding Conventions (codestyle.md)
- Uses sealed classes for type-safe error handling in the UI.
- Maintains single responsibility by isolating parsing logic in `BloodPressureValidator`.

### Project Structure (structure.md)
- Enhances existing files in `domain/validation/`, `ui/table/`, and `data/export/` to support optional pulse values.

## Technical Analysis

### Impacted Files
- `app/src/main/java/com/otakeessen/underpressure/domain/validation/BloodPressureValidator.kt` (Logic update)
- `app/src/main/java/com/otakeessen/underpressure/ui/table/MeasurementTableViewModel.kt` (State management)
- `app/src/main/java/com/otakeessen/underpressure/ui/table/components/MeasurementEditDialog.kt` (UI feedback)
- `app/src/main/java/com/otakeessen/underpressure/ui/chart/ChartViewModel.kt` (Skip optional pulse in charts)
- `app/src/main/java/com/otakeessen/underpressure/data/export/TableExportManager.kt` (Export formatting)
- `app/src/main/res/values/strings.xml` (Localized messages)
- `app/src/main/res/values-de/strings.xml`
- `app/src/main/res/values-uk/strings.xml`
- `app/src/test/java/com/otakeessen/underpressure/domain/validation/BloodPressureValidatorTest.kt` (Unit tests)

### Data Model Changes
- No changes to `MeasurementEntity`. Pulse value `0` will be used to represent a missing pulse, as the valid range is 30-300.

### API Changes
- `BloodPressureValidator.validate` will return a more detailed `ValidationResult.Error` sealed class.
- `ValidationResult.Success` will contain an optional `pulse: Int?`.

## Implementation Strategy

1.  **Domain Layer (Validator):**
    -   Update `ValidationResult` to include specific error types: `EmptyInput`, `InvalidFormat`, `IncorrectMeasurements`, `InvalidNumbers`.
    -   Implement a flexible Regex: `^(\d{2,3})[/\s,]+(\d{2,3})(?:[/\s,@]+(\d{2,3}))?\s*$`.
    -   Add range checks: Sys (40-300), Dia (20-200), Pulse (30-300).
    -   Add logical check: Systolic > Diastolic.
2. **UI Layer (Dialog, Row & ViewModel):**
    -   Update `MeasurementEditDialog` to show specific error messages based on the error type returned by the validator.
    -   Update `MeasurementTableViewModel` to format the `initialValue` without a pulse if it's `0`.
    -   Update `DayRow` to omit the `@pulse` display if the pulse is `0`.
3.  **Data Layer (Export & Charts):**
    -   Modify `ChartViewModel` to filter out entries where `pulse == 0` for pulse data sets.
    -   Modify `TableExportManager` to omit the `@pulse` part in exports if `pulse == 0`.
    -   Modify `SearchDialog` / `SearchResultItem` to hide pulse if it is `0`.
4.  **Localization:**
    -   Add new strings for specific validation errors in English, German, and Ukrainian.

## Verification & Testing
-   **Unit Tests:** Update `BloodPressureValidatorTest` with cases for all supported formats, missing pulse, and boundary values for ranges.
-   **Manual Verification:**
    -   Input `120 80` and verify it saves.
    -   Input `120, 80, 70` and verify it saves.
    -   Input `80/120` and verify "Incorrect measurements" error appears.
    -   Check Chart screen to ensure no zero-pulse points are plotted.
    -   Export data and verify format is consistent with existing records.
