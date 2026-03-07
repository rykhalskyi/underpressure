# Requirements Document - Issue #7: Table UI Layout

## Introduction
The Daily Measurements Table screen is the primary interface for users to review their blood pressure and heart rate history. It provides a structured, chronologically ordered view of daily recordings, enabling users to identify patterns and trends in their health data.

## Alignment with Product Vision
This feature fulfills the core mission of "UnderPressure" by providing an intuitive and efficient way for users to monitor their cardiovascular health through a high-density, easily scannable layout of historic data.

## Requirements

### Requirement 1: Daily Measurements List
**User Story:** As a user, I want to see a list of my daily measurements in a table format, so that I can easily track my health trends over time.

#### Acceptance Criteria
1. WHEN the user navigates to the table screen, THEN the system SHALL display a scrollable list where each row represents one calendar day.
2. WHEN a day is displayed, THEN the system SHALL show 4 columns: Date (or Time), Systolic, Diastolic, and Pulse.
3. IF a day has measurements recorded, THEN the system SHALL display the most recent measurement values for that day.
4. IF a day has no measurements, THEN the system SHALL display an "empty" state or placeholder text (e.g., "-") in the value columns.
5. WHEN the current date is visible in the list, THEN the system SHALL highlight the row with a distinct background or indicator to improve visibility.

### Requirement 2: Performance and Scalability
**User Story:** As a long-term user, I want the table to remain responsive even with hundreds of entries, so that I can access my history without frustration.

#### Acceptance Criteria
1. WHEN the user scrolls through the dataset, THEN the system SHALL maintain a smooth UI performance (target 60/120 FPS).
2. IF the dataset is being loaded from the database, THEN the system SHALL handle the loading asynchronously using Kotlin Flow and Coroutines.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility**: The table UI logic SHALL be separated from data orchestration via a ViewModel.
- **Modular Design**: Row and header components SHALL be implemented as small, reusable Composables.

### Performance
- Target scroll performance: 60/120 FPS.
- Initial data load time: < 500ms for 365 entries.

### Reference Steering Documents
- **Structure**: Follows `app/src/main/java/com/example/underpressure/ui/` organization.
- **Tech**: Uses Jetpack Compose `LazyColumn` for high-performance list rendering.
- **Codestyle**: Adheres to `Modifier` parameter placement and `StateFlow` management.
