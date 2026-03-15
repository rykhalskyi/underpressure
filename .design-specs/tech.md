# Technology Stack

## Project Type
This is a modern **Android mobile application** built for the Android platform.

## Core Technologies

### Primary Language(s)
- **Language**: Kotlin 2.0.21
- **Runtime/Compiler**: JVM 11
- **Language-specific tools**: Kotlin Symbol Processing (KSP) 2.0.21-1.0.26, Kotlin Coroutines (1.10.1) for asynchronous programming.

### Key Dependencies/Libraries
- **Jetpack Compose (BOM 2024.09.00)**: Modern toolkit for building native UI.
- **AndroidX Core KTX (1.17.0)**: Kotlin extensions for Android framework APIs.
- **AndroidX Lifecycle (2.10.0)**: Lifecycle-aware components including ViewModel and Flow support.
- **AndroidX Activity Compose (1.12.4)**: Integration of Compose with Android Activities.
- **Material 3**: Google's latest design system for UI components.
- **Room Persistence Library (2.6.1)**: SQLite object mapping for local data storage.

### Application Architecture
- **Pattern**: MVVM (Model-View-ViewModel) with Clean Architecture principles.
- **Structure**:
    - **UI Layer**: Jetpack Compose functions for rendering UI and ViewModels for state management.
    - **Domain Layer**: Pure Kotlin logic containing Repository interfaces and Validators.
    - **Data Layer**: Repositories for data orchestration and Room database management.

### Data Storage
- **Primary storage**: Room Persistence Library (SQLite).
- **Caching**: In-memory caching within Repositories.
- **Data formats**: SQLite (via Room), Kotlin objects.

### External Integrations
- **System Services**: AlarmManager for scheduling measurement reminders.
- **Receivers**: BroadcastReceivers for alarm events and boot completion.

## Development Environment

### Build & Development Tools
- **Build System**: Gradle 8.13 (Kotlin DSL)
- **Package Management**: Gradle Version Catalog (`libs.versions.toml`)
- **Development workflow**: Android Studio with Hot Reload (Live Edit/Apply Changes).

### Code Quality Tools
- **Static Analysis**: Android Lint.
- **Formatting**: ktlint (via Gradle plugin).
- **Testing Frameworks**: 
    - **Unit Tests**: JUnit 4.13.2, MockK 1.13.13, Kotlinx Coroutines Test 1.10.1.
    - **Instrumentation Tests**: Compose UI Test, Espresso 3.7.0, UI Automator 2.3.0, MockK Android 1.13.13.
    - **Architecture Testing**: AndroidX Arch Core Testing 2.2.0.
- **Documentation**: Dokka (Planned).

### Version Control & Collaboration
- **VCS**: Git
- **Branching Strategy**: GitHub Flow (Feature branches -> Main).
- **Code Review Process**: Pull Requests on GitHub.

## Deployment & Distribution
- **Target Platform(s)**: Android devices (Mobile/Tablet).
- **Distribution Method**: Google Play Store (AAB - Android App Bundle).
- **Installation Requirements**: Android 10.0 (API 29) minimum.
- **Update Mechanism**: Play Store auto-updates.

## Technical Requirements & Constraints

### Performance Requirements
- **Startup Time**: Cold start < 2 seconds.
- **UI Performance**: Consistent 60/120 FPS for smooth animations using Compose.
- **Memory Usage**: Optimized for devices with 4GB+ RAM.

### Compatibility Requirements  
- **Platform Support**: Android 10 (API 29) to Android 16 (API 36).
- **SDK Versions**: `minSdk` 29, `targetSdk` 36, `compileSdk` 36.
- **Standards Compliance**: Material Design 3 guidelines.

### Security & Compliance
- **Security Requirements**: ProGuard/R8 for code obfuscation.
- **Threat Model**: Secure local storage (Room), protection against reverse engineering.

## Technical Decisions & Rationale

### Decision Log
1. **Jetpack Compose**: Chosen over XML Views for faster development, declarative UI, and better state management integration.
2. **Kotlin DSL (Gradle)**: Used for build scripts to provide better type safety and IDE support compared to Groovy.
3. **Version Catalog**: Centralized dependency management to ensure consistency across modules.
4. **Room Database**: Selected for robust, type-safe local persistence with built-in migration support.

## Known Limitations
- **Build Times**: Gradle builds can be slow; requires incremental build optimization as the project grows.
- **API level constraints**: Some modern features require higher API levels, though the app targets API 29+.
