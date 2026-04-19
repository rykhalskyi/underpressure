# Tasks Document - 33: Generic Measurement Lists Feature

- [ ] 1. Create MeasurementListEntity and MeasurementListType
  - File: `app/src/main/java/com/example/underpressure/data/local/entities/MeasurementListEntity.kt`
  - Define `MeasurementListType` enum: `DOUBLE`, `BOOLEAN`, `TEXT`.
  - Define `MeasurementListEntity` with fields: `id`, `name`, `type`, `active`.
  - Purpose: Foundation for user-defined measurement lists.
  - _Requirements: 33.1_
  - _Prompt: Role: Android Developer with expertise in Room persistence | Task: Create MeasurementListEntity and MeasurementListType enum following requirement 33.1. Ensure type-safety and proper Room annotations. | Restrictions: Follow existing entity patterns, maintain consistency with project structure. | Success: Entities compile and are ready for DAO implementation._

- [ ] 2. Create MeasurementEntryEntity
  - File: `app/src/main/java/com/example/underpressure/data/local/entities/MeasurementEntryEntity.kt`
  - Define `MeasurementEntryEntity` with fields: `id`, `date`, `slotIndex`, `listId` (FK), `value` (STRING), `updatedAt`.
  - Purpose: Store individual data points for generic lists.
  - _Requirements: 33.1_
  - _Prompt: Role: Android Developer with expertise in Room and Relational Mapping | Task: Create MeasurementEntryEntity following requirement 33.1, including foreign key relationship to MeasurementListEntity. | Restrictions: Use existing slotIndex (0-3) logic and date format (YYYY-MM-DD). | Success: Entity compiles and supports relationships._

- [ ] 3. Create DAOs for Generic Measurements
  - Files: `app/src/main/java/com/example/underpressure/data/local/dao/MeasurementListDao.kt`, `app/src/main/java/com/example/underpressure/data/local/dao/MeasurementEntryDao.kt`
  - Implement CRUD operations for both entities.
  - Include queries for active lists and entries by date/slot.
  - Purpose: Data access layer for generic measurements.
  - _Requirements: 33.1_
  - _Prompt: Role: Android Developer specializing in Room DAOs | Task: Implement DAOs for MeasurementList and MeasurementEntry with CRUD and specialized query methods as per requirement 33.1. | Restrictions: Follow existing DAO patterns, use Coroutines (suspend) and Flow. | Success: DAOs are fully implemented and testable._

- [ ] 4. Update AppDatabase and Repository
  - Files: `app/src/main/java/com/example/underpressure/data/local/database/AppDatabase.kt`, `app/src/main/java/com/example/underpressure/domain/repository/GenericMeasurementRepository.kt`, `app/src/main/java/com/example/underpressure/data/repository/GenericMeasurementRepositoryImpl.kt`
  - Increment DB version, add entities and DAOs.
  - Implement the new repository for generic measurements.
  - Purpose: Connect data layer to the rest of the app.
  - _Requirements: 33.1_
  - _Prompt: Role: Android Developer with expertise in Room Database and Clean Architecture | Task: Update AppDatabase and implement GenericMeasurementRepository following requirement 33.1. Ensure backward compatibility and follow repository patterns. | Restrictions: Use fallbackToDestructiveMigration if necessary but prefer incremental approach if possible. | Success: Repository is available for ViewModels to use._

- [ ] 5. Implement Measurement List Management UI
  - Files: `app/src/main/java/com/example/underpressure/ui/measurements/MeasurementListViewModel.kt`, `app/src/main/java/com/example/underpressure/ui/measurements/MeasurementListScreen.kt`
  - Create ViewModel for list management.
  - Build Compose screen to list, add, edit, and delete measurement lists.
  - Purpose: Allow users to manage their custom measurements.
  - _Requirements: 33.2_
  - _Prompt: Role: Android Developer specializing in Jetpack Compose and MVVM | Task: Implement Measurement List management UI following requirement 33.2. Include fields for name, type, and active status. | Restrictions: Follow Material 3 design and project's UI patterns. | Success: Users can manage generic measurement lists._

- [ ] 6. Extend Measurement Input Dialog
  - Files: `app/src/main/java/com/example/underpressure/ui/table/components/MeasurementEditDialog.kt`, `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableViewModel.kt`
  - Modify `MeasurementEditDialog` to dynamically show inputs for active generic lists.
  - Update ViewModel to fetch and save generic entries alongside blood pressure.
  - Purpose: Enable data entry for generic measurements.
  - _Requirements: 33.3_
  - _Prompt: Role: Android UI Developer with expertise in dynamic forms and Jetpack Compose | Task: Update MeasurementEditDialog to dynamically render inputs based on active MeasurementLists as per requirement 33.3. Update ViewModel to handle saving. | Restrictions: Maintain existing blood pressure input logic, ensure optional inputs are handled. | Success: Inputs for active lists appear correctly in the dialog and persist data._

- [ ] 7. Update Main Table View
  - Files: `app/src/main/java/com/example/underpressure/ui/table/components/DayRow.kt`, `app/src/main/java/com/example/underpressure/ui/table/DayMeasurementSummary.kt`
  - Update data structures to hold generic values.
  - Update `DayRow` to display these values in the table.
  - Purpose: Display generic measurements in the daily summary.
  - _Requirements: 33.4_
  - _Prompt: Role: Android UI Developer specializing in complex Compose layouts | Task: Update the main table view to display generic measurement values alongside blood pressure as per requirement 33.4. | Restrictions: Maintain table readability and existing BP format. | Success: Generic values are visible in the table per date and slot._

- [ ] 8. Update Plot View for Generic Measurements
  - Files: `app/src/main/java/com/example/underpressure/ui/chart/ChartViewModel.kt`, `app/src/main/java/com/example/underpressure/ui/chart/components/BloodPressureChart.kt`
  - Allow selection of DOUBLE and BOOLEAN lists in the chart.
  - Render DOUBLE as lines and BOOLEAN as indicators below the main chart.
  - Purpose: Visualize generic measurement data.
  - _Requirements: 33.5_
  - _Prompt: Role: Android Developer with expertise in data visualization and MPAndroidChart (or similar) | Task: Update ChartViewModel and BloodPressureChart to support plotting generic DOUBLE and BOOLEAN measurements as per requirement 33.5. | Restrictions: TEXT lists must be excluded from plots. | Success: Generic measurements are correctly visualized in the chart._

- [ ] 9. Final Integration and Navigation
  - Files: `app/src/main/java/com/example/underpressure/MainActivity.kt`, `app/src/main/java/com/example/underpressure/ui/table/MeasurementTableScreen.kt`
  - Add navigation item/button to access Measurement Lists screen.
  - Perform final cleanup and verification.
  - Purpose: Complete the feature integration.
  - _Requirements: All_
  - _Prompt: Role: Senior Android Developer | Task: Add navigation to the new Measurement Lists screen in MainActivity and perform final integration checks. | Restrictions: Ensure no regression in existing features. | Success: Feature is fully integrated and accessible._
