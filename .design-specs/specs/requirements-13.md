# Requirements Document - Global Alarm Toggle

## Introduction

The Global Alarm Toggle feature introduces a master switch to enable or disable all measurement reminders across the application. This provides users with a quick way to silence all notifications without individually deactivating each slot, while still preserving their specific slot configurations for later restoration.

## Alignment with Product Vision

Consistency is crucial for meaningful blood pressure tracking. While reminders (alarms) help maintain this consistency, users sometimes need to temporarily disable all interruptions (e.g., during vacations or illness). By providing a global toggle, we support the user's need for flexibility while ensuring that returning to a consistent routine is as simple as a single tap, aligning with the project's goal of being a reliable yet unobtrusive health companion.

## Requirements

### Requirement 1: Master Alarm Toggle in Settings

**User Story:** As a user, I want a single switch in the Settings screen to enable or disable all measurement alarms, so that I can quickly manage my notification preferences.

#### Acceptance Criteria

1. WHEN the Settings screen is opened THEN the system SHALL display a "Global Alarm" toggle after a separator below the individual slot toggles.
2. IF the "Global Alarm" toggle is switched OFF THEN the system SHALL immediately cancel all scheduled alarms for all measurement slots.
3. IF the "Global Alarm" toggle is switched ON THEN the system SHALL schedule alarms for all active slots that have their individual alarms enabled.
4. WHEN the "Global Alarm" state is changed THEN the system SHALL persist this state in the local database.

### Requirement 2: Quick Toggle on Main Screen

**User Story:** As a user, I want to be able to toggle all alarms directly from the main screen, so that I don't have to navigate to settings for this frequent action.

#### Acceptance Criteria

1. WHEN the Measurement Table (Main Screen) is displayed THEN the system SHALL provide an action button/icon in the TopAppBar to toggle the global alarm state.
2. IF alarms are currently enabled THEN the icon SHALL represent an active state (e.g., Alarm icon).
3. IF alarms are currently disabled THEN the icon SHALL represent a silenced state (e.g., Alarm Off icon).
4. WHEN the icon is clicked THEN the system SHALL toggle the `masterAlarmEnabled` state and update all alarms accordingly.

### Requirement 3: Reliability and Persistence

**User Story:** As a user, I want my global alarm preference to be respected even after my device restarts, so that I don't receive unexpected notifications or miss expected ones.

#### Acceptance Criteria

1. WHEN the device finishes booting (ACTION_BOOT_COMPLETED) THEN the system SHALL check the `masterAlarmEnabled` flag before rescheduling any alarms.
2. IF `masterAlarmEnabled` is FALSE THEN the system SHALL NOT schedule any alarms during the boot sequence.
3. WHEN any individual slot settings (time or activity) are changed while `masterAlarmEnabled` is FALSE THEN the system SHALL NOT schedule an alarm for that slot.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility Principle**: The `AlarmScheduler` should handle the logic for interpreting the master toggle and individual slot states.
- **Modular Design**: The global toggle state should be part of the `AppSettingsEntity` and managed via the `SettingsRepository`.
- **Clear Interfaces**: ViewModels for both Settings and Main screens should interact with the same repository methods to ensure consistency.

### Performance
- Alarms cancellation and scheduling should happen asynchronously to avoid blocking the UI thread.
- The impact on battery life should remain minimal by using `AlarmManager` appropriately (respecting `canScheduleExactAlarms`).

### Reliability
- The master toggle state must be transactionally saved to the Room database to prevent desynchronization between UI and actual alarm state.

### Usability
- The UI should clearly indicate the state of the global alarm on both screens.
- A horizontal separator in Settings should clearly distinguish between individual slot settings and the global master control as per design specifications.

## Reference Steering Documents
- **Structure**: Adheres to the layered architecture (UI -> Domain -> Data) as defined in `.design-specs/structure.md`.
- **Tech**: Uses Jetpack Compose and Material 3 icons for the UI, and Room for persistence as per `.design-specs/tech.md`.
- **Codestyle**: Follows Kotlin and Compose best practices (State hoisting, ViewModel-managed state) as defined in `.design-specs/codestyle.md`.
