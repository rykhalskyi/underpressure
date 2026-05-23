# Implementation Plan - Epic 4: Custom Trackers & Annotations (Issue 53)

## Overview
Transform the app into a comprehensive health detective tool by allowing users to track additional health metrics (Weight, Temperature, Medications, Symptoms) alongside their blood pressure. This includes flexible schema support, smart chart annotations, secondary Y-axis visualization, and an updated input system.

## Steering Alignment
- **Architecture**: MVVM + Clean Architecture.
- **Database**: Room for persistence.
- **UI**: Jetpack Compose.
- **Visualization**: MPAndroidChart.
- **Style**: Maintain consistency with existing color schemes and UI components (Pills, Dialogs).

## Impact
- **Data Layer**: New entities `TrackerDefinitionEntity` and `TrackerValueEntity`. Migration or database update.
- **Domain Layer**: New repository `TrackerRepository`, models for Tracker Definitions and Values.
- **UI Layer**:
    - Enhanced `MeasurementEditDialog` to include dynamic tracker inputs.
    - Updated `MeasurementTableScreen` to show active trackers.
    - Updated `ChartScreen` to support Smart Markers and Secondary Y-Axis.
    - New `TrackerManagementScreen` for configuration.

## Strategy

### 1. Data Schema & Persistence
- **TrackerDefinitionEntity**:
  - `id: Long` (PK)
  - `name: String`
  - `type: TrackerType` (Enum: FLOAT, BOOLEAN, STRING)
  - `unit: String?`
  - `isActive: Boolean`
  - `showOnChart: Boolean`
  - `useSecondaryAxis: Boolean`
- **TrackerValueEntity**:
  - `id: Long` (PK)
  - `measurementId: Long` (FK to `measurements.id`)
  - `trackerId: Long` (FK to `tracker_definitions.id`)
  - `floatValue: Double?`
  - `booleanValue: Boolean?`
  - `stringValue: String?`
  - `timestamp: Long`
- Create `TrackerDao` and `TrackerRepository`.

### 2. Tracker Management Screen
- Create `TrackerManagementScreen` as a new top-level screen.
- Update `MainActivity.kt` to include `Screen.Trackers` and handle navigation to it.
- Add a navigation entry (button/icon) on the `MeasurementTableScreen` or a side menu/bottom bar to access Tracker Management.
- List default trackers (Weight, Pills, etc.) and allow toggling `isActive`, `showOnChart`, `useSecondaryAxis`.

### 3. Measurement Input Enhancement
- Modify `MeasurementDialogState` to hold current tracker values.
- Update `MeasurementEditDialog` to dynamically render inputs based on active `TrackerDefinitions`.
  - Float: Numeric field.
  - Boolean: Toggle/Checkbox.
  - String: Multi-line text field (Notes/Symptoms).
- Update `MeasurementRepository.save/update` or create a combined use-case to save both BP and Tracker values.

### 4. TableView Updates
- Add `activeTrackers` to `TableUiState`.
- Update `DayMeasurementSummary` to include tracker values.
- In `MeasurementTableScreen`, display active tracker values in a way similar to anytime measurements (perhaps as small pills or secondary rows).

### 5. Chart Visualization
- **Smart Markers**:
    - Update `BloodPressureChart` to render icons for Boolean/String trackers at the bottom of the chart area or on the X-axis.
    - Enhance `BloodPressureMarkerView` to show tracker details when a point is selected.
- **Secondary Y-Axis**:
    - If a tracker has `useSecondaryAxis` enabled, add its data to `axisRight`.
    - Configure `axisRight` styling (labels, grid) to match `axisLeft`.
- **ChartConfigurationSheet**: Add toggles for showing/hiding specific trackers on the chart.

## Verification & Testing
- **Unit Tests**:
    - `TrackerRepository` tests.
    - `BloodPressureValidator` extension for numeric tracker values (if needed).
- **Instrumentation Tests**:
    - UI test for adding measurement with custom trackers.
    - UI test for `TrackerManagementScreen`.
- **Manual Verification**:
    - Ensure secondary Y-axis scales correctly with weight data.
    - Verify markers appear correctly on the chart.
    - Check table view layout with multiple active trackers.

## Migration & Rollback
- Room migration to add new tables.
- Since it's a new feature, rollback involves deleting the new tables or ignoring them in the UI.
