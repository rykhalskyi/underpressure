# Design Document - Issue #5: Implement Room Database

## Overview
This design implements the local storage layer using Room. It introduces two main entities for blood pressure measurements and application settings, their respective DAOs, and the database configuration. The persistence layer will be located in the `data.local` package.

## Steering Document Alignment

### Technical Standards (tech.md)
- **Room Persistence Library**: Used for local storage as specified in `tech.md`.
- **Kotlin Coroutines**: All database operations will be `suspend` functions, ensuring background thread execution.
- **KSP**: Room will use Kotlin Symbol Processing for code generation.

### Project Structure (structure.md)
- **Data Layer Isolation**: All Room-related files will be placed in `com.example.underpressure.data.local`.
- **Package Organization**: 
    - `entities/` for Room entities.
    - `dao/` for Data Access Objects.
    - `database/` for the Room database class.

## Code Reuse Analysis
As the project is currently a fresh scaffold, this feature will be the first implementation of the data layer.

### Existing Components to Leverage
- **Coroutines**: Built-in Kotlin coroutines for asynchronous tasks.

### Integration Points
- **Domain Layer (Planned)**: Repositories in the domain layer will later wrap these DAOs to provide data to the ViewModels.

## Architecture

The architecture follows a standard Room implementation pattern:
- **Entities**: Represent database tables.
- **DAOs**: Define methods for data access.
- **AppDatabase**: Main access point for the persistent data.

```mermaid
graph TD
    UI[UI/ViewModel] --> Repos[Repository (Planned)]
    Repos --> MDAO[MeasurementDao]
    Repos --> SDAO[AppSettingsDao]
    MDAO --> DB[AppDatabase]
    SDAO --> DB[AppDatabase]
    DB --> SQLite[(SQLite DB)]
```

## Components and Interfaces

### MeasurementDao
- **Purpose:** Handles CRUD operations for `MeasurementEntity`.
- **Interfaces:**
    - `insert(measurement: MeasurementEntity)`
    - `update(measurement: MeasurementEntity)`
    - `delete(measurement: MeasurementEntity)`
    - `getByDate(date: String): Flow<List<MeasurementEntity>>`
    - `getAll(): Flow<List<MeasurementEntity>>`

### AppSettingsDao
- **Purpose:** Handles CRUD operations for `AppSettingsEntity`.
- **Interfaces:**
    - `insertOrUpdate(settings: AppSettingsEntity)`
    - `getSettings(): Flow<AppSettingsEntity?>`

## Data Models

### MeasurementEntity
```kotlin
@Entity(tableName = "measurements", indices = [Index(value = ["date"])])
data class MeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val slotIndex: Int,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### AppSettingsEntity
```kotlin
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1, // Singleton settings record
    val masterAlarmEnabled: Boolean = false,
    val slotTimes: List<String>, // Serialized list or separate table/fields
    val slotAlarmsEnabled: List<Boolean>
)
```
*Note: For lists like `slotTimes`, a TypeConverter will be implemented.*

## Error Handling

### Error Scenarios
1. **Scenario 1: Constraint Violation**
   - **Handling:** Use `@OnConflictStrategy.REPLACE` or explicit checks in the repository.
   - **User Impact:** Data is either overwritten or the user is notified if a conflict is critical.

2. **Scenario 2: Database Migration Failure**
   - **Handling:** Provide a fallback to destructive migration during early development; implement proper migrations for production.
   - **User Impact:** Data might be lost if migration fails during development; production will require robust migration scripts.

## Testing Strategy

### Unit Testing (Local DB Tests)
- **Room In-Memory DB**: Use `Room.inMemoryDatabaseBuilder` for testing.
- **Key components to test**:
    - Insertion and retrieval of measurements.
    - Querying by date.
    - Updating settings.

### Integration Testing
- Test the integration between DAOs and (future) Repositories.
