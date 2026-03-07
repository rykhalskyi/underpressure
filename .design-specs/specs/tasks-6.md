# Tasks Document - Issue #6: Implement Repository Layer

- [x] 1. Create Repository Interfaces in Domain Layer
  - File: app/src/main/java/com/example/underpressure/domain/repository/
  - Create `MeasurementRepository.kt` and `SettingsRepository.kt` interfaces
  - Define method signatures as specified in the design document
  - Purpose: Establish a clean contract for data access, independent of implementation
  - _Leverage: .design-specs/specs/design-6.md_
  - _Requirements: 1, 2_
  - _Prompt: Role: Android Architect | Task: Create Kotlin repository interfaces `MeasurementRepository` and `SettingsRepository` in `com.example.underpressure.domain.repository` following the design in `design-6.md`. Ensure all methods use Kotlin Flow and Coroutines (suspend) appropriately. | Restrictions: Interfaces must not have any Android or Room dependencies. | Success: Interfaces are defined with correct signatures and compile._

- [x] 2. Implement MeasurementRepository in Data Layer
  - File: app/src/main/java/com/example/underpressure/data/repository/MeasurementRepositoryImpl.kt
  - Create concrete implementation of `MeasurementRepository`
  - Inject `MeasurementDao` via constructor
  - Purpose: Provide Room-based implementation for measurement data
  - _Leverage: app/src/main/java/com/example/underpressure/data/local/dao/MeasurementDao.kt_
  - _Requirements: 1_
  - _Prompt: Role: Android Developer | Task: Implement `MeasurementRepositoryImpl` in `com.example.underpressure.data.repository`. It should implement `MeasurementRepository` and take `MeasurementDao` as a constructor parameter, delegating all calls to the DAO. | Restrictions: Follow the naming conventions in `structure.md`. | Success: Implementation correctly delegates to `MeasurementDao` and handles data flow._

- [x] 3. Implement SettingsRepository in Data Layer
  - File: app/src/main/java/com/example/underpressure/data/repository/SettingsRepositoryImpl.kt
  - Create concrete implementation of `SettingsRepository`
  - Inject `AppSettingsDao` via constructor
  - Purpose: Provide Room-based implementation for application settings
  - _Leverage: app/src/main/java/com/example/underpressure/data/local/dao/AppSettingsDao.kt_
  - _Requirements: 2_
  - _Prompt: Role: Android Developer | Task: Implement `SettingsRepositoryImpl` in `com.example.underpressure.data.repository`. It should implement `SettingsRepository` and take `AppSettingsDao` as a constructor parameter, delegating all calls to the DAO. | Restrictions: Adhere to Kotlin coding style in `codestyle.md`. | Success: Implementation correctly delegates to `AppSettingsDao`._

- [x] 4. Create Repository Unit Tests
  - File: app/src/test/java/com/example/underpressure/data/repository/
  - Create `MeasurementRepositoryImplTest.kt` and `SettingsRepositoryImplTest.kt`
  - Use MockK to mock the DAOs and verify delegation
  - Purpose: Ensure repositories correctly interact with their data sources
  - _Leverage: .design-specs/specs/design-6.md, gradle/libs.versions.toml_
  - _Requirements: 3_
  - _Prompt: Role: QA Engineer | Task: Create unit tests for `MeasurementRepositoryImpl` and `SettingsRepositoryImpl` using MockK. Verify that each repository method calls the corresponding DAO method exactly once with correct parameters. | Restrictions: Tests must be pure unit tests (no Room/Android dependencies). | Success: All repository methods are verified via unit tests._

- [x] 5. Create Repository Integration Tests
  - File: app/src/androidTest/java/com/example/underpressure/data/repository/
  - Create `RepositoryIntegrationTest.kt`
  - Use an in-memory Room database to test the full stack from Repository to DB
  - Purpose: Verify end-to-end data persistence through the repository layer
  - _Leverage: app/src/androidTest/java/com/example/underpressure/data/local/RoomDatabaseTest.kt_
  - _Requirements: 1, 2_
  - _Prompt: Role: Android Developer | Task: Create an instrumented integration test `RepositoryIntegrationTest` in `com.example.underpressure.data.repository`. Use an in-memory `AppDatabase` to verify that data saved via repositories is correctly persisted and retrieved. | Restrictions: Follow existing patterns from `RoomDatabaseTest.kt`. | Success: Integration tests pass on an Android device/emulator._
