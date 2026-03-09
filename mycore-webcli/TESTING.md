# Testing `mycore-webcli` without a running MyCoRe backend

## Checklist
- Test the GUI without the full MyCoRe framework
- Mock the backend contract instead of starting the real server
- Cover both component behavior and protocol behavior
- Keep the approach realistic for the Angular 2.4.x stack
- Reuse useful testing patterns already present in other `mycore-*` modules
- Document the concrete commands that run the new suite

## Short answer
Yes, the GUI can be unit-tested without a running MyCoRe instance.

The key is to mock the real backend boundary:
- static assets do not need mocking for unit tests,
- the business interface is the WebSocket protocol implemented in `src/main/ts/service/rest.service.ts` and `src/main/java/org/mycore/webcli/resources/MCRWebCLIResourceSockets.java`.

Canonical protocol reference:
- `PROTOCOL.md`

For most tests, the best seam is one of these:
1. replace `RESTService` with a fake service, or
2. stub the browser `WebSocket` used by `RESTService`.

Notes about the current frontend stack:
- the GUI uses Bootstrap 5.3.3 assets copied from npm by `GruntFile.js`
- unit tests do not need Bootstrap's runtime JavaScript to verify the GUI behaviors listed in this document
- command-history modal and tab state are driven by component state, which keeps tests independent from jQuery and Bootstrap plugin internals

## What needs to be mocked

### Not necessary to mock
You do not need a running MyCoRe application for:
- Angular component rendering,
- localStorage behavior,
- command history navigation,
- log rendering behavior,
- settings UI behavior.

### Necessary to mock
You do need to mock the backend-facing transport behavior:
- command catalog loading,
- command execution messages,
- queue updates,
- log streaming,
- `continueIfOneFails` sync,
- permission denial (`noPermission`),
- socket close/reconnect behavior.

## The real backend contract to emulate

Use `PROTOCOL.md` as the canonical contract definition. The examples below are the minimum fixtures a fake backend or fake transport should support.

### Frontend -> backend
```json
{ "type": "getKnownCommands" }
{ "type": "run", "command": "process resource demo.txt" }
{ "type": "startLog" }
{ "type": "stopLog" }
{ "type": "clearCommandList" }
{ "type": "continueIfOneFails", "value": true }
```

### Backend -> frontend
```json
{
  "type": "getKnownCommands",
  "return": {
    "commands": [
      {
        "name": "Basic commands",
        "commands": [
          {
            "command": "process resource {0}",
            "help": "Execute the commands listed in the resource file {0}."
          }
        ]
      }
    ]
  }
}
```

```json
{ "type": "currentCommand", "return": "process resource demo.txt" }
```

```json
{ "type": "commandQueue", "return": ["cmd1", "cmd2"], "size": 2 }
```

```json
{
  "type": "log",
  "return": {
    "logLevel": "INFO",
    "message": "Processing command:'cmd1' (1 left)",
    "exception": null,
    "time": 1773054321000
  }
}
```

```json
{ "type": "continueIfOneFails", "value": false }
```

Special case:
```text
noPermission
```

## Recommended testing strategy

## Useful testing precedents in other `mycore-*` modules

### `mycore-mets`: best local precedent for frontend unit tests
What exists there:
- `@types/jasmine`
- `grunt-contrib-jasmine`
- `angular-mocks`
- TypeScript test sources under `src/test/ts`
- `GruntFile.js` tasks that compile test TypeScript and run Jasmine
- a module `pom.xml` execution that runs `grunt jasmine` during Maven `test`

Why it is useful here:
- it proves this repo already accepts Grunt-driven frontend unit tests
- it shows how to wire frontend tests into Maven without inventing a brand-new repo-wide standard
- the Jasmine style fits `mycore-webcli` better than Selenium for first-step protocol and component tests

What is reusable vs not reusable:
- reusable idea: Grunt + Jasmine test compilation and a Maven `test` phase hook
- reusable idea: browser-oriented tests with explicit fixture helpers
- not directly reusable: `angular-mocks`, because `mycore-webcli` is Angular 2.4.x, not AngularJS 1.x

### `mycore-viewer`: browser-level integration precedent
What exists there:
- Selenium dependencies and integration-test wiring in Maven

Why it is useful here:
- it is a precedent if `mycore-webcli` later needs real browser regression tests against a running app or fake server

Why it is not the first choice here:
- it is much heavier than needed for protocol and component tests
- it does not solve the immediate problem of isolating the WebSocket contract in fast unit tests

### What was not found elsewhere
During the repo scan, no clear sibling-module standard was found for:
- Karma
- Jest
- Vitest
- Playwright
- Cypress
- Puppeteer

That makes `mycore-mets` the least-surprising frontend test precedent currently available.

## Strategy A: component tests with a fake `RESTService`
This is the easiest way to test the GUI without MyCoRe.

### When to use it
Use this for tests that care about rendering and local interaction:
- command dropdown rendering,
- log rendering,
- queue rendering,
- command input behavior,
- settings synchronization inside the browser.

### Why it works well
Components already consume observables from `RESTService`.
That means a fake service can expose the same observable API and drive the UI deterministically.

### Fake service shape
The fake should mimic the public surface of `RESTService`:
- `currentCommandList`
- `currentLog`
- `currentQueue`
- `currentCommand`
- `currentQueueLength`
- `continueIfOneFails`
- `getCommands()`
- `executeCommand(command)`
- `startLogging()`
- `stopLogging()`
- `clearCommandList()`
- `setContinueIfOneFails(value)`

### Example approach
In a test you can:
1. provide a fake `RESTService`,
2. create the component under test,
3. push values into the fake service subjects,
4. assert what the template renders.

### Good test cases for Strategy A
- `WebCliCommandsComponent` renders grouped commands from a fake `getKnownCommands` result.
- `WebCliCommandInputComponent` calls `executeCommand()` on Enter.
- `WebCliCommandInputComponent` restores `commandHistory` from `localStorage`.
- `WebCliLogComponent` trims the visible log list to `historySize`.
- `WebCliQueueComponent` reacts to `currentQueue` and displays the queue tab state.
- `WebCliSettingsComponent` loads defaults from `localStorage` and calls `setContinueIfOneFails()`.

## Strategy B: protocol tests by stubbing `window.WebSocket`
This is the best way to test `RESTService` itself.

### When to use it
Use this when you want to verify:
- socket URL construction,
- outgoing JSON messages,
- incoming message routing,
- reconnect/retry behavior,
- handling of the special `noPermission` message.

### Why it matters
`RESTService` is the real protocol adapter. It serializes requests and demultiplexes messages into RxJS subjects.

### What the fake socket must support
A minimal fake `WebSocket` should expose:
- constructor URL capture,
- `readyState`,
- `send(data)`,
- `close()`,
- `onmessage`,
- optionally `onclose`, `onerror`, `onopen`.

Suggested readyState values to emulate:
- `0` connecting
- `1` open
- `2` closing
- `3` closed

### Example fake-socket workflow
1. Create a fake `WebSocket` instance with `readyState = 1`.
2. Instantiate `RESTService`.
3. Call `getCommands()`.
4. Assert that `send()` received:
   ```json
   { "type": "getKnownCommands" }
   ```
5. Trigger `onmessage` with a backend payload.
6. Assert that the correct observable emitted.

### Good test cases for Strategy B
- `getCommands()` sends the correct JSON envelope.
- `executeCommand('foo')` sends `{ type: 'run', command: 'foo' }`.
- `executeCommand('')` sends nothing.
- incoming `log` messages are forwarded to `currentLog`.
- incoming `commandQueue` messages update both queue and queue length.
- incoming `currentCommand` messages update the running-command UI state.
- incoming `continueIfOneFails` updates the observable only when `value` is present.
- `noPermission` triggers the special handling path.
- closed socket causes reconnection attempts when sending a new message.

## Strategy C: lightweight fake backend for browser-level tests
This is useful when you want to exercise the whole GUI in a browser without MyCoRe.

### What it looks like
Run a tiny standalone mock server that:
- serves a WebSocket endpoint compatible with `/ws/mycore-webcli/socket`,
- accepts the same six request types,
- returns scripted JSON responses.

### Good uses
- manual UI exploration,
- browser automation,
- screenshot or demo environments,
- regression tests for full message sequences.

### Suggested scripted behavior
- `getKnownCommands` -> send a static catalog
- `run` -> emit `commandQueue`, `currentCommand`, `log`, then empty queue and current command
- `startLog` -> begin periodic fake logs
- `stopLog` -> stop fake logs
- `clearCommandList` -> emit empty queue and empty current command
- `continueIfOneFails` -> store state and echo it back

## Recommended test split for this module

### 1. Fast component tests
Use fake `RESTService` objects for:
- `commands.component.ts`
- `command-input.component.ts`
- `log.component.ts`
- `queue.component.ts`
- `settings.component.ts`
- `app.component.ts`

### 2. Focused protocol tests
Stub `window.WebSocket` for:
- `rest.service.ts`

### 3. Optional browser integration tests
Run against a tiny fake WebSocket backend if you want confidence in the full UI flow.

## Edge cases worth covering
- empty command should not send a `run` request
- empty queue should hide or disable queue-related UI appropriately
- queue sizes greater than 100 should preserve `size` while rendering only a subset
- log messages with exceptions should render both message and stack trace
- missing permission should handle non-JSON input safely
- socket readyState transitions should not lose messages unnecessarily
- command history navigation should handle top/bottom boundaries
- settings should behave correctly when localStorage is empty or malformed

## Suggested implementation path

### Phase 0: settle the test harness shape
Before implementing tests, pick the harness direction:
- preferred: follow the `mycore-mets` precedent with Grunt + Jasmine and add a Maven `test` hook
- fallback: keep tests out of Maven initially and run them as a local frontend-only task until the suite stabilizes

### Phase 1: no production code changes
- Add tests that provide a fake `RESTService` to components.
- Add tests that stub global `WebSocket` when testing `RESTService`.

This is the lowest-risk path.

### Phase 2: small refactor for easier tests
Introduce a thin socket abstraction, for example:
- `WebCliTransport`
- `BrowserWebCliTransport`
- `FakeWebCliTransport`

Then keep message parsing in a dedicated protocol service.

Benefits:
- easier unit tests,
- clearer separation of concerns,
- cleaner future modernization.

## Suggested test tooling
This module currently does not declare a frontend test stack in `package.json`.

Based on the surrounding repo, the least surprising path is:
- Jasmine for unit/spec style tests
- Grunt task(s) to compile and run those tests
- optional Maven `frontend-maven-plugin` execution to run the frontend tests during the `test` phase

This follows the pattern already used by `mycore-mets`.

Implemented harness in this module:
- `grunt test` builds the GUI and then runs the Node-side Jasmine suite
- `npm test` delegates to `grunt test`
- Maven `test` now invokes the frontend test task through `frontend-maven-plugin`
- the specs use `jsdom` plus fake `RESTService` / fake `WebSocket` helpers so no MyCoRe backend is required

## Minimal fake service contract example
The exact implementation can vary, but the fake should conceptually provide:
- observable streams for the same six frontend subscriptions,
- methods that record which commands/settings the UI attempted to send.

That lets tests assert both:
- what the UI sends,
- how the UI reacts to backend-like events.

## Practical recommendation
If the goal is to start testing soon without touching too much production code:
1. adopt a `mycore-mets`-style Jasmine + Grunt setup rather than introducing a brand-new test runner,
2. fake `RESTService` in component tests,
3. stub `window.WebSocket` in service tests,
4. delay full mock-server or Selenium work until browser-level regression testing is needed.

This matches the actual architecture of `mycore-webcli`, keeps the contract centered on `PROTOCOL.md`, and avoids depending on a live MyCoRe runtime.

## Concrete commands
Install frontend dependencies if needed:
```zsh
cd /Users/thosch/git/mycore/mycore-webcli
npm install
```

Local frontend-only test run:
```zsh
cd /Users/thosch/git/mycore/mycore-webcli
npm test
```

Equivalent Grunt invocation:
```zsh
cd /Users/thosch/git/mycore/mycore-webcli
grunt test
```

Module Maven test run:
```zsh
cd /Users/thosch/git/mycore
mvn -pl mycore-webcli test
```
