# TODO for `mycore-webcli`

## Current follow-up items

### 1. Decide whether `noPermission` should stay plain text

The backend still sends the special non-JSON frame:

- `noPermission`

This is preserved for compatibility, but a JSON error envelope would be cleaner.

### 2. Consider extracting the state split further

The Vue app now uses a transport composable plus component-local state.

If the UI grows, move more state into a dedicated composable or store module.

### 3. Reduce wording-based selectors in frontend tests

The Vitest suite is now split and easier to maintain, but some app-level tests still locate toolbar actions by visible text.

If those labels change, unrelated tests will fail. Prefer more stable selectors or more focused component-level assertions where practical.
