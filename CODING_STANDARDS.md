# Coding Standards

## Scope
These standards apply to the TestCurrency Android app.

## Kotlin and Compose
- Keep composables small and focused on UI rendering.
- Hoist state out of composables when business logic or asynchronous work is involved.
- Prefer immutable UI state objects and state updates via `copy`.
- Use `remember` only for UI-local objects that should survive recomposition.
- Keep side effects explicit. Network calls and coroutine launching belong in controllers or other non-UI layers, not directly in composable bodies.
- Use Material 3 components and theme tokens consistently.
- Use descriptive names and fix typos in file and symbol names instead of preserving incorrect names.
- Keep formatting idiomatic Kotlin: trailing commas where supported, expression bodies for simple functions, and one responsibility per function.

## Project structure
- UI belongs in composables and activities.
- Business logic belongs in controller/domain classes.
- Data catalogs and service integrations belong outside UI files.
- Prefer adding to the existing package structure before creating new top-level areas.

## Testing
- Write tests for all new behavior and bug fixes.
- Prefer local unit tests for domain and formatting logic.
- Use Compose UI tests for screen behavior, visible text, and user interactions.
- Keep instrumentation tests focused on Android integration or Compose rendering that cannot be covered by local unit tests.
- Replace template example tests with behavior-based tests tied to the app.
- Test user-observable outcomes, not implementation details.

## Compose test expectations
- Add or update at least one Compose UI test when changing `ConverterScreen` behavior.
- Query UI with stable text or semantics and assert the visible result after user actions.
- Prefer tests that cover amount entry, currency selection, swap behavior, loading state, and error rendering when those areas change.

## Review checklist
- Does UI logic stay out of composables where possible?
- Is state immutable and updated predictably?
- Are new code paths covered by unit tests or Compose tests?
- Are names clear and free of accidental typos?
- Does the change preserve Material 3 and existing project conventions?
