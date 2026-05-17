# Atomic Tasks - Issue 55: Improve Classification Summary

## Task 1: Add String Resources
- **File**: `app/src/main/res/values/strings.xml`
- **Description**: Add localized strings for range boundaries and guideline source attribution.
- **Leverage**: Existing `bp_level_*` strings.
- **Prompt**:
    As an Android developer, add the following string resources to `strings.xml`:
    1. Range boundaries for AHA (Hypotension, Normal, Elevated, Stage 1, Stage 2, Crisis).
    2. Range boundaries for ESC (Hypotension, Normal, Elevated, Stage 1, Stage 2, Crisis).
    3. Source attribution strings:
       - `label_source_aha`: "Source: AHA/ACC 2017 Guidelines"
       - `label_source_esc`: "Source: ESC/ESH 2018 Guidelines"
    4. Format string for stats:
       - `label_classification_stats_format`: "%1$d readings (%2$d%%)"
    
    Ensure AHA ranges use logic:
    - Normal: "< 120 / < 80"
    - Elevated: "120-129 / < 80"
    - Stage 1: "130-139 / 80-89"
    - Stage 2: "≥ 140 / ≥ 90"
    - Crisis: "≥ 180 / ≥ 120"
    - Hypotension: "< 90 / < 60"
    
    Ensure ESC ranges use logic:
    - Normal: "< 130 / < 85"
    - Elevated: "130-139 / 85-89"
    - Stage 1: "140-159 / 90-99"
    - Stage 2: "160-179 / 100-109"
    - Crisis: "≥ 180 / ≥ 110"
    - Hypotension: "< 90 / < 60"

## Task 2: Update Domain Logic and Mapper
- **Files**: 
    - `app/src/main/java/com/otakeessen/underpressure/domain/BloodPressureLevel.kt`
    - `app/src/main/java/com/otakeessen/underpressure/ui/util/BpLevelMapper.kt`
- **Description**: Add methods to retrieve range string resources and source labels.
- **Leverage**: Existing `BpLevelMapper.getStringRes` and `BloodPressureClassifier` object.
- **Prompt**:
    As a Kotlin developer:
    1. Add `getRangeStringRes(level: BloodPressureLevel, guidelines: BpGuidelines): Int` to `BpLevelMapper` to return the appropriate range string resource added in Task 1.
    2. Add `getSourceStringRes(guidelines: BpGuidelines): Int` to `BpLevelMapper` to return the guideline source string resource.
    3. Ensure the mapping correctly handles all levels and both guidelines.

## Task 3: Refactor ClassificationSummary UI
- **File**: `app/src/main/java/com/otakeessen/underpressure/ui/table/components/ClassificationSummary.kt`
- **Description**: Update the legend to show absolute counts and ranges, and add the source footer.
- **Leverage**: Existing `ClassificationSummary` Composable.
- **Prompt**:
    As a Jetpack Compose developer, refactor the `ClassificationSummary` Composable:
    1. Update the legend loop to display a more detailed row for each level:
       - Left: Color indicator.
       - Center: Level name and range (e.g., "Normal (< 120/80)").
       - Right: Reading count and percentage (e.g., "12 readings (45%)").
    2. Use a layout that ensures labels and stats are aligned (e.g., using `Modifier.weight` or a `ConstraintLayout`).
    3. Add a footer `Text` at the bottom of the `Column` displaying the guideline source (using `BpLevelMapper.getSourceStringRes`).
    4. Style the footer with `MaterialTheme.typography.labelSmall` and a muted color.

## Task 4: Verification and Tests
- **Files**: 
    - `app/src/test/java/com/otakeessen/underpressure/domain/BloodPressureClassifierTest.kt` (or create new UI test)
- **Description**: Verify that the summary displays the correct information.
- **Prompt**:
    Verify the changes by:
    1. Manually checking the `ClassificationSummary` in the app's table view.
    2. (Optional) Add a unit test to `BloodPressureClassifierTest.kt` or a new test file to verify the range string resource mapping in `BpLevelMapper`.
