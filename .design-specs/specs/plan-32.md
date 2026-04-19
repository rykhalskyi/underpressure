# Implementation Plan - 32: Tiered time hierarchy in measurements list

## Overview
Implement a hierarchical, collapsible structure for the measurements list grouped by Year > Month > Day. This optimizes navigation for large datasets and prioritizes visibility for recent data.

## Steering Document Alignment

### Technical Standards (tech.md)
- Uses Jetpack Compose for declarative UI and animations.
- Follows MVVM with Clean Architecture.
- Adheres to Material 3 design guidelines (headers, chevrons).

### Coding Conventions (codestyle.md)
- ViewModel manages UI state and expansion logic.
- Uses `StateFlow.update` for atomic state transitions.
- Employs constructor injection for dependencies.

### Project Structure (structure.md)
- New UI components will be placed in `com.example.underpressure.ui.table.components`.
- UI-specific models will be in `com.example.underpressure.ui.table`.

## Technical Analysis

### Impacted Files
- **Created**:
    - `app/src/main/java/com/example/underpressure/ui/table/components/YearHeader.kt`
    - `app/src/main/java/com/example/underpressure/ui/table/components/MonthHeader.kt`
- **Modified**:
    - `app/src/main/java/com/example/underpressure/ui/table/TableUiState.kt`: Add expansion state and hierarchical item models.
    - `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`: Implement grouping and toggling logic.
    - `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`: Update LazyColumn to render the hierarchy.

### Data Model Changes
- No database changes.
- New UI sealed class `TableItem` to represent different row types in the `LazyColumn`.

### API Changes
- Internal ViewModel methods added: `toggleYearExpansion(year: Int)`, `toggleMonthExpansion(yearMonth: String)`.

## Implementation Strategy

1.  **Define UI Items**:
    ```kotlin
    sealed class TableItem {
        data class YearHeader(val year: Int, val isExpanded: Boolean) : TableItem()
        data class MonthHeader(val yearMonth: String, val monthName: String, val isExpanded: Boolean, val summary: String?) : TableItem()
        data class DayRow(val summary: DayMeasurementSummary) : TableItem()
    }
    ```
2.  **Update ViewModel State**:
    - Track `expandedYears: Set<Int>` and `expandedMonths: Set<String>` (format "YYYY-MM").
    - Default: Current year and current month are expanded.
3.  **Grouping Logic**:
    - Group flat daily measurements by year and month.
    - Flatten into a list of `TableItem` based on expansion sets.
    - If a year is collapsed, only its `YearHeader` is shown.
    - If a month is collapsed, only its `MonthHeader` is shown.
4.  **UI Implementation**:
    - `YearHeader`: Large font, primary color, trailing chevron.
    - `MonthHeader`: Medium font, secondary color, trailing chevron, optional avg BP summary.
    - `MeasurementTableScreen`: Use `LazyColumn` with `items(uiState.displayItems)` and `key = { ... }`.
5.  **Interaction**:
    - Click handlers for headers to trigger expansion toggles in ViewModel.
    - Ensure `lazyListState` preserves position during toggles (Compose handles this mostly automatically if keys are stable).

## Verification & Testing

### Unit Tests
- `MeasurementTableViewModelTest`: Verify grouping logic and default expansion states.
- `MeasurementTableViewModelTest`: Verify `toggleYearExpansion` and `toggleMonthExpansion` update the `displayItems` correctly.

### Instrumented Tests
- `TableHierarchyIntegrationTest`: Verify clicking headers expands/collapses sections.
- `TableHierarchyIntegrationTest`: Verify recent data is visible by default.

### Manual Verification
- Inject large dataset (e.g., 2 years of data) and verify smooth scrolling and animations.
- Check "Current month is expanded" requirement.
