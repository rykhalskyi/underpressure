# Requirements Document - Issue #18: Add Measurements Action Button

## Introduction
The Add Measurements Action Button provides a quick and intuitive way for users to record their blood pressure and pulse readings. By automatically identifying the current active time slot, the system reduces user effort and ensures data is recorded accurately according to the user's schedule.

## Alignment with Product Vision
The main goal of "UnderPressure" is to provide a seamless and user-friendly experience for tracking cardiovascular health. This feature enhances the usability of the application by streamlining the data entry process, making it easier for users to maintain a consistent tracking routine.

## Requirements

### Requirement 1: Main Action Button Interface
**User Story:** As a user, I want a clearly visible button to add my measurements, so that I can quickly record my readings when it's time.

#### Acceptance Criteria
1. WHEN the user is on the Measurement Table screen, THEN the system SHALL display a Floating Action Button (FAB) with a "+" icon.
2. IF there is an active, empty time slot within +/- 15 minutes of the current time, THEN the system SHALL enable the action button.
3. IF there are no active, empty time slots within +/- 15 minutes of the current time, THEN the system SHALL disable the action button.
4. IF there are multiple active, empty time slots within the +/- 15-minute window, THEN the system SHALL select the slot closest to the current time that is in the past.
5. WHEN the user clicks the enabled action button, THEN the system SHALL open the `MeasurementEditDialog` for the identified time slot on the current date.

### Requirement 2: Dynamic Button State
**User Story:** As a user, I want the action button to update its state automatically as time passes, so that I always know when I can record a measurement.

#### Acceptance Criteria
1. WHEN time passes and a slot becomes eligible (within the 15-minute window), THEN the system SHALL enable the action button.
2. WHEN time passes and a slot is no longer eligible (outside the 15-minute window), THEN the system SHALL disable the action button.
3. WHEN a measurement is saved to the current eligible slot, THEN the system SHALL immediately disable the action button (as the slot is no longer empty).

### Requirement 3: Data Entry and Persistence
**User Story:** As a user, I want the measurements I enter to be saved to the correct time slot, so that my history is accurate.

#### Acceptance Criteria
1. WHEN the `MeasurementEditDialog` is opened from the action button, THEN it SHALL be pre-configured for the identified time slot and today's date.
2. WHEN the user saves measurements in the dialog, THEN the system SHALL persist the data to the local database for that specific slot and date.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility**: The logic for determining the active slot SHALL be encapsulated in the `MeasurementTableViewModel`.
- **Modular Design**: The Action Button SHALL be integrated into the `MeasurementTableScreen` using the standard Material 3 FAB pattern.
- **UI State**: The enabled/disabled state of the button and the target slot index SHALL be part of the `TableUiState`.

### Performance
- The button state SHALL update reactively without noticeable delay as the current time or measurement data changes.
- UI state calculations SHALL NOT block the main thread.

### Reference Steering Documents
- **Structure**: Follows `.design-specs/structure.md`, placing UI logic in `app/src/main/java/com/example/underpressure/ui/table/`.
- **Tech**: Follows `.design-specs/tech.md`, using Jetpack Compose and `StateFlow`.
- **Codestyle**: Follows `.design-specs/codestyle.md`, ensuring proper `Modifier` usage and `ViewModel` patterns.
