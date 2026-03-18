# Requirements Document - Issue #25: Configurable Blood Pressure Chart Activity with Sharing feature

## Introduction
The Charting feature allows users to visualize their blood pressure and pulse data over time through interactive, configurable line charts. This feature provides insights into health trends and enables users to share their data with healthcare professionals or personal contacts through standard Android sharing mechanisms.

## Alignment with Product Vision
"UnderPressure" aims to provide users with a comprehensive and personalized health tracking experience. By offering advanced visualization and sharing capabilities, the app empowers users to take control of their cardiovascular health, identify trends, and facilitate communication with their medical providers.

## Requirements

### Requirement 1: Data Visualization and Default Configuration
**User Story:** As a user, I want to see my blood pressure chart immediately upon opening the chart screen, so that I can quickly review my recent data.

#### Acceptance Criteria
1. WHEN the user opens the Chart screen, THEN the system SHALL load data from the existing measurements table.
2. WHEN the Chart screen is displayed, THEN the system SHALL automatically render the chart using default settings (all active slots, SYS and DIA measurement types).
3. WHEN the chart is rendered, THEN the system SHALL use distinct colors for each slot and follow the styling rules (SYS: thick solid, DIA: normal solid).

### Requirement 2: Configuration and Slot Selection
**User Story:** As a user, I want to be able to change which data is displayed in the chart, so that I can focus on specific metrics or time periods.

#### Acceptance Criteria
1. WHEN the user clicks the "Configure" button on the Chart screen, THEN the system SHALL display a configuration interface (e.g., a dialog or bottom sheet).
2. WHEN the configuration interface is open, THEN the system SHALL allow the user to:
    - Select which slots (1-4) are displayed.
    - Select measurement types (systolic, diastolic, pulse).
    - Choose a date range (`fromDate` and `toDate`).
3. WHEN the user applies the changes, THEN the system SHALL re-render the chart according to the new settings.
4. IF no slots are selected, THEN the system SHALL prevent chart generation and notify the user.

### Requirement 3: Image Export and Sharing
**User Story:** As a user, I want to export and share my chart as an image, so that I can easily provide my health data to others.

#### Acceptance Criteria
1. WHEN the user clicks the "Share" button, THEN the system SHALL render the current chart to a bitmap.
2. WHEN the bitmap is generated, THEN the system SHALL export it as a PNG file stored in the application's cache directory.
3. WHEN the PNG file is ready, THEN the system SHALL open the Android standard share sheet (ACTION_SEND) with the image attached and MIME type `image/png`.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: Chart rendering logic SHALL be isolated from data retrieval and UI state management via a `ChartViewModel`.
- **Modular Design**: Slot and measurement selection components SHALL be implemented as reusable Jetpack Compose elements.
- **Dependency Management**: Use `MPAndroidChart` for rendering, integrated through a `Compose` wrapper (e.g., `AndroidView`).

### Performance
- Chart rendering time for up to 1 year of data: < 500ms.
- Image export and share sheet opening: < 1 second.

### Security
- Use `FileProvider` to securely share the exported image with other applications.
- Temporary images stored in `cacheDir` SHALL be accessible only to the app and its `FileProvider`.

### Reliability
- The chart rendering SHALL handle missing data points gracefully.
- The sharing mechanism SHALL handle cases where no external apps are available to receive the intent.

### Usability
- The chart SHALL be responsive and support pinch-to-zoom and panning.
- The configuration and share actions SHALL be clearly visible and accessible.
- Labels and legends SHALL be legible and consistent with Material 3 design.
