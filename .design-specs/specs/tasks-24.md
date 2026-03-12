# Tasks Document - Issue 24: Porting Python OpenCV 7-Segment Recognition

- [x] 1. Add OpenCV Android SDK dependency
  - File: `gradle/libs.versions.toml`, `app/build.gradle.kts`
  - Add OpenCV version and library to Version Catalog
  - Include OpenCV implementation in the `app` module's dependencies
  - Purpose: Enable native image processing capabilities
  - _Leverage: `gradle/libs.versions.toml`_
  - _Requirements: 1.1_
  - _Prompt: Role: Android Build Engineer | Task: Add OpenCV Android SDK to the project using Version Catalog and update app build script. | Success: Gradle syncs successfully with OpenCV dependency._

- [x] 2. Create `SevenSegmentRecognizer` class and basic structure
  - File: `app/src/main/java/com/example/underpressure/ocr/SevenSegmentRecognizer.kt`
  - Implement class skeleton with `recognize(Bitmap): String?` and `recognize(Mat): String?`
  - Add `SEGMENT_MAP` constant for digit lookup
  - Purpose: Establish the core recognizer component
  - _Leverage: `OcrResult.kt` (for reference)_
  - _Requirements: NFR (Architecture)_
  - _Prompt: Role: Kotlin Developer | Task: Create `SevenSegmentRecognizer` class in the OCR package with basic method signatures and the `SEGMENT_MAP` lookup table from the design. | Success: Class compiles and contains required constant._

- [x] 3. Implement image preprocessing stage
  - File: `app/src/main/java/com/example/underpressure/ocr/SevenSegmentRecognizer.kt`
  - Implement `preprocess(Mat): Mat` applying grayscale conversion, 5x5 Gaussian blur, and Canny edge detection (50, 200)
  - Purpose: Prepare image for contour detection
  - _Leverage: `test_image/test.py` (authoritative reference)_
  - _Requirements: 1.3_
  - _Prompt: Role: Computer Vision Engineer | Task: Implement `preprocess` method in `SevenSegmentRecognizer` exactly following the Python reference parameters. | Success: Grayscale image is correctly blurred and edged._

- [x] 4. Implement display detection and perspective transform
  - File: `app/src/main/java/com/example/underpressure/ocr/SevenSegmentRecognizer.kt`
  - Implement `detectDisplay(Mat): Mat?` logic to find the largest 4-point contour and apply perspective transform
  - Purpose: Normalize the display area
  - _Leverage: `test_image/test.py` (authoritative reference)_
  - _Requirements: 2.1, 2.2_
  - _Prompt: Role: Computer Vision Engineer | Task: Port `four_point_transform` and display detection logic from Python to `SevenSegmentRecognizer`. | Success: Correctly warped display Mat is returned._

- [x] 5. Implement thresholding and digit segmentation
  - File: `app/src/main/java/com/example/underpressure/ocr/SevenSegmentRecognizer.kt`
  - Implement thresholding (33, inverse), morphological opening (1x5 ellipse), resizing (height 500px), and Gaussian blur (7x7)
  - Purpose: Isolate digits from the background
  - _Leverage: `test_image/test.py` (authoritative reference)_
  - _Requirements: 3.1, 3.2_
  - _Prompt: Role: Computer Vision Engineer | Task: Implement the digit isolation pipeline in `SevenSegmentRecognizer` following requirements 3.1 and 3.2. | Success: Resulting Mat shows clear isolated digits._

- [x] 6. Implement digit contour filtering and grouping
  - File: `app/src/main/java/com/example/underpressure/ocr/SevenSegmentRecognizer.kt`
  - Implement logic to find contours, filter by size (width 20-125, height 70-138), group by height (±3px), and group into rows (±3px y-coord)
  - Purpose: Identify and order digit candidates
  - _Leverage: `test_image/test.py` (authoritative reference)_
  - _Requirements: 3.3, 3.4, 3.7_
  - _Prompt: Role: Kotlin Developer | Task: Port the contour filtering and height/row grouping logic from Python to `SevenSegmentRecognizer`. | Success: Digit contours are correctly grouped and ordered._

- [x] 7. Implement segment classification logic
  - File: `app/src/main/java/com/example/underpressure/ocr/SevenSegmentRecognizer.kt`
  - Implement `classifyDigit(Mat): Int` using the 7-segment regions and 0.25 ratio threshold
  - Include special width check for digit '1' (width < 0.25 * height)
  - Purpose: Map image regions to numeric digits
  - _Leverage: `test_image/test.py` (authoritative reference)_
  - _Requirements: 3.5, 3.6_
  - _Prompt: Role: Computer Vision Engineer | Task: Implement segment activation detection and digit mapping in `SevenSegmentRecognizer`. | Success: ROI is correctly classified as a digit._

- [x] 8. Update `BloodPressureOcrManager` to use OpenCV
  - File: `app/src/main/java/com/example/underpressure/ocr/BloodPressureOcrManager.kt`
  - Integrate `SevenSegmentRecognizer` as the primary engine with ML Kit as fallback
  - Ensure results are passed through `OcrParser` if necessary
  - Purpose: Unified OCR entry point
  - _Leverage: `app/src/main/java/com/example/underpressure/ocr/BloodPressureOcrManager.kt`_
  - _Requirements: Design (Architecture)_
  - _Prompt: Role: Android Developer | Task: Update `BloodPressureOcrManager` to attempt `SevenSegmentRecognizer` first, falling back to ML Kit if no digits are found. | Success: Manager returns `OcrResult` from either engine._

- [x] 9. Create unit tests for `SevenSegmentRecognizer`
  - File: `app/src/test/java/com/example/underpressure/ocr/SevenSegmentRecognizerTest.kt`
  - Write tests using `7.jpg` and `8.jpg` from `test_image/`
  - Purpose: Verify functional parity with Python prototype
  - _Leverage: `test_image/7.jpg`, `test_image/8.jpg`_
  - _Requirements: Testing (Case 1 & 2)_
  - _Prompt: Role: QA Engineer | Task: Create unit tests that load `7.jpg` and `8.jpg` and assert the expected row strings ("123\n81\n92" and "141\n90\n85"). | Success: Tests pass with exact string matches._
