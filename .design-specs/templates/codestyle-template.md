# Code Style and Best Practices

**Before you begin:** Identify the primary programming languages, frameworks, and technologies used in this project. The guidelines you document should be relevant to this specific technology stack.

---

# Foundational Principles

This project adheres to established software engineering principles to ensure the codebase is robust, maintainable, and scalable. When writing code, consider the following principles. Use the "Specific Guidelines" section below to document concrete applications of these principles.

*   **SOLID:**
    *   **S**ingle Responsibility Principle: A class should have only one reason to change.
    *   **O**pen/Closed Principle: Software entities should be open for extension, but closed for modification.
    *   **L**iskov Substitution Principle: Subtypes must be substitutable for their base types.
    *   **I**nterface Segregation Principle: Clients should not be forced to depend on interfaces they do not use.
    *   **D**ependency Inversion Principle: High-level modules should not depend on low-level modules. Both should depend on abstractions.

*   **KISS (Keep It Simple, Stupid):** Prefer simple, straightforward solutions over complex ones. Avoid unnecessary complexity.

*   **YAGNI (You Ain't Gonna Need It):** Do not add functionality until it is deemed necessary. Avoid premature optimization and feature creep.

*   **DRY (Don't Repeat Yourself):** Avoid duplicating code. Centralize logic and configuration in a single, authoritative place.

*   **Clean Code & Clean Architecture:** Write code that is easy to read, understand, and maintain. Structure the application in a way that separates concerns, making it independent of frameworks, UI, and databases.

*   **GRASP (General Responsibility Assignment Software Patterns):** A set of patterns for assigning responsibilities to classes and objects in object-oriented design.

---

# Specific Guidelines

## [Guideline Title]

**Problem:**
[Describe a specific, recurring code issue or anti-pattern. Provide a small, concise code example of what *not* to do.]

*Example Problem:*
The same value (e.g., a color, a size) is hardcoded in multiple component style files, leading to inconsistencies and difficult maintenance.

```scss
// component-a.scss
.header {
  height: 64px;
}

// component-b.scss
.container {
  padding-top: 64px;
}
```

**Recommendation:**
[Provide a clear, step-by-step solution to the problem. Include a concise code example of the *correct* approach.]

*Example Recommendation:*
Centralize shared values in a `_variables.scss` file and import it where needed.

1.  **Create a central variables file:** `src/styles/_variables.scss`
    ```scss
    $topbar-height: 64px;
    ```
2.  **Include it in `angular.json`** to make it globally available.
3.  **Use the variable:**
    ```scss
    @import 'variables';

    .header {
      height: $topbar-height;
    }
    ```

**Rationale:**
[Explain *why* this guideline is important. What are the benefits? (e.g., "Improves maintainability," "Reduces bugs," "Enhances performance").]

*Example Rationale:*
This approach follows the DRY (Don't Repeat Yourself) principle. It ensures that shared values are defined in a single source of truth, making the codebase easier to maintain and update.

---

*Repeat the "Specific Guidelines" section for each new guideline.*
