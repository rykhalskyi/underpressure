# Requirements Document - Camera Integration and OCR (Issue #14)

## Introduction

This feature enables users to capture blood pressure readings directly from their monitor's screen using the device camera. By integrating CameraX and Google ML Kit's Text Recognition (OCR), the app will automatically extract systolic, diastolic, and pulse values, reducing manual data entry errors and improving user convenience.

## Alignment with Product Vision

This feature supports the core vision of "underpressure" as a fast and reliable blood pressure tracker. Automating data entry through OCR simplifies the tracking process, encourages regular logging, and ensures data accuracy by minimizing human input errors. It aligns with the goal of providing a modern, user-friendly experience for health monitoring.

## Requirements

### Requirement 1: Camera Capture Integration

**User Story:** As a user, I want to open the camera from the measurement entry dialog, so that I can take a picture of my blood pressure monitor's screen.

#### Acceptance Criteria

1. WHEN the user clicks the "Read from Camera" button in the `MeasurementEditDialog` THEN the system SHALL request camera permissions (if not already granted).
2. IF camera permission is granted THEN the system SHALL launch the camera interface using CameraX.
3. WHEN the user captures an image THEN the system SHALL process the image in-memory and immediately close the camera interface.
4. IF the user cancels the camera interface THEN the system SHALL return to the `MeasurementEditDialog` without changes.

### Requirement 2: On-Device OCR Processing

**User Story:** As a user, I want the app to automatically recognize the numbers on the screen picture, so that I don't have to type them manually.

#### Acceptance Criteria

1. WHEN an image is captured THEN the system SHALL use Google ML Kit's Text Recognition to identify numeric patterns.
2. IF numeric patterns matching "SYS/DIA @PULSE" format (e.g., 120, 80, 70) are found THEN the system SHALL extract these values.
3. WHEN values are extracted THEN the system SHALL format them as "SYS/DIA @PULSE" (e.g., "120/80 @70") and update the input field in `MeasurementEditDialog`.
4. IF multiple numeric candidates are found THEN the system SHALL use the most likely matches based on typical blood pressure ranges.
5. IF no valid numeric patterns are found THEN the system SHALL notify the user that recognition failed.

### Requirement 3: Data Privacy and Resource Management

**User Story:** As a user, I want my captured images to not be saved on my phone, so that my privacy is protected and storage is not wasted.

#### Acceptance Criteria

1. WHEN the camera captures an image THEN the system SHALL process it entirely in-memory as a `Bitmap` or `ImageProxy`.
2. IF the processing is complete THEN the system SHALL immediately recycle/dispose of the image data.
3. THEN the system SHALL NOT save any captured images to the device's persistent storage (Gallery, app-specific cache, etc.).

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: The OCR logic SHALL be encapsulated in a dedicated service or utility within a new `ocr` package.
- **Modular Design**: The Camera integration SHALL be reusable and decoupled from the specific `MeasurementEditDialog` UI.
- **Dependency Management**: Dependencies on ML Kit and CameraX SHALL be managed via `libs.versions.toml`.
- **Clear Interfaces**: Define a clear contract for the OCR result (e.g., a data class containing SYS, DIA, and PULSE).

### Performance
- OCR processing SHALL complete in less than 2 seconds on supported devices.
- Camera interface SHALL launch within 500ms.

### Security
- The app SHALL NOT require Internet access for OCR (on-device processing only).
- Captured image data SHALL be cleared from memory as soon as possible.

### Reliability
- The system SHALL handle low-light conditions or blurry images by providing feedback to the user or failing gracefully.

### Usability
- The "Read from Camera" button SHALL be clearly visible and accessible within the measurement entry flow.
- Feedback SHALL be provided during image processing (e.g., a loading spinner).

## Reference Steering Documents
- **structure.md**: New `ocr` package added to `app/src/main/java/com/example/underpressure/ocr`.
- **tech.md**: Integration of `CameraX` and `Google ML Kit Text Recognition`.
- **codestyle.md**: Adherence to MVVM, using `StateFlow` for OCR results and `stringResource` for all user-facing messages.
