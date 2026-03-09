# Tasks Document - Camera Integration and OCR (Issue #14)

- [x] 1. Add CameraX and ML Kit dependencies to libs.versions.toml
  - File: `gradle/libs.versions.toml`
  - Add versions and libraries for CameraX (core, camera2, lifecycle, view) and ML Kit Text Recognition.
  - Purpose: Provide necessary libraries for camera and OCR functionality.
  - _Leverage: Existing version catalog structure._
  - _Requirements: Non-Functional: Dependency Management_
  - _Prompt: Role: Android Developer | Task: Add CameraX (1.4.0) and ML Kit Text Recognition (16.0.1) dependencies to libs.versions.toml. | Success: dependencies are correctly added to the version catalog._

- [x] 2. Update app module build.gradle.kts with new dependencies
  - File: `app/build.gradle.kts`
  - Add the newly defined CameraX and ML Kit libraries to the dependencies block.
  - Purpose: Enable the libraries in the app module.
  - _Leverage: libs.versions.toml_
  - _Requirements: Non-Functional: Dependency Management_
  - _Prompt: Role: Android Developer | Task: Add CameraX and ML Kit dependencies to app/build.gradle.kts using the version catalog. | Success: Project syncs successfully with new dependencies._

- [x] 3. Create OcrResult and OcrParser in ocr package
  - File: `app/src/main/java/com/example/underpressure/ocr/OcrResult.kt`, `app/src/main/java/com/example/underpressure/ocr/OcrParser.kt`
  - Define `OcrResult` data class and `OcrParser` with regex logic to extract SYS, DIA, and PULSE.
  - Purpose: Decouple text extraction logic from image processing.
  - _Leverage: com.example.underpressure.domain.validation.BloodPressureValidator_
  - _Requirements: Requirement 2, Non-Functional: Single Responsibility Principle_
  - _Prompt: Role: Kotlin Developer | Task: Create OcrResult data class and OcrParser utility. OcrParser should use Regex to find numbers in raw text that likely represent blood pressure (SYS/DIA and Pulse). | Success: Parser correctly identifies numbers from noisy text strings._

- [x] 4. Create unit tests for OcrParser
  - File: `app/src/test/java/com/example/underpressure/ocr/OcrParserTest.kt`
  - Write tests for `OcrParser` with various raw text inputs (correct format, noisy text, missing values).
  - Purpose: Ensure reliability of the extraction logic.
  - _Leverage: com.example.underpressure.domain.validation.BloodPressureValidatorTest (if exists)_
  - _Requirements: Requirement 2_
  - _Prompt: Role: QA Engineer | Task: Create unit tests for OcrParser covering success and failure scenarios including noisy OCR output. | Success: All tests pass._

- [x] 5. Implement BloodPressureOcrManager
  - File: `app/src/main/java/com/example/underpressure/ocr/BloodPressureOcrManager.kt`
  - Implement ML Kit Text Recognition wrapper to process Bitmaps.
  - Purpose: Encapsulate ML Kit specific API calls.
  - _Leverage: None_
  - _Requirements: Requirement 2, Requirement 3_
  - _Prompt: Role: Android Developer | Task: Implement BloodPressureOcrManager to process Bitmaps using ML Kit TextRecognition. Ensure it doesn't save images to disk. | Success: Manager returns recognized raw text from a Bitmap._

- [x] 6. Create CameraCaptureActivity and layout
  - File: `app/src/main/java/com/example/underpressure/ui/camera/CameraCaptureActivity.kt`, `app/src/main/res/layout/activity_camera_capture.xml` (or Compose equivalent)
  - Implement CameraX preview and capture logic.
  - Purpose: Provide a dedicated UI for taking the measurement picture.
  - _Leverage: None_
  - _Requirements: Requirement 1, Requirement 3_
  - _Prompt: Role: Android Developer | Task: Create CameraCaptureActivity using CameraX. It should show a preview, have a capture button, and return the captured image as a Bitmap in-memory to the caller. | Success: Camera opens, shows preview, and returns result on capture._

- [x] 7. Add Camera strings and Permission handling
  - File: `app/src/main/res/values/strings.xml`
  - Add strings for "Read from Camera", permission requests, and OCR failure messages.
  - Purpose: Localization and user guidance.
  - _Leverage: Existing strings.xml_
  - _Requirements: Requirement 1, Non-Functional: Usability_
  - _Prompt: Role: Android Developer | Task: Add necessary strings for the camera feature to strings.xml. | Success: All new UI strings are externalized._

- [x] 8. Update MeasurementEditDialog with Camera button
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/MeasurementEditDialog.kt`
  - Add an IconButton or Button to launch the camera activity.
  - Purpose: Allow users to trigger the OCR flow.
  - _Leverage: MeasurementEditDialog.kt_
  - _Requirements: Requirement 1_
  - _Prompt: Role: Compose Developer | Task: Add a "Read from Camera" button to MeasurementEditDialog. Use rememberLauncherForActivityResult to handle the camera result. | Success: Button appears and launches camera activity._

- [x] 9. Integrate OCR result into MeasurementEditDialog logic
  - File: `app/src/main/java/com/example/underpressure/ui/table/components/MeasurementEditDialog.kt`
  - Handle the result from `CameraCaptureActivity`, process it through `BloodPressureOcrManager` and `OcrParser`, and update the text field.
  - Purpose: Complete the automated data entry flow.
  - _Leverage: MeasurementEditDialog.kt, OcrParser.kt_
  - _Requirements: Requirement 2_
  - _Prompt: Role: Compose Developer | Task: Update MeasurementEditDialog to handle the Bitmap returned from the camera, run it through OCR, and update the text input if successful. | Success: Extracted values appear in the dialog input field._

- [x] 10. Add UI tests for Camera integration flow
  - File: `app/src/androidTest/java/com/example/underpressure/ui/CameraIntegrationTest.kt`
  - Write instrumented tests to verify the "Read from Camera" button opens the camera and handles the result (using mocks where possible).
  - Purpose: Ensure end-to-end reliability of the feature.
  - _Leverage: MeasurementTableScreenTest.kt_
  - _Requirements: All Requirements_
  - _Prompt: Role: QA Automation Engineer | Task: Create instrumented tests to verify the camera launch from MeasurementEditDialog and result handling. | Success: UI tests pass for the camera flow._
