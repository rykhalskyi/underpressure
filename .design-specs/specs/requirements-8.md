# Requirements Document - Issue 8: Cell Click + Edit Dialog

## Introduction

This feature enables users to add or update blood pressure measurements directly from the main measurement table by clicking on a specific time slot (cell). This streamlines data entry and ensures health tracking is quick and intuitive.

## Alignment with Product Vision

While a separate `product.md` is not present, this feature directly supports the core mission of "UnderPressure" to provide an efficient and reliable way for users to monitor their blood pressure trends over time, as outlined in the project's foundational goals.

## Requirements

### Requirement 1: Trigger Edit Dialog

**User Story:** As a user, I want to click on a table cell (either empty or with an existing value), so that I can open a measurement entry dialog.

#### Acceptance Criteria

1. WHEN the user clicks on any cell within a `DayRow` (excluding the date label), THEN the system SHALL display the Edit Measurement Dialog.
2. IF the clicked cell already contains a measurement, THEN the system SHALL prefill the input field with the existing value in the format "SYS/DIA @PULSE".
3. IF the clicked cell is empty, THEN the system SHALL display an empty input field with a placeholder indicating the "SYS/DIA @PULSE" format.

### Requirement 2: Measurement Entry and Validation

**User Story:** As a user, I want the system to validate my input, so that I don't accidentally save incorrect or malformed data.

#### Acceptance Criteria

1. WHEN the user types in the input field, THEN the system SHALL continuously validate the format against the pattern `[Systolic]/[Diastolic] @[Pulse]` (e.g., `120/80 @72`).
2. IF the input does not match the required pattern, THEN the system SHALL display a validation error message and disable the "Save" button.
3. IF the input is valid, THEN the system SHALL enable the "Save" button.

### Requirement 3: Persistence and UI Update

**User Story:** As a user, I want my changes to be saved and reflected in the table immediately, so that I can see the updated data without manual refresh.

#### Acceptance Criteria

1. WHEN the user clicks the "Save" button with valid input, THEN the system SHALL persist the new or updated measurement to the database.
2. WHEN the "Save" operation is successful, THEN the system SHALL close the dialog and update the table UI immediately.
3. WHEN the user clicks the "Cancel" button or clicks outside the dialog, THEN the system SHALL close the dialog without saving any changes.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: Validation logic should be extracted into a dedicated utility or domain-level validator.
- **Modular Design**: The Edit Dialog should be a reusable Composable component.
- **Clear Interfaces**: The ViewModel should expose clear methods for opening the dialog and saving validated input.
- **Alignment**: Follows `.design-specs/structure.md` by placing UI components in `ui/table/components` or `ui/common`.

### Performance
- The dialog should appear instantaneously (< 100ms) upon cell click.
- Table updates after saving should be reactive and fluid, leveraging `StateFlow`.

### Reliability
- Data must be persisted to the Room database before the UI reflects the change to ensure consistency.

### Usability
- The input field should automatically request focus when the dialog opens.
- The numeric keyboard should be preferred for input efficiency.
- Follows Material 3 design standards as specified in `.design-specs/tech.md`.

## Alignment with Steering Documents
- **Tech Stack**: Uses Jetpack Compose, ViewModels, and StateFlow as mandated by `.design-specs/tech.md`.
- **Code Style**: Adheres to SOLID principles and Compose best practices (e.g., Modifier placement) defined in `.design-specs/codestyle.md`.
- **Project Structure**: New components and logic will follow the layered architecture defined in `.design-specs/structure.md`.
