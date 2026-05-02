# Implementation Plan: Improving First-Run User Experience (Issue #42)

## Overview
Enhance user understanding of the "Timeslots" measurement system during the first-run experience. New users often find the restriction on measurement times confusing. The solution involves introducing a `slotModifiedFlags` property per timeslot and providing proactive guidance when users attempt to add measurements.

## Proposed Changes

### 1. Data Layer Enhancements
- **Modify `AppSettingsEntity`** to track slot modification state with `slotModifiedFlags: List<Boolean> = listOf(false, false, false, false)` to track if a slot has been manually set or used.

### 2. Logic & Domain Layer
- **New Feature Logic**:
  - Implement a helper to track slot modifications and usage
  - Update "Add measurement" capability check: if all slots are modified, restrict entry; otherwise, allow but trigger guidance if outside +/- 15 min window
  - Implement auto-update logic: When a user accepts the prompt to use the current time, update the first available slot and mark it as modified

### 3. UI/UX Enhancements
- **Guidance Mechanism**:
  - Create a custom Dialog/Popup that explains the "UnderPressure" philosophy (measurement consistency)
  - Include an option in the dialog to automatically re-configure the first available slot to the current time and proceed with the measurement

## Implementation Details

### Slot Modification Tracking
- Track when user changes time of a timeslot or enters measurements for it
- Mark timeslots as *changed_by_user* if user changes time of the timeslot or already entered measurements for it
- On the start ALL slots have changed_by_user = false

## Slot Modification Logic
1. If there's at least one unchanged slot "Add measurements" is enabled, else - disabled and existing popup is shown
2. Adding data must mark this timeslot as set by user

## UI Implementation Details

### Guidance Mechanism Implementation
When user clicks on "Add measurements" outside the +/-15 minutes show popup:
"Hey, You are trying to add time outside the slot. UnderPressure allows users to enter data only in particular time to make measurements more precise. Do you want to set Slot {x} time to {current_time} and add measurements right now?" {x} is first unset timeslot.

If user press OK - set this timeslot time to current_time close this popup and show "Add Measurements" dialog box

## Implementation Steps

### Step 1: Data Layer Changes
1. Modify the AppSettingsEntity to include `slotModifiedFlags: List<Boolean> = listOf(false, false, false, false)` to track if a slot has been manually set or used.

### Step 2: Business Logic Implementation
1. Implement helper to determine if any timeslot remains "unmodified" (`slotModifiedFlags` will be false for unmodified slots)
2. Update "Add measurement" capability check: if all slots are modified, restrict entry; otherwise, allow but trigger guidance if outside +/- 15 min window
3. Implement auto-update logic: When a user accepts the prompt to use the current time, update the first unmodified slot and mark it as modified

### Step 3: UI Implementation
1. Create a custom Dialog/Popup that explains the "UnderPressure" philosophy (measurement consistency)
2. Include an option in the dialog to automatically re-configure the first unmodified slot to the current time and proceed with the measurement

## Test Plan
1. Unit Tests: Verify the `isModified` state update logic
2. Instrumented Tests: Ensure the "Add Measurement" UI flow correctly triggers the popup when outside windows and that the auto-configuration logic works as expected

## Potential Downsides & Mitigation
- **Increased complexity in state management**: Need careful synchronization of `slotModifiedFlags`
- **Unexpected Auto-Configuration**: Users might find auto-setting the timeslot confusing if it happens too automatically. *Mitigation: Always show an informative, action-oriented popup before making changes.*
- **Database Migrations**: Changing `AppSettingsEntity` requires a Room database migration. *Mitigation: Use proper migration strategies as defined in existing project patterns.*