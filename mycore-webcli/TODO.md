# TODO for `mycore-webcli`

## Purpose
This file captures observations from the current implementation and turns them into concrete improvement ideas.

Related docs:
- `PROTOCOL.md` – canonical frontend/backend message contract
- `TESTING.md` – recommended testing direction and repo-local tooling precedent

## High-priority observations

### 1. `RESTService` is misnamed
Observation:
- `src/main/ts/service/rest.service.ts` is the central WebSocket transport.
- It does not meaningfully use Angular HTTP for backend business calls.

Why it matters:
- The name hides the real architecture.
- It makes protocol testing and maintenance harder to reason about.

Suggested action:
- Rename or split it into something like:
  - `webcli-transport.service.ts`
  - `webcli-protocol.service.ts`

---

### 2. Raw `WebSocket` usage is not abstracted
Observation:
- The service creates `new WebSocket(...)` directly.
- This makes unit testing more awkward than necessary.

Why it matters:
- Tests need to stub browser globals.
- Connection lifecycle, retry logic, and protocol parsing are tightly coupled.

Suggested action:
- Introduce a tiny transport wrapper interface around socket operations.
- Use a fake implementation in tests.

---

### 3. Socket reconnect behavior is fragile
Observation:
- The backend may close the socket after command processing completes.
- The frontend only reconnects lazily when sending a new message.

Why it matters:
- Passive UI states can become stale until the next user action.
- Connection state is implicit and not exposed in the UI.

Suggested action:
- Handle `onclose` explicitly.
- Optionally reconnect proactively.
- Surface connection state in the UI for debugging.

---

### 4. The frontend mixes transport, state, and UI concerns
Observation:
- `RESTService` handles URL construction, reconnection, message serialization, deserialization, and RxJS subject fan-out.
- Several components directly manipulate the DOM.

Why it matters:
- Harder to test in isolation.
- More brittle under framework upgrades.

Suggested action:
- Separate transport from protocol mapping.
- Prefer Angular bindings over direct DOM updates where practical.

---

### 5. Direct DOM manipulation is used in multiple components
Observation:
- `log.component.ts` appends DOM nodes manually.
- `queue.component.ts` writes HTML directly with `innerHTML`.
- `app.component.ts` clicks a tab through raw DOM access.

Why it matters:
- Harder to test.
- Easier to introduce XSS or rendering issues.
- Fights Angular's change-detection model.

Suggested action:
- Move log and queue rendering to component state + templates.
- Avoid `innerHTML` for queue rendering.
- Replace raw tab clicking with state-driven tab selection.

---

### 6. Queue rendering currently uses HTML string concatenation
Observation:
- Queue entries are joined with `</br>` and injected into a `<pre>` via `innerHTML`.

Why it matters:
- Commands containing HTML-sensitive characters are not escaped by design.
- This is a potential presentation and security risk.

Suggested action:
- Render queue entries with `*ngFor` and text binding.
- Keep truncation logic in component state.

---

### 7. The codebase is on legacy frontend dependencies
Observation:
- Angular 2.0.0-rc.1
- TypeScript 1.8.10
- SystemJS 0.21
- Bootstrap 5.3.x on top of legacy Angular-era templates and custom CSS
- RxJS beta series

Why it matters:
- Tooling and security support are still very limited outside the Bootstrap layer.
- Modern testing/documentation/examples will not work unchanged.
- Developer onboarding cost is higher.

Suggested action:
- Document the legacy stack clearly.
- Consider a phased modernization plan before adding larger new features.

---

### 8. A stale vendored Bootstrap stylesheet still exists under `src/main/resources`
Observation:
- `index.html` now loads Bootstrap from copied npm assets under `node_modules/bootstrap.min.css`.
- The old checked-in file `src/main/resources/META-INF/resources/modules/webcli/css/bootstrap.min.css` still exists and appears to be a legacy Bootstrap 4 alpha snapshot.

Why it matters:
- It is no longer the active source of truth.
- It can confuse future maintenance and grep-based audits.

Suggested action:
- Remove the stale vendored stylesheet once downstream packaging assumptions are double-checked.
- Keep npm + Grunt copy as the only Bootstrap asset path.

---

### 9. `angular2-in-memory-web-api` is present but unused
Observation:
- The package is copied to the browser output.
- The current app does not use it.

Why it matters:
- Extra dependency and copied files with no current value.
- Could mislead maintainers into expecting an HTTP-mocked architecture.

Suggested action:
- Remove it if no longer needed.
- Or document why it is intentionally kept.

---

### 10. `Http` is injected but effectively unused
Observation:
- `RESTService` injects Angular `Http`, but the service does not use it for API requests.

Why it matters:
- Creates noise and confusion.

Suggested action:
- Remove the unused dependency if safe.
- Re-check whether it was meant for an abandoned REST path.

---

### 11. Browser storage concerns are spread across components
Observation:
- Settings and command history are read/written directly from component code.

Why it matters:
- Repeated localStorage access patterns are harder to test.
- Validation/defaulting behavior is duplicated conceptually.

Suggested action:
- Introduce a small client-side persistence service.
- Centralize storage keys and defaults.

---

## Medium-priority improvements

### 12. Strengthen typing for backend messages
Observation:
- Command and protocol payloads are only loosely typed.
- Example: `Commands.commands` is declared as `any`.

Suggested action:
- Define explicit TypeScript interfaces for:
  - command groups,
  - command entries,
  - log events,
  - queue messages,
  - protocol envelopes.

---

### 13. Add protocol-focused tests
Observation:
- The transport contract is small and stable enough to test directly.
- `PROTOCOL.md` now centralizes the message catalog that those tests should follow.

Suggested action:
- Add tests for:
  - socket URL construction,
  - outgoing message encoding,
  - incoming message routing,
  - `noPermission` handling,
  - retry behavior,
  - queue truncation display behavior.
- Prefer the repo-local `mycore-mets` precedent for the first iteration: Jasmine + Grunt, with a Maven `test`-phase hook once the suite stabilizes.

---

### 14. Add component tests around local UX behaviors
Suggested coverage:
- Enter executes current command
- Up/Down navigate command history
- Tab selects the first `{0}` placeholder
- log history is trimmed to configured size
- settings read/write localStorage defaults correctly

---

### 15. Consider a clearer protocol document
Observation:
- The protocol is simple but was previously implicit in code.

Suggested action:
- Keep `PROTOCOL.md` in sync with the real message handlers.
- Keep `README.md` and `TESTING.md` aligned with `PROTOCOL.md` instead of duplicating protocol rules independently.
- If the protocol grows substantially, split request/response fixtures into dedicated test data files.

---

## Longer-term modernization ideas
- Replace SystemJS-based loading with a modern bundler.
- Upgrade Angular incrementally or rewrite the small UI with a lighter modern frontend stack.
- Replace jQuery/Bootstrap runtime behavior with framework-native UI state.
- Separate UI state management from transport concerns.
- Introduce a stable test harness and CI frontend checks.

## Quick wins
1. Add frontend unit tests with a fake backend transport.
2. Replace queue `innerHTML` rendering with template rendering.
3. Define typed message interfaces.
4. Extract localStorage handling into a helper service.
5. Expose socket connection state for easier debugging.
