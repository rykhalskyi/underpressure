# Requirements Document - Issue #12: Per-Slot Alarm Implementation

## Introduction
The Per-Slot Alarm feature provides users with daily repeating notifications for each active and enabled measurement slot. This ensures users are reminded to take their blood pressure readings at the scheduled times, improving compliance with their health monitoring routine.

## Alignment with Product Vision
By providing timely reminders, "UnderPressure" helps users maintain a consistent measurement schedule, which is critical for accurate health data collection and long-term cardiovascular monitoring.

## Requirements

### Requirement 1: Per-Slot Daily Alarms
**User Story:** As a user, I want to have an alarm for each active time slot, so that I don't forget to make measurements at the scheduled times.

#### Acceptance Criteria
1. WHEN a time slot is active and its alarm is enabled, THEN the system SHALL schedule a daily repeating alarm for that slot's time.
2. WHEN the scheduled time for an enabled slot is reached, THEN the system SHALL trigger a notification.
3. IF a slot is disabled or its alarm toggle is turned off, THEN the system SHALL cancel any scheduled alarm for that slot.
4. WHEN the user clicks on the notification, THEN the system SHALL open the main screen of the application.
5. IF the system time or slot configuration changes, THEN the system SHALL reschedule all enabled slot alarms accordingly.

### Requirement 2: Notification Experience
**User Story:** As a user, I want the notification to be noticeable but not intrusive, so that I am alerted without being disrupted for too long.

#### Acceptance Criteria
1. WHEN an alarm triggers, THEN the system SHALL display a notification with a brief sound or vibration.
2. IF the notification is not dismissed, THEN the system SHALL NOT continue the alarm for longer than 5 seconds (standard Android notification behavior).
3. WHEN the notification is displayed, THEN it SHALL include a clear title and message indicating which measurement slot is due.

### Requirement 3: Persistence and Reliability
**User Story:** As a user, I want my alarms to remain active even after my device restarts, so that I don't miss measurements after a reboot.

#### Acceptance Criteria
1. WHEN the device finishes booting (ACTION_BOOT_COMPLETED), THEN the system SHALL automatically reschedule all enabled slot alarms.
2. WHEN the application is updated or reinstalled, THEN the system SHALL ensure alarms are correctly initialized based on stored settings.

## Non-Functional Requirements

### Code Architecture and Modularity
- **Single Responsibility**: Alarm scheduling logic SHALL be encapsulated in a dedicated helper or service (e.g., `AlarmScheduler`).
- **Modular Design**: Notification handling SHALL be separated from the main UI logic using a `BroadcastReceiver`.
- **Dependency Management**: Alarms SHALL be scheduled using `AlarmManager` for precise daily triggers.

### Performance
- Alarm scheduling operations SHALL be performed on background threads.
- Battery impact SHALL be minimized by using efficient `AlarmManager` APIs.

### Reference Steering Documents
- **Structure**: Follows standard Android patterns for background tasks and notifications.
- **Tech**: Uses `AlarmManager`, `NotificationManager`, and `BroadcastReceiver`.
- **Codestyle**: Follows project guidelines for clean code and SOLID principles.
