# Testing `mycore-webcli`

The current frontend test stack is:

- Vitest
- jsdom
- Vite module resolution
- Playwright
- axe-core

Tests live under:

- [`src/main/vue/webcli/src/`](src/main/vue/webcli/src/) `**/*.spec.ts`
- [`src/main/vue/webcli/tests/a11y/`](src/main/vue/webcli/tests/a11y/) `**/*.spec.ts`

The Vitest suite is split by responsibility:

- app-level command input and execution behavior
- app-level console, queue, and settings behavior
- app-level history integration
- component-level menu and dialog behavior
- service/composable-level protocol and settings logic

Shared Vitest helpers live under:

- [`src/main/vue/webcli/src/test/helpers/`](src/main/vue/webcli/src/test/helpers/)

Shared Playwright helpers live under:

- [`src/main/vue/webcli/tests/a11y/helpers/`](src/main/vue/webcli/tests/a11y/helpers/)

## What to test

The backend contract is still the most important seam.

Priority coverage:

- WebSocket URL construction
- keepalive URL construction
- outgoing protocol messages
- incoming message routing
- `noPermission` handling
- command-history persistence
- settings persistence
- command menu keyboard and flyout behavior
- command suggestion behavior
- queue and log rendering behavior

## What not to mock at integration level

You do not need a full MyCoRe runtime to test most frontend behavior.

For unit-style tests, mock:

- the browser `WebSocket`
- `window.location`
- `window.localStorage`

You usually do not need to mock:

- Bootstrap CSS
- Font Awesome CSS
- the servlet layer

## Maven integration

Maven runs the frontend tests through `frontend-maven-plugin` with:

```bash
yarn test --run
```

from:

```bash
src/main/vue/webcli
```

The `local-testing` Maven profile adds the browser-level accessibility checks. In CI this uses a system Chromium-based browser when available.

Locally, Playwright first checks `CHROME_BIN` and `CHROMIUM_BIN`. If those variables are not set, it tries to detect a system Chrome or Chromium installation automatically.

Requirement:

- Chrome or Chromium must be installed locally for Playwright runs
- `CHROME_BIN` and `CHROMIUM_BIN` are optional

Playwright artifacts are written under:

- [`target/playwright/test-results/`](target/playwright/test-results/)
- [`target/playwright/report/`](target/playwright/report/)

```bash
yarn test:a11y
```

## Manual local commands

Run from `src/main/vue/webcli`:

```bash
yarn install
yarn test --run
yarn build
yarn test:a11y
```

If you want to force a specific browser binary, set:

```bash
export CHROME_BIN=$(which google-chrome)
yarn test:a11y
```

## Canonical protocol reference

Use [`PROTOCOL.md`](PROTOCOL.md) as the source of truth for the message contract.
