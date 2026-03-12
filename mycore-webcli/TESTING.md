# Testing `mycore-webcli`

The current frontend test stack is:

- Vitest
- jsdom
- Vite module resolution
- Playwright
- axe-core

Tests live under:

- `src/main/vue/webcli/src/**/*.spec.ts`
- `src/main/vue/webcli/tests/a11y/**/*.spec.ts`

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

- Chrome or Chromium must be installed locally for browser-less runs
- `CHROME_BIN` and `CHROMIUM_BIN` are optional

Playwright artifacts are written under:

- `target/playwright/test-results`
- `target/playwright/report`

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

Use `PROTOCOL.md` as the source of truth for the message contract.
