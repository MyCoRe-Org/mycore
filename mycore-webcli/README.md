# mycore-webcli

`mycore-webcli` provides a browser-based command line interface for MyCoRe.

The current frontend is a Vue 3 + Vite application. It lets an authorized user:

- browse known CLI commands
- compose and execute commands
- watch the current command and queued commands
- stream command-related logs
- change a small set of execution settings

## Runtime URLs

- launch pad: `/modules/webcli/launchpad.xml`
- protected GUI: `/modules/webcli/gui/`
- WebSocket endpoint: `/ws/mycore-webcli/socket`

The launch pad stays public as a normal webpage resource. The GUI itself is served through `MCRVueRootServlet` with the optional init-param `permission=use-webcli`, so both the wrapped `index.html` and the pass-through built assets stay protected.

## Module layout

### Frontend

- [`src/main/vue/webcli/`](src/main/vue/webcli/) - Vue source, Vite config, and Vitest tests
- [`target/classes/META-INF/resources/modules/webcli/gui/`](target/classes/META-INF/resources/modules/webcli/gui/) - Vite build output during Maven builds
- [`src/main/resources/META-INF/resources/modules/webcli/launchpad.xml`](src/main/resources/META-INF/resources/modules/webcli/launchpad.xml) - launch page that opens the GUI

### Backend

- [`src/main/resources/META-INF/web-fragment.xml`](src/main/resources/META-INF/web-fragment.xml) - `MCRVueRootServlet` mapping for the protected GUI
- [`src/main/java/org/mycore/webcli/resources/MCRWebCLIResourceSockets.java`](src/main/java/org/mycore/webcli/resources/MCRWebCLIResourceSockets.java) - WebSocket endpoint
- [`src/main/java/org/mycore/webcli/resources/MCRWebCLIPermission.java`](src/main/java/org/mycore/webcli/resources/MCRWebCLIPermission.java) - permission check helper
- [`src/main/java/org/mycore/webcli/container/`](src/main/java/org/mycore/webcli/container/) - session-scoped command execution and message publishing
- [`src/main/java/org/mycore/webcli/flow/`](src/main/java/org/mycore/webcli/flow/) - JSON/log/queue event processors
- [`src/main/java/org/mycore/webcli/cli/`](src/main/java/org/mycore/webcli/cli/) - command discovery and built-in commands

## Build

The module uses `frontend-maven-plugin` from Maven.

Current frontend build flow:

1. install Node and Yarn
2. run `yarn install` in `src/main/vue/webcli`
3. run `yarn build` in `src/main/vue/webcli`
4. run `yarn test --run` in `src/main/vue/webcli` during Maven `test`
5. in Maven profile `local-testing`, run `yarn test:a11y` during integration testing against a Chromium-based browser; Playwright uses `CHROME_BIN` or `CHROMIUM_BIN` when set and otherwise tries to detect a local installation

Important frontend config:

- Vite `base` is `"./"`
- Vite output goes directly to [`target/classes/META-INF/resources/modules/webcli/gui/`](target/classes/META-INF/resources/modules/webcli/gui/)

## Frontend/backend contract

The GUI is WebSocket-driven.

Canonical protocol reference:

- [`PROTOCOL.md`](PROTOCOL.md)

The Vue transport preserves the established backend message types:

- `getKnownCommands`
- `run`
- `startLog`
- `stopLog`
- `clearCommandList`
- `continueIfOneFails`

The backend may still send the special plain-text frame:

- `noPermission`

## Testing

Frontend tests now live with the Vue app and run with Vitest. Browser-level accessibility checks run with Playwright + axe-core in the `local-testing` profile.

See:

- [`TESTING.md`](TESTING.md)
