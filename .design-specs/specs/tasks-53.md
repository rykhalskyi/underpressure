# Tasks - Epic 4: Custom Trackers & Annotations (Issue 53)

- [ ] 1. **Database Schema Setup**
  - File: `app/src/main/java/com/otakeessen/underpressure/data/local/entities/TrackerEntities.kt`
  - Description: Create `TrackerDefinitionEntity` and `TrackerValueEntity` along with `TrackerType` enum.
  - _Leverage_: `MeasurementEntity.kt`
  - _Prompt_: Create `TrackerDefinitionEntity` and `TrackerValueEntity` in a new file `TrackerEntities.kt`. Define `TrackerType` enum (FLOAT, BOOLEAN, STRING). Ensure proper Foreign Key relationship from `TrackerValueEntity` to `MeasurementEntity` and `TrackerDefinitionEntity`.

- [ ] 2. **Room DAO and Repository**
  - File: `app/src/main/java/com/otakeessen/underpressure/data/local/dao/TrackerDao.kt`, `app/src/main/java/com/otakeessen/underpressure/domain/repository/TrackerRepository.kt`
  - Description: Implement Room DAO for trackers and define the Repository interface.
  - _Leverage_: `MeasurementRepository.kt`
  - _Prompt_: Implement `TrackerDao` with basic CRUD operations and `TrackerRepository` interface. Include methods to get active trackers and values by measurement ID.

- [ ] 3. **Tracker Repository Implementation**
  - File: `app/src/main/java/com/otakeessen/underpressure/data/repository/TrackerRepositoryImpl.kt`
  - Description: Implement the `TrackerRepository` interface.
  - _Leverage_: `MeasurementRepositoryImpl.kt`
  - _Prompt_: Implement `TrackerRepositoryImpl` using `TrackerDao`. Ensure it handles data mapping between entities and domain models.

- [ ] 4. **Tracker Management Screen and Navigation**
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/trackers/TrackerManagementScreen.kt`, `app/src/main/java/com/otakeessen/underpressure/MainActivity.kt`
  - Description: Build the dedicated screen to manage trackers and update app navigation.
  - _Leverage_: `SettingsScreen.kt`, `MainActivity.kt`
  - _Prompt_: Create `TrackerManagementScreen` in a new package `ui.trackers`. Update `MainActivity.kt` to include `Screen.Trackers` in the navigation enum and handle the routing. Add a navigation button in `MeasurementTableScreen` to open this new screen.


- [ ] 5. **Enhanced Measurement Dialog State**
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/table/TableUiState.kt`
  - Description: Add tracker values to `MeasurementDialogState`.
  - _Leverage_: Existing `MeasurementDialogState`
  - _Prompt_: Update `MeasurementDialogState` in `TableUiState.kt` to include a list or map of current tracker values being edited.

- [ ] 6. **Dynamic Tracker Inputs in Dialog**
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/table/components/MeasurementEditDialog.kt`
  - Description: Render inputs for active trackers in the edit dialog.
  - _Leverage_: Existing `MeasurementEditDialog.kt`
  - _Prompt_: Modify `MeasurementEditDialog` to fetch active trackers and render appropriate input fields (Numeric for FLOAT, Switch for BOOLEAN, OutlinedTextField for STRING) in a scrollable column.

- [ ] 7. **Saving Tracker Values**
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Description: Update the ViewModel to save tracker values along with the measurement.
  - _Leverage_: `MeasurementTableViewModel.kt`
  - _Prompt_: Update `onSaveMeasurement` in `MeasurementTableViewModel` to also save the collected tracker values using `TrackerRepository`.

- [ ] 8. **Chart Smart Markers**
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/chart/components/BloodPressureChart.kt`
  - Description: Implement icon rendering for annotations and boolean trackers on the chart.
  - _Leverage_: `RiskZoneLineChart` in `BloodPressureChart.kt`
  - _Prompt_: Update `BloodPressureChart` and its internal `LineChart` subclass to render small icons or markers on the X-axis for measurements that have boolean flags (e.g., "Took Pills") or text notes.

- [ ] 9. **Chart Secondary Y-Axis**
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/chart/components/BloodPressureChart.kt`
  - Description: Enable and configure the right Y-axis for float trackers like Weight.
  - _Leverage_: `MPAndroidChart` axis configuration.
  - _Prompt_: Update `BloodPressureChart` to support a secondary Y-axis (right side). If tracker data is provided with `useSecondaryAxis=true`, bind it to `axisRight` and configure scaling.

- [ ] 10. **TableView Integration**
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/table/MeasurementTableScreen.kt`
  - Description: Display active tracker values in the measurement table.
  - _Leverage_: `MeasurementTableScreen.kt`
  - _Prompt_: Update the measurement table to display active tracker values (e.g., weight or pill icons) alongside the BP readings.
