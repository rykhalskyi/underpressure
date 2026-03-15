# Requirements Document - Issue #27: Share Table Data as ASCII Message or CSV Export

## Introduction
This feature introduces a sharing capability for blood pressure measurement data within the UnderPressure Android application. Users will be able to export their recorded history in two formats: a human-readable ASCII table (optimized for messaging apps and email) and a CSV file (for spreadsheet analysis). This addresses the need for users to communicate their health data to healthcare providers or for personal archiving and external analysis.

## Alignment with Product Vision
The core goal of UnderPressure is to provide a reliable way to track and manage blood pressure health history. Adding sharing and export capabilities enhances the utility of the logged data, transforming it from a static local log into a shareable report. This supports better health management by enabling easy data sharing with doctors, aligning with the "Sharing Capabilities" mentioned in the product vision.

## Requirements

### Requirement 1: Share Data as ASCII Message
**User Story:** As a user, I want to share my logged data as a plain text table so that I can easily send it to my doctor via messaging apps or email.

#### Acceptance Criteria
1. WHEN the user selects "Share as Message" THEN the system SHALL generate a monospaced ASCII table with padded columns for alignment.
2. The table SHALL include a "Date" column followed by columns for **only active measurement slots**.
3. **Column headers for measurement slots SHALL be the configured slot times** (e.g., "07:15", "12:00").
4. IF there are between 1 and 4 active data columns THEN the system SHALL adjust the table width accordingly (total width should remain < ~80 characters).
5. IF data is missing for a specific slot THEN the system SHALL display "—" as a placeholder.
6. WHEN the table is generated THEN the system SHALL include a header showing the date range of the exported data (e.g., "2026-03-10 → 2026-03-11").
7. WHEN the user confirms the share THEN the system SHALL open the Android share sheet with `text/plain` content.

### Requirement 2: Export Data as CSV File
**User Story:** As a user, I want to export my data as a CSV file so that I can analyze it in spreadsheet applications like Excel or Google Sheets.

#### Acceptance Criteria
1. WHEN the user selects "Export CSV" THEN the system SHALL generate a comma-separated values file.
2. The CSV SHALL include a "Date" column followed by columns for **only active measurement slots**.
3. **Column headers for measurement slots SHALL be the configured slot times** (e.g., "07:15", "12:00").
4. WHEN the file is generated THEN the system SHALL save it to the app's cache directory using the naming pattern `table_export_YYYYMMDD_HHMMSS.csv`.
5. WHEN the file is ready THEN the system SHALL share it using `FileProvider` via the Android share sheet.
6. IF the export fails (e.g., storage full) THEN the system SHALL notify the user with an appropriate error message.

### Requirement 3: Date Range Filtering
**User Story:** As a user, I want to filter my data by a date range before sharing so that I only send relevant information.

#### Acceptance Criteria
1. IF no dates are selected THEN the system SHALL include the entire table in the export/share.
2. IF a "From" date is selected THEN the system SHALL include rows starting from that date (inclusive).
3. IF a "To" date is selected THEN the system SHALL include rows up to that date (inclusive).
4. IF "From" date is greater than "To" date THEN the system SHALL show a validation error and prevent sharing.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: Logic for ASCII formatting, CSV generation, and date filtering SHALL be isolated into dedicated utility or helper classes.
- **Modular Design**: The sharing logic SHALL be decoupled from the UI components, following the Clean Architecture pattern (Domain/Data layer for logic, UI layer for triggering).
- **Clear Interfaces**: Formatting utilities SHALL have well-defined interfaces (e.g., `formatAsciiTable(headers: List<String>, rows: List<List<String>>): String`).

### Performance
- Table formatting and file generation SHALL be performed on background threads using Kotlin Coroutines to avoid blocking the UI.

### Security
- **File Sharing**: Exported files SHALL be stored in the app's cache directory and shared via `FileProvider` to adhere to Android's security best practices.
- **Data Privacy**: Only data explicitly selected by the user SHALL be shared.

### Reliability
- The system SHALL handle empty data sets gracefully by disabling the share action or notifying the user if no data is available for the selected range.

### Usability
- **Dialog Design**: The share dialog SHALL follow Material 3 guidelines, providing clear options for format selection and date range input.
- **Feedback**: The user SHALL receive feedback (e.g., a snackbar or toast) when a share action is initiated or fails.

## Reference Steering Documents
- **structure.md**: Logic implementation will be placed in the appropriate layers (e.g., `ui/table/` for UI, `domain/` for business logic).
- **tech.md**: Adheres to Kotlin 2.0, Coroutines, and Material 3 standards.
- **codestyle.md**: Follows SOLID principles and Clean Architecture.
