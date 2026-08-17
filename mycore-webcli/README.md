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

The GUI is served through `MCRVueRootServlet` with the init-param `permission=use-webcli`, so both the wrapped `index.html` and the pass-through built assets stay protected.

The launch pad is a normal webpage resource, but in the default integration it is not public either: the module setup grants read access to localhost administrators and loads the `use-webcli` permission rule from the component config.

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

The app is built with the shared toolchain in [`mycore-vue`](../mycore-vue/README.md), so every step below runs
`yarn` with `mycore-vue` as working directory. `yarn install` runs there once for all Vue apps, not here.

1. install Node and Yarn through the parent `frontend-maven-plugin` configuration
2. run `yarn build:webcli`
3. during Maven `test`, run `yarn test:webcli`, `yarn lint:webcli`, and `yarn typecheck:webcli`
4. in Maven profile `checks`, run `yarn ci-check:webcli` during `test`
5. in Maven profile `local-testing`, run `yarn test-a11y:webcli` during integration testing against a Chromium-based browser; Playwright uses `CHROME_BIN` or `CHROMIUM_BIN` when set and otherwise tries to detect a local installation

Important frontend config:

- Vite `base` is `"./"`
- Vite output goes directly to [`target/classes/META-INF/resources/modules/webcli/gui/`](target/classes/META-INF/resources/modules/webcli/gui/)
- Vitest coverage reports go to `target/vitest-coverage` and enforce coverage thresholds in `vite.config.mts`

## Frontend/backend contract

The GUI is WebSocket-driven.

Current protocol implementation lives in:

- [`src/main/java/org/mycore/webcli/resources/MCRWebCLIResourceSockets.java`](src/main/java/org/mycore/webcli/resources/MCRWebCLIResourceSockets.java)
- [`src/main/vue/webcli/src/services/webcliTransport.ts`](src/main/vue/webcli/src/services/webcliTransport.ts)

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

Relevant files:

- [`src/main/vue/webcli/vite.config.mts`](src/main/vue/webcli/vite.config.mts) (vitest configuration and coverage thresholds)
- [`src/main/vue/webcli/tests/a11y/`](src/main/vue/webcli/tests/a11y/)
- [`../mycore-vue/package.json`](../mycore-vue/package.json) (dependencies and the `*:webcli` scripts)
- [`../mycore-vue/playwright.webcli.config.mts`](../mycore-vue/playwright.webcli.config.mts)
- [`../mycore-vue/playwright.webcli.performance.config.mts`](../mycore-vue/playwright.webcli.performance.config.mts)
- [`src/main/vue/webcli/tests/performance/`](src/main/vue/webcli/tests/performance/)
- [`../mycore-vue/testing/webcli-stub-server.mjs`](../mycore-vue/testing/webcli-stub-server.mjs)

The MCR-3794 performance probe uses a local HTTP/WebSocket stub that implements the
Web CLI protocol and can stream either command-queue snapshots or log messages:

```bash
cd ../mycore-vue
yarn test-performance:webcli
```

The probe reports DOM churn, elapsed time, peak JavaScript heap growth, and retained
JavaScript heap growth after garbage collection for several workload sizes, including
10,000 objects. Its Chromium measurements are intended for repeatable regression
comparisons; the DOM-churn result is independent of the browser's garbage collector.
