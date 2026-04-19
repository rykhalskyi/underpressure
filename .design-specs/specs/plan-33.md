# Implementation Plan - 33: Generic Measurement Lists Feature

## Overview
Extend the application to support user-defined measurement lists (e.g., Weight, Blood Sugar, Medication taken) with multiple data types (DOUBLE, BOOLEAN, TEXT). Integrate these lists into the measurement input dialog, the main table view, and the visualization (plotting) layer.

## Steering Document Alignment

### Technical Standards (tech.md)
- Uses Room for persistence of new entities.
- Follows MVVM with Clean Architecture.
- Uses Jetpack Compose for new UI screens and components.
- Adheres to Kotlin 2.0 and Coroutines.

### Coding Conventions (codestyle.md)
- Follows SOLID principles by extending existing interfaces and creating new specialized ones.
- Uses `ViewModel` and `StateFlow` for state management.
- Uses constructor injection for repositories.
- Adheres to naming conventions (PascalCase for classes, camelCase for functions).

### Project Structure (structure.md)
- New entities in `data/local/entities/`.
- New DAOs in `data/local/dao/`.
- New repository interfaces in `domain/repository/`.
- New repository implementations in `data/repository/`.
- New UI components in `ui/measurements/` (or similar).

## Technical Analysis

### Impacted Files
#### New Files
- `app/src/main/java/com/example/underpressure/data/local/entities/MeasurementListEntity.kt`
- `app/src/main/java/com/example/underpressure/data/local/entities/MeasurementEntryEntity.kt`
- `app/src/main/java/com/example/underpressure/data/local/dao/MeasurementListDao.kt`
- `app/src/main/java/com/example/underpressure/data/local/dao/MeasurementEntryDao.kt`
- `app/src/main/java/com/example/underpressure/domain/repository/GenericMeasurementRepository.kt`
- `app/src/main/java/com/example/underpressure/data/repository/GenericMeasurementRepositoryImpl.kt`
- `app/src/main/java/com/example/underpressure/ui/measurements/MeasurementListScreen.kt`
- `app/src/main/java/com/example/underpressure/ui/measurements/MeasurementListViewModel.kt`

#### Modified Files
- `app/src/main/java/com/example/underpressure/data/local/database/AppDatabase.kt`: Add new entities and DAOs.
- `app/src/main/java/com/example/underpressure/MainActivity.kt`: Add navigation for Measurement Lists screen.
- `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`: Fetch and include generic measurements in UI state.
- `app/src/main/java/com/example/underpressure/ui/table/components/MeasurementEditDialog.kt`: Add dynamic input fields for active lists.
- `app/src/main/java/com/example/underpressure/ui/table/components/DayRow.kt`: Render generic measurement values.
- `app/src/main/java/com/example/underpressure/ui/chart/ChartViewModel.kt`: Include DOUBLE/BOOLEAN lists in plot options.
- `app/src/main/java/com/example/underpressure/ui/chart/components/BloodPressureChart.kt`: Update to handle multi-line/indicator plotting.

### Data Model Changes
- **MeasurementListEntity**: `id`, `name`, `type` (ENUM), `active` (BOOLEAN).
- **MeasurementEntryEntity**: `id`, `date`, `slotIndex`, `listId`, `value` (STRING), `updatedAt`.

### API Changes
- `GenericMeasurementRepository`: Methods to CRUD lists and entries.
- `MeasurementTableViewModel`: UI state updated to include `genericValues` per slot.

## Implementation Strategy

### Phase 1: Data Layer
1. Define `MeasurementListType` enum (DOUBLE, BOOLEAN, TEXT).
2. Create `MeasurementListEntity` and `MeasurementEntryEntity`.
3. Create `MeasurementListDao` and `MeasurementEntryDao`.
4. Update `AppDatabase` to version 3 (adding new entities).
5. Implement `GenericMeasurementRepository`.

### Phase 2: List Management UI
1. Create `MeasurementListViewModel` to manage the list of available measurements.
2. Create `MeasurementListScreen` with a FAB to add new lists and edit/delete actions.

### Phase 3: Measurement Input Integration
1. Update `MeasurementEditDialog` to accept a list of active generic measurements.
2. In `MeasurementTableViewModel`, fetch active lists and their existing entries for the selected slot.
3. Dynamically render input fields (Number for DOUBLE, Checkbox for BOOLEAN, TextField for TEXT).

### Phase 4: Table View Integration
1. Update `DayMeasurementSummary` to include generic measurement values.
2. Update `DayRow` to display these values alongside blood pressure.

### Phase 5: Chart & Visualization
1. Update `ChartViewModel` to load generic measurements of type DOUBLE and BOOLEAN.
2. Extend `BloodPressureChart` to support additional line datasets (DOUBLE) and bar indicators (BOOLEAN).

## Verification & Testing
1. **Unit Tests**:
    - Repository tests for CRUD operations on generic measurements.
    - ViewModel tests for state transformation including generic measurements.
2. **Instrumented Tests**:
    - UI tests for the new Measurement Lists screen.
    - End-to-end test for entering generic measurements and seeing them in the table and chart.
3. **Manual Verification**:
    - Create a "Weight" list (DOUBLE), enter data, verify plot.
    - Create a "Meds" list (BOOLEAN), enter data, verify indicator on chart.
    - Create a "Notes" list (TEXT), enter data, verify visible in table.
