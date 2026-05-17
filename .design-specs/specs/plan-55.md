# Implementation Plan - Issue 55: Improve Classification Summary

## Overview
Enhance the `ClassificationSummary` component to be more informative and educational by adding numeric range boundaries for each blood pressure level, absolute reading counts, and guideline source attribution.

## Steering Alignment
- **Architecture**: Domain-driven. Classification logic remains in `BloodPressureClassifier` (Domain), while UI rendering stays in `ClassificationSummary` (UI).
- **Localization**: All strings (including ranges and source labels) should be externalized to `strings.xml`.
- **UI/UX**: Use Material 3 typography and spacing. Improve legend alignment for better readability.

## Impact

### Logic & Data
- **`BloodPressureLevel.kt`**: 
    - Enhance `BloodPressureClassifier` with a new method `getRangeStringRes(level, guidelines): Int` or similar to return the appropriate string resource for the numeric range.
- **`BpLevelMapper.kt`**: 
    - Possibly add mapping for range strings if not handled directly in `BloodPressureClassifier`.

### UI Components
- **`ClassificationSummary.kt`**:
    - Update the legend to display: `Color Square | Label | Range | Count (Percentage)`.
    - Add a footer with the source guidelines.
    - Improve layout alignment (e.g., using a table-like structure or `Row` with `weight` or `Arrangement`).

### Resources
- **`strings.xml`**:
    - Add strings for AHA and ESC range boundaries for each level.
    - Add strings for the guideline source (e.g., "Source: AHA/ACC 2017 Guidelines", "Source: ESC/ESH 2018 Guidelines").
    - Add a format string for the count/percentage (e.g., "%1$d readings (%2$d%%)").

## Strategy

### 1. Update Domain Logic
- In `BloodPressureClassifier`, implement a way to retrieve the numeric range string resource.
- Ensure the ranges match the logic used in `classifyAmerican` and `classifyEuropean`.

### 2. Add String Resources
- Define range strings in `strings.xml`.
- Examples:
    - `bp_range_aha_normal`: "< 120 / < 80"
    - `bp_range_aha_elevated`: "120-129 / < 80"
    - `bp_range_esc_normal`: "< 130 / < 85"
    - ... and so on.

### 3. Refactor `ClassificationSummary`
- Update the legend item rendering.
- Use `total` to calculate both percentage and show the raw `count`.
- Add the source attribution footer at the bottom of the `Column`.

### 4. Verification
- Verify the summary displays correctly for both AHA and ESC guidelines.
- Ensure counts and percentages are accurate.
- Check layout on different screen sizes (especially the legend alignment).
