# Project Structure

## Directory Organization

```
UnderPressure/
├── app/                        # Main Android application module
│   ├── src/
│   │   ├── main/               # Application source code
│   │   │   ├── java/           # Kotlin source files
│   │   │   │   └── com/example/underpressure/
│   │   │   │       ├── alarm/     # Alarm scheduling and management
│   │   │   │       ├── data/      # Data layer (Local DB, Repositories)
│   │   │   │       │   ├── local/ # Room database implementation
│   │   │   │       │   │   ├── converters/ # Room TypeConverters
│   │   │   │       │   │   ├── dao/        # Data Access Objects
│   │   │   │       │   │   ├── database/   # RoomDatabase definition
│   │   │   │       │   │   └── entities/   # Room Entities
│   │   │   │       │   └── repository/ # Repository implementations
│   │   │   │       ├── domain/    # Domain layer (Models, Interfaces, Use Cases)
│   │   │   │       │   ├── repository/ # Repository interfaces
│   │   │   │       │   └── validation/ # Business logic validators
│   │   │   │       ├── receiver/  # Broadcast receivers (Alarm, Boot)
│   │   │   │       ├── ui/        # UI layer (Screens, ViewModels, Theme)
│   │   │   │       │   ├── settings/ # Settings screen
│   │   │   │       │   │   └── components/ # UI components specific to settings
│   │   │   │       │   ├── table/    # Measurement table screen
│   │   │   │       │   └── theme/    # Material 3 theme definitions
│   │   │   │       └── MainActivity.kt # Entry point activity
│   │   │   ├── res/            # Android resources (strings, drawables, etc.)
│   │   │   ├── assets/         # Static assets
│   │   │   │   └── tessdata/   # Tesseract OCR data (placeholder)
│   │   │   └── AndroidManifest.xml # App manifest
│   │   ├── test/               # Local unit tests (JUnit, MockK)
│   │   └── androidTest/        # Instrumented tests (Compose UI Test, Espresso, UI Automator)
│   └── build.gradle.kts        # Module-level build configuration
├── gradle/                     # Gradle wrapper and version catalog
│   └── libs.versions.toml      # Centralized dependency management
├── .design-specs/              # Project design and technical documentation
│   ├── specs/                  # Detailed design and requirements specs
│   ├── templates/              # Markdown templates for documentation
│   ├── codestyle.md            # Coding standards
│   ├── structure.md            # Project structure
│   ├── tech.md                 # Tech stack
│   └── testing.md              # Testing strategy
├── .gemini/                    # Gemini CLI configuration and skills
│   ├── commands/               # Custom Gemini commands
│   └── skills/                 # Gemini specialized skills
├── build.gradle.kts            # Project-level build configuration
├── settings.gradle.kts         # Project settings and module inclusion
└── README.md                   # Project overview
```

## Naming Conventions

### Files
- **Kotlin Files**: `PascalCase.kt` (e.g., `MainActivity.kt`, `UserViewModel.kt`)
- **Resource Files**: `snake_case.xml` (e.g., `strings.xml`, `ic_launcher_background.xml`)
- **Build Scripts**: `kebab-case.gradle.kts` (standard Gradle convention)

### Code
- **Classes/Interfaces**: `PascalCase`
- **Compose Functions**: `PascalCase` (e.g., `@Composable fun UserProfile(...)`)
- **Functions/Methods**: `camelCase`
- **Constants**: `UPPER_SNAKE_CASE` or `top-level const val`
- **Variables/Parameters**: `camelCase`
- **Packages**: `lowercase` without underscores (e.g., `com.example.underpressure.ui.theme`)

## Import Patterns

### Import Order
1. **Standard Library**: `kotlin.*`, `java.*`
2. **Android & Jetpack**: `android.*`, `androidx.*`
3. **Third-party Libraries**: `com.google.*`, `io.mockk.*`, etc.
4. **Local Project Imports**: `com.example.underpressure.*`

## Code Structure Patterns

### Module/Class Organization
1. Package declaration
2. Imports
3. Top-level constants or functions
4. Class/Interface definition
    - Properties
    - Init block
    - Public functions
    - Private functions
    - Companion object

### Jetpack Compose Organization
- **State hoisting**: Pass state down and events up.
- **Parameters**: `Modifier` is always the first optional parameter.
- **Composition**: Small, reusable functions over large monolithic blocks.

## Code Organization Principles

1. **Single Responsibility**: Each file (ViewModel, Repository, Composable) handles one specific concern.
2. **Modularity**: Logic is separated into layers (UI, Domain, Data) to ensure decoupling.
3. **Testability**: Dependencies are injected to facilitate easy unit and instrumented testing.
4. **Consistency**: Adhere to the Material 3 design system and standard Android architectural patterns.

## Module Boundaries
- **UI Layer**: Depends on ViewModels and Domain repositories. Does not touch the database directly.
- **Domain Layer**: Contains pure business logic and repository interfaces. Does not depend on Android-specific frameworks.
- **Data Layer**: Implements Domain repositories and manages Room database entities and DAOs.

## Code Size Guidelines
- **File size**: Aim for < 300 lines; split into smaller files or modules if exceeded.
- **Composable size**: Keep functions focused; if a Composable has too many nested levels, extract sub-components.
- **Nesting depth**: Avoid nesting logic or UI deeper than 4 levels where possible.

## Documentation Standards
- **KDoc**: Use for public APIs, complex logic, and custom Composables.
- **Inline Comments**: Use sparingly for "why" rather than "what".
- **READMEs**: Maintain module-specific documentation as the project grows.

### Restrictions
- Dependency management is handled exclusively via Gradle and `libs.versions.toml`. No local `node_modules` or `venv` equivalents are permitted within the source tree.
