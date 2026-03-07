# Tasks Document - Issue #5: Implement Room Database

- [x] 1. Add Room and KSP dependencies to build configuration
  - File: `gradle/libs.versions.toml`, `app/build.gradle.kts`, `build.gradle.kts`
  - Define Room versions (v2.6.1 or latest stable) and KSP version in `libs.versions.toml`
  - Apply KSP plugin in project and app `build.gradle.kts`
  - Add Room runtime and KSP dependencies to `app/build.gradle.kts`
  - Purpose: Provide the necessary libraries for local database persistence
  - _Leverage: Existing dependency management in libs.versions.toml_
  - _Requirements: 1.1, 2.1_
  - _Prompt: Role: Android Developer with expertise in Gradle and Room | Task: Add Room and KSP dependencies to the project following requirement 1.1 and 2.1. Use Room 2.6.1 and compatible KSP version. | Restrictions: Maintain consistency with libs.versions.toml, ensure KSP is correctly applied at both project and module levels | Success: Project syncs successfully with Gradle, Room classes are accessible in the project_

- [x] 2. Create MeasurementEntity in data.local.entities
  - File: `app/src/main/java/com/example/underpressure/data/local/entities/MeasurementEntity.kt`
  - Implement `MeasurementEntity` data class with Room annotations
  - Include fields: `id` (PK), `date` (Indexed), `slotIndex`, `systolic`, `diastolic`, `pulse`, `createdAt`, `updatedAt`
  - Purpose: Define the schema for blood pressure measurements
  - _Leverage: tech.md (Room patterns), design-5.md (entity structure)_
  - _Requirements: 1.1_
  - _Prompt: Role: Android Developer with expertise in Room Persistence | Task: Implement MeasurementEntity as defined in design-5.md. Ensure indexing on the 'date' field. | Restrictions: Adhere to Kotlin codestyle for data classes, use appropriate Room annotations | Success: Entity is correctly defined and compile-time check passes_

- [x] 3. Create AppSettingsEntity and TypeConverters
  - File: `app/src/main/java/com/example/underpressure/data/local/entities/AppSettingsEntity.kt`, `app/src/main/java/com/example/underpressure/data/local/converters/Converters.kt`
  - Implement `AppSettingsEntity` for singleton settings record
  - Implement `Converters` for `List<String>` and `List<Boolean>` serialization
  - Purpose: Define application settings schema and handle list serialization
  - _Leverage: design-5.md (entity structure), tech.md (Kotlin Serialization or Gson if preferred)_
  - _Requirements: 2.1, 2.2_
  - _Prompt: Role: Android Developer with expertise in Room and JSON serialization | Task: Implement AppSettingsEntity and necessary TypeConverters as defined in design-5.md. | Restrictions: Use a singleton-like ID (e.g., id=1) for settings, follow Room converter patterns | Success: Entity is defined, Converters are ready for database integration_

- [x] 4. Implement MeasurementDao interface
  - File: `app/src/main/java/com/example/underpressure/data/local/dao/MeasurementDao.kt`
  - Define CRUD operations with `@Insert`, `@Update`, `@Delete`, and `@Query`
  - Implement `getByDate` and `getAll` as `Flow<List<MeasurementEntity>>`
  - Use `suspend` functions for write operations
  - Purpose: Provide data access methods for blood pressure records
  - _Leverage: tech.md (Coroutines/Flow patterns), design-5.md (DAO interfaces)_
  - _Requirements: 1.2, 1.3, 1.4, 1.5_
  - _Prompt: Role: Android Developer specializing in reactive programming (Coroutines/Flow) | Task: Implement MeasurementDao with the specified query methods. Use Flow for observable queries. | Restrictions: All write operations must be suspend functions | Success: DAO provides all required CRUD and query methods_

- [x] 5. Implement AppSettingsDao interface
  - File: `app/src/main/java/com/example/underpressure/data/local/dao/AppSettingsDao.kt`
  - Define `insertOrUpdate` (using `@Upsert`) and `getSettings` query
  - Implement `getSettings` as `Flow<AppSettingsEntity?>`
  - Purpose: Provide data access for application configuration
  - _Leverage: design-5.md (DAO interfaces)_
  - _Requirements: 2.1, 2.2_
  - _Prompt: Role: Android Developer with expertise in Room DAOs | Task: Implement AppSettingsDao as defined in design-5.md. | Restrictions: Ensure proper upsert logic for the settings singleton | Success: DAO allows saving and observing application settings_

- [x] 6. Create AppDatabase singleton configuration
  - File: `app/src/main/java/com/example/underpressure/data/local/database/AppDatabase.kt`
  - Implement abstract class extending `RoomDatabase`
  - Register `MeasurementEntity`, `AppSettingsEntity`, and `Converters`
  - Provide abstract methods for `MeasurementDao` and `AppSettingsDao`
  - Purpose: Define the central database instance
  - _Leverage: tech.md (Room standards), structure.md (package organization)_
  - _Requirements: All_
  - _Prompt: Role: Android Architect specializing in Room Persistence | Task: Implement the AppDatabase configuration, including entities and converters. | Restrictions: Follow established Room Database patterns | Success: Database compiles and can be instantiated_

- [x] 7. Implement Instrumented Unit Tests for Room Database
  - File: `app/src/androidTest/java/com/example/underpressure/data/local/RoomDatabaseTest.kt`
  - Write tests for measurement CRUD operations and date-based queries
  - Write tests for settings updates and upsert logic
  - Use in-memory database for testing
  - Purpose: Ensure reliability and correctness of the data layer
  - _Leverage: testing.md (if available), tech.md (Testing Frameworks)_
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2_
  - _Prompt: Role: QA Engineer specializing in Android Instrumented Testing | Task: Create comprehensive instrumented tests for the Room database implementation. | Restrictions: Use in-memory database instance for isolation, cover both success and edge cases | Success: All tests pass on emulator or device_
