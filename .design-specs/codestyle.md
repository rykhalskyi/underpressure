# Code Style and Best Practices

**Before you begin:** This project is an Android application developed with **Kotlin** and **Jetpack Compose**. It follows modern Android development standards, emphasizing reactive programming and declarative UI.

---

# Foundational Principles

This project adheres to established software engineering principles to ensure the codebase is robust, maintainable, and scalable. When writing code, consider the following principles. Use the "Specific Guidelines" section below to document concrete applications of these principles.

*   **SOLID:**
    *   **S**ingle Responsibility Principle: A class should have only one reason to change. (e.g., ViewModels should only manage UI state, not business logic or networking).
    *   **O**pen/Closed Principle: Software entities should be open for extension, but closed for modification.
    *   **L**iskov Substitution Principle: Subtypes must be substitutable for their base types.
    *   **I**nterface Segregation Principle: Clients should not be forced to depend on interfaces they do not use.
    *   **D**ependency Inversion Principle: High-level modules should not depend on low-level modules. Both should depend on abstractions.

*   **KISS (Keep It Simple, Stupid):** Prefer simple, straightforward solutions over complex ones. Avoid unnecessary complexity in Composable functions and logic.

*   **YAGNI (You Ain't Gonna Need It):** Do not add functionality until it is deemed necessary. Avoid premature optimization and feature creep.

*   **DRY (Don't Repeat Yourself):** Avoid duplicating code. Centralize logic and UI components (like Custom Themes or common UI elements) in a single, authoritative place.

*   **Clean Code & Clean Architecture:** Write code that is easy to read, understand, and maintain. Structure the application in a way that separates concerns, making it independent of frameworks, UI, and databases. Aim for a layered architecture: UI (Compose/ViewModels) -> Domain (UseCases/Models) -> Data (Repositories/APIs/DB).

*   **GRASP (General Responsibility Assignment Software Patterns):** A set of patterns for assigning responsibilities to classes and objects in object-oriented design.

---

# Specific Guidelines

## Jetpack Compose: Modifier Parameter Placement

**Problem:**
Composables that don't accept a `Modifier` or place it incorrectly make it difficult for parent Composables to layout, style, or add interactions to their children.

```kotlin
// Problematic: Hard to position from the outside
@Composable
fun UserAvatar(name: String) {
    Image(
        painter = painterResource(id = R.drawable.avatar),
        contentDescription = null,
        modifier = Modifier.size(48.dp) // Fixed size, no external control
    )
}
```

**Recommendation:**
Every Composable that emits UI should accept a `Modifier` as its first optional parameter and pass it to its root UI element.

1.  **Accept Modifier:** Add `modifier: Modifier = Modifier` to the parameters.
2.  **Pass to root:** Chain external modifiers with internal ones.

```kotlin
@Composable
fun UserAvatar(
    name: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.avatar),
        contentDescription = null,
        modifier = modifier.size(48.dp) // External modifier applied first
    )
}
```

**Rationale:**
This ensures Composables are reusable and follow the Compose design system, allowing parents to control layout, padding, and constraints.

---

## State Management: ViewModel and StateFlow

**Problem:**
Managing UI state directly in Composable functions or using mutable variables leads to unpredictable UI behavior, memory leaks, and difficulty testing.

```kotlin
// Problematic: State is lost on configuration changes
@Composable
fun UserProfile() {
    var userName by remember { mutableStateOf("") }
    // ... logic to fetch user ...
}
```

**Recommendation:**
Use `ViewModel` to hold and manage UI state. Expose state using `StateFlow` and collect it in Composables using `collectAsStateWithLifecycle`.

1.  **Define UI State:** Use a data class for the screen state.
2.  **ViewModel:** Manage state transitions in the ViewModel.
3.  **Collect in UI:** Use lifecycle-aware collection.

```kotlin
// Data/Domain layer
data class UserUiState(val name: String = "", val isLoading: Boolean = false)

// ViewModel
class UserViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(UserUiState())
    val uiState: StateFlow<UserUiState> = _uiState.asStateFlow()
}

// UI
@Composable
fun UserProfile(viewModel: UserViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // ... use state ...
}
```

**Rationale:**
ViewModels survive configuration changes (like rotation), and StateFlow provides a robust, thread-safe way to handle asynchronous updates.

---

## Resource Management: String Hardcoding

**Problem:**
Hardcoding strings directly in the UI makes internationalization (i18n) and localization impossible and leads to inconsistent messaging.

```kotlin
// Problematic
Text(text = "Submit Application")
```

**Recommendation:**
Always define user-facing strings in `strings.xml` and access them via `stringResource`.

1.  **Add to strings.xml:**
    ```xml
    <string name="submit_label">Submit Application</string>
    ```
2.  **Use in Composable:**
    ```kotlin
    Text(text = stringResource(id = R.string.submit_label))
    ```

**Rationale:**
Centralizing strings facilitates translation and ensures a consistent UI across the entire application.

---

## Kotlin Coding Style: Trailing Commas

**Problem:**
Adding or removing items in lists or parameters often results in multi-line diffs in version control, making code reviews noisier.

```kotlin
// No trailing comma
val items = listOf(
    "Apple",
    "Orange"
)
```

**Recommendation:**
Use trailing commas for multi-line parameter lists, collections, and arguments.

```kotlin
val items = listOf(
    "Apple",
    "Orange", // Trailing comma
)
```

**Rationale:**
This minimizes diff noise when adding new items and makes the code cleaner.

---

## State Management: ViewModel State Update

**Problem:**
Updating `MutableStateFlow` using `value = value.copy(...)` is not thread-safe and can lead to race conditions if multiple updates happen concurrently.

```kotlin
// Problematic: Not thread-safe
_uiState.value = _uiState.value.copy(isLoading = false)
```

**Recommendation:**
Always use the `.update { ... }` extension function for `MutableStateFlow` to ensure atomic updates.

```kotlin
// Correct: Thread-safe update
_uiState.update { it.copy(isLoading = false) }
```

**Rationale:**
The `update` function ensures that the state transition is atomic and based on the most recent value, preventing lost updates in concurrent environments.

---

## Architecture: Dependency Injection (Constructor Injection)

**Problem:**
Hardcoding dependencies inside classes (e.g., creating a Repository inside a ViewModel) makes the code difficult to test and violates the Dependency Inversion Principle.

```kotlin
// Problematic: Hard to test
class SettingsViewModel : ViewModel() {
    private val repository = SettingsRepositoryImpl() // Hardcoded dependency
}
```

**Recommendation:**
Use constructor injection for all dependencies. Pass interfaces instead of concrete implementations where possible.

```kotlin
// Correct: Dependencies injected via constructor
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {
    // ...
}
```

**Rationale:**
Constructor injection makes dependencies explicit, facilitates easy unit testing with mocks, and allows for better decoupling of components.
