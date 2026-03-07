# Requirements Document - Issue #11: Settings Screen (Slot Times)

## Introduction
The Settings Screen allows users to configure their measurement schedule by defining up to four specific time slots per day. This customization ensures that the application's tracking and reporting align with each user's personal routine and medical recommendations.

## Alignment with Product Vision
By allowing users to define their own measurement slots, "UnderPressure" provides a personalized health tracking experience that adapts to the user's lifestyle, improving the accuracy and relevance of their cardiovascular data.

## Requirements

### Requirement 1: Slot Configuration Interface
**User Story:** As a user, I want to configure my daily measurement slots, so that the application correctly organizes my data throughout the day.

#### Acceptance Criteria
1. WHEN the user opens the Settings screen, THEN the system SHALL display four time slots (1, 2, 3, and 4).
2. WHEN a slot is displayed, THEN the system SHALL show its number, a TimePicker for its time, and a toggle for its active state.
3. IF the first slot is displayed, THEN the system SHALL NOT allow it to be disabled (it is always active).
4. IF the first slot is initialized for the first time, THEN the system SHALL set its default time to 07:00.
5. WHEN the user interacts with slots 2, 3, or 4, THEN the system SHALL allow them to be enabled or disabled.
6. WHEN the user clicks on a slot's time, THEN the system SHALL open a TimePicker to allow changing the time.

### Requirement 2: Data Persistence
**User Story:** As a user, I want my slot configurations to be saved, so that I don't have to reconfigure them every time I open the app.

#### Acceptance Criteria
1. WHEN the user changes a slot's time or active state, THEN the system SHALL persist these changes to the local database.
2. WHEN the application restarts, THEN the system SHALL load the persisted slot configurations and reflect them in the UI.
3. WHEN slot configurations change, THEN the column headers in the main measurement table SHALL update dynamically to reflect the new times.

### Requirement 3: Integration and Accessibility
**User Story:** As a user, I want to easily access the settings from the main screen, so that I can quickly adjust my configuration.

#### Acceptance Criteria
1. WHEN the user is on the main measurement table screen, THEN the system SHALL provide a menu button or icon to navigate to the Settings screen.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility**: The Settings UI logic SHALL be separated from data orchestration via a `SettingsViewModel`.
- **Modular Design**: Time slot configuration components SHALL be implemented as reusable Composables.
- **Data Layer**: Slot configurations SHALL be managed by a `SettingsRepository` and stored using Room or DataStore.

### Performance
- Settings screen load time: < 300ms.
- Persistence operations SHALL be performed asynchronously on a background thread.

### Reference Steering Documents
- **Structure**: Follows `app/src/main/java/com/example/underpressure/ui/` for UI and `com.example.underpressure.data/` for persistence.
- **Tech**: Uses Jetpack Compose `TimePicker` and Room/DataStore for storage.
- **Codestyle**: Adheres to `Modifier` parameter placement and `StateFlow` management in ViewModels.
