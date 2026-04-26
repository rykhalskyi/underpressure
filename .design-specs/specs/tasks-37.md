  # Tasks - Issue 37: Onboarding Popup

## Phase 1: Data & Resources
- [ ] 1. Add onboarding strings to `app/src/main/res/values/strings.xml` and translations.
  - File: `app/src/main/res/values/strings.xml`
  - Description: Add strings for slide captions and descriptions as specified in the issue.
  - _Leverage_: Existing string groups in `strings.xml`.
  - _Prompt_: Add the following strings to `app/src/main/res/values/strings.xml`:
    - `onboarding_slide1_caption`, `onboarding_slide1_description`
    - `onboarding_slide2_caption`, `onboarding_slide2_description`
    - `onboarding_slide3_caption`, `onboarding_slide3_description`
    - `onboarding_slide4_caption`, `onboarding_slide4_description`
    - `button_onboarding_close`
    Use the text provided in Issue 37.

- [ ] 2. Update `AppSettingsEntity` to track onboarding status.
  - File: `app/src/main/java/com/otakeessen/underpressure/data/local/entities/AppSettingsEntity.kt`
  - Description: Add `lastOnboardedVersion: String? = null` to the entity.
  - _Leverage_: Existing entity structure.
  - _Prompt_: Modify `AppSettingsEntity` to include `val lastOnboardedVersion: String? = null`. Increment the database version in `AppDatabase.kt` if necessary (current version is 2).

## Phase 2: UI Components
- [ ] 3. Create `OnboardingSlide` data class and UI component.
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/onboarding/OnboardingSlide.kt`
  - Description: Defines the data structure for a slide and a Composable to render it.
  - _Leverage_: Material 3 `Column`, `Text`, and `Image` (using placeholder for now).
  - _Prompt_: Create `OnboardingSlide.kt`. Define a data class `OnboardingSlideData(val imageRes: Int?, val caption: String, val description: String)`. Create a Composable `OnboardingSlideView` that displays an image placeholder at the top, followed by the caption (title style) and description (body style).

- [ ] 4. Create `OnboardingDialog` with pager.
  - File: `app/src/main/java/com/otakeessen/underpressure/ui/onboarding/OnboardingDialog.kt`
  - Description: A dialog containing a `HorizontalPager` to swipe through slides.
  - _Leverage_: `androidx.compose.foundation.pager.HorizontalPager`, `androidx.compose.material3.AlertDialog`.
  - _Prompt_: Create `OnboardingDialog.kt`. Use `AlertDialog` or a full-screen `Dialog`. It should contain a `HorizontalPager` with 4 slides using data from strings. Include a "Close" button that calls an `onDismiss` callback. Add pager indicators (dots).

## Phase 3: Integration
- [ ] 5. Update `SettingsViewModel` and `SettingsScreen`.
  - Files: `app/src/main/java/com/otakeessen/underpressure/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt`
  - Description: Add logic to update `lastOnboardedVersion` and trigger the dialog from the "About" section.
  - _Leverage_: Existing `updateSettings` patterns.
  - _Prompt_: In `SettingsViewModel`, add `setOnboardingSeen(version: String)`. In `SettingsScreen`, add an "About UnderPressure" list item (or update existing one) that opens the `OnboardingDialog` when clicked.

- [ ] 6. Trigger onboarding in `MainActivity`.
  - File: `app/src/main/java/com/otakeessen/underpressure/MainActivity.kt`
  - Description: Check if onboarding is needed on app start.
  - _Leverage_: `tableViewModel` or `settingsViewModel` to access settings.
  - _Prompt_: In `MainActivity`, observe the settings. Compare current `versionName` with `lastOnboardedVersion`. If they differ, show `OnboardingDialog`. When the dialog is dismissed, call `viewModel.setOnboardingSeen(versionName)`.
