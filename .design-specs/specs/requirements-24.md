# Requirements Document - Issue 24: Porting Python OpenCV 7-Segment Recognition

## Introduction

The current OCR solution in `underpressure` relies on Google ML Kit, which performs well on standard printed text but often fails with digital seven-segment displays typical of many blood pressure monitors. This project involves porting a proven Python-based OpenCV pipeline to the Android platform using the OpenCV Android SDK. This will provide a robust, purely geometric, and segment-based recognition system that doesn't rely on general-purpose OCR engines.

## Alignment with Product Vision

This feature directly supports the goal of being a "Blood pressure tracker for Android" by enhancing the accuracy and reliability of automated measurement entry from digital monitors. By providing a specialized recognizer for 7-segment displays, we improve the user experience for those using older or common digital BP monitors.

## Requirements

### Requirement 1: Native OpenCV Image Processing

**User Story:** As a developer, I want to use a native OpenCV pipeline for image processing, so that I can achieve high performance and precise geometric control over digit recognition.

#### Acceptance Criteria

1. WHEN a Bitmap or CameraX frame is provided THEN the system SHALL convert it to an OpenCV `Mat` for processing.
2. IF the input image is color THEN the system SHALL convert it to grayscale before starting the processing pipeline.
3. WHEN preprocessing is performed THEN the system SHALL apply Gaussian blur (5x5 kernel) and Canny edge detection (thresholds 50, 200).

### Requirement 2: Display Area Detection and Perspective Correction

**User Story:** As a user, I want the app to automatically find the display on my BP monitor, so that I don't have to perfectly align the camera.

#### Acceptance Criteria

1. WHEN detecting the display THEN the system SHALL find external contours, sort them by area, and select the largest 4-point contour.
2. IF a valid 4-point contour is found THEN the system SHALL apply a four-point perspective transform to normalize the display into a top-down view.

### Requirement 3: 7-Segment Digit Segmentation and Recognition

**User Story:** As a user, I want the app to accurately read the digits on the 7-segment display, so that my BP measurements are recorded correctly.

#### Acceptance Criteria

1. WHEN processing the warped display THEN the system SHALL apply binary threshold (value 33, inverse) and morphological opening (ellipse 1x5 kernel).
2. IF the image is processed THEN the system SHALL resize it to a height of 500px and apply an additional Gaussian blur (7x7).
3. WHEN detecting digit contours THEN the system SHALL filter by width (20-125px) and height (70-138px).
4. IF digit candidates are found THEN the system SHALL group them by height (within ±3 pixels) and sort them by x-coordinate.
5. WHEN recognizing segments THEN the system SHALL determine segment activation if more than 25% of its pixels are "on".
6. IF a digit's width is less than 25% of its height THEN it SHALL be classified as "1" automatically.
7. WHEN all digits are recognized THEN the system SHALL group them into rows (within ±3 pixels y-coordinate) and return the recognized numeric values.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: The OpenCV recognition logic should be encapsulated in a dedicated `SevenSegmentRecognizer` class.
- **Modular Design**: The pipeline should be built as a sequence of discrete operations, allowing for easier debugging and potential future tuning.
- **Clear Interfaces**: The recognizer should expose a clean interface: `recognize(bitmap: Bitmap): String`.

### Performance
- The system SHALL process a single frame in less than 100ms on a mid-range Android device.

### Reliability
- The recognizer must produce identical digit outputs as the Python reference implementation for the same input test images.
- The system must be robust to minor lighting variations through the use of thresholding and morphological operations.

### Usability
- (Optional) The system SHALL support a debug mode that overlays bounding boxes and segment states on the original image for visualization.

## Testing

Test using the same image dataset used for the Python prototype. Specifically, verify recognition on the following authoritative test cases:

- **Case 1 (7.jpg):**
  - Expected output: Row 1: 123, Row 2: 81, Row 3: 92
- **Case 2 (8.jpg):**
  - Expected output: Row 1: 141, Row 2: 90, Row 3: 85

Validation criteria:
- The Android implementation must produce identical digit outputs as the Python reference for these files.
- Correct digit recognition (including special width handling for '1').
- Stable contour detection and correct row grouping.
- Robustness to minor lighting variation.

## Reference Steering Documents
- **Structure**: Implementation will be placed in `com.example.underpressure.ocr` following `structure.md`.
- **Tech**: Uses OpenCV Android SDK as specified in `tech.md` (though not yet listed, it's a project-specific addition).
- **Codestyle**: Will follow Kotlin 2.0 and Jetpack Compose state management patterns if UI is added for debugging.
