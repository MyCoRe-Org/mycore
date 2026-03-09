# mycore-webcli

## Checklist
- Summarize what the component does
- Explain how the GUI is built and packaged
- Describe the runtime architecture and frontend/backend interface
- Point to the most important source files
- Link to the canonical protocol and testing references

## What this module is
`mycore-webcli` provides a browser-based command line interface for MyCoRe.

At runtime it serves a small Angular 2.4 Application that lets an authorized user:
- browse known CLI commands,
- compose and execute commands,
- watch the current command and queued commands,
- stream command-related logs,
- toggle a small set of execution settings.

The frontend is a classic Angular 2.4 + SystemJS application. It is not bundled with webpack or Vite. Instead, TypeScript is compiled into browser-loadable JavaScript modules and the browser loads them through SystemJS.

Current UI baseline:
- Angular 2.4.10 + SystemJS
- Bootstrap 5.3.3
- Font Awesome 4.7
- custom CSS in `src/main/resources/META-INF/resources/modules/webcli/css/webcli.css` using Bootstrap variables

## Module layout

### Frontend sources
- `src/main/ts/` – TypeScript components and services
- `src/main/resources/META-INF/resources/modules/webcli/` – HTML entry page, Angular templates, CSS, SystemJS config, launchpad files

### Backend sources
- `src/main/java/org/mycore/webcli/resources/` – HTTP resource and WebSocket endpoint
- `src/main/java/org/mycore/webcli/container/` – session-scoped command execution and message publishing
- `src/main/java/org/mycore/webcli/flow/` – JSON/log/queue event processors
- `src/main/java/org/mycore/webcli/cli/` – command discovery and built-in commands

### Build files
- `pom.xml` – module Maven descriptor
- `GruntFile.js` – TypeScript compilation and asset copying
- `tsconfig.json` – TypeScript compiler output settings
- `package.json` – Angular/SystemJS/npm dependencies

## How the GUI is built

### Build chain
The module relies on Maven plus `frontend-maven-plugin`.

The shared parent build config binds these frontend steps to `generate-resources`:
1. install Node and Yarn,
2. run `yarn install`,
3. run `grunt`.

### Grunt tasks
`GruntFile.js` defines two active tasks:
- `ts` – compiles `src/main/ts` with TypeScript 2.2
- `copy` – copies browser dependencies from `node_modules` into the final web resource tree

Default task:
- `grunt` => `ts` + `copy`

### TypeScript output
`tsconfig.json` compiles the TypeScript sources from:
- `src/main/ts`

to:
- `target/classes/META-INF/resources/modules/webcli/build`

That output path matters because the browser later loads modules from `build/` using SystemJS.

### Static resources
The following files are served directly from `src/main/resources/META-INF/resources/modules/webcli/`:
- `index.html`
- `systemjs.config.js`
- `css/*`
- `app/**/*.html`
- `launchpad.xml`

### Runtime bootstrap
`index.html` loads:
- `core-js` shim,
- `zone.js`,
- `reflect-metadata`,
- `system.js`,
- Bootstrap 5 bundle,
- `systemjs.config.js`.

Then it calls:
- `System.import('app')`

`systemjs.config.js` maps:
- `app` -> `build`
- `@angular/*` -> copied `node_modules/@angular/*`
- `rxjs` -> copied `node_modules/rxjs`

`src/main/ts/main.ts` bootstraps `AppComponent`.

## Frontend architecture

### Root component
`src/main/ts/app.component.ts`

This is the shell component. It wires together the visible UI and coordinates state coming from the transport layer.

### Main child components
- `commands/commands.component.ts` – loads and renders known commands
- `command-input/command-input.component.ts` – command entry, execution, and local command history
- `log/log.component.ts` – rendered log stream
- `queue/queue.component.ts` – rendered command queue
- `settings/settings.component.ts` – local UI settings plus `continueIfOneFails` sync

### Services
- `service/rest.service.ts` – actual backend transport and message routing
- `service/communication.service.ts` – in-browser event bus between components

Note: despite its name, `RESTService` is primarily a WebSocket client, not a REST client.

## Runtime communication model

### HTTP usage
HTTP is used for:
- loading the entry page and all static assets,
- a keepalive ping from `index.html` to `../../echo/ping`.

The main HTTP resource is `org.mycore.webcli.resources.MCRWebCLIResource`.

Relevant paths:
- `GET .../rsc/WebCLI/` -> returns `index.html`
- `GET .../rsc/WebCLI/gui/{filename}` -> serves JS, CSS, templates, and other web assets

### WebSocket usage
The interactive command API is WebSocket-based.

Endpoint:
- `/ws/mycore-webcli/socket`

The frontend computes the socket URL from `window.location` and connects with:
- `ws://` when the page is served over HTTP
- `wss://` when the page is served over HTTPS

Backend endpoint class:
- `org.mycore.webcli.resources.MCRWebCLIResourceSockets`

## Frontend/backend interface

Canonical protocol reference:
- `PROTOCOL.md`

The summary below is intentionally brief. For message-by-message details, state transitions, edge cases, and mocking expectations, use `PROTOCOL.md` as the source of truth.

### Messages sent from the frontend
The UI sends JSON messages with a `type` field.

#### Request known commands
```json
{ "type": "getKnownCommands" }
```

#### Run a command
```json
{ "type": "run", "command": "process resource my-batch.txt" }
```

#### Start log streaming
```json
{ "type": "startLog" }
```

#### Stop log streaming
```json
{ "type": "stopLog" }
```

#### Clear the queued commands
```json
{ "type": "clearCommandList" }
```

#### Set failure handling mode
```json
{ "type": "continueIfOneFails", "value": true }
```

### Messages sent from the backend
The backend emits JSON envelopes with a `type` field.

#### Known command catalog
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

#### Current command update
```json
{
  "type": "currentCommand",
  "return": "process resource my-batch.txt"
}
```

#### Queue update
```json
{
  "type": "commandQueue",
  "return": [
    "command 1",
    "command 2"
  ],
  "size": 2
}
```

#### Log event
```json
{
  "type": "log",
  "return": {
    "logLevel": "INFO",
    "message": "Processing command:'process resource my-batch.txt' (0 left)",
    "exception": null,
    "time": 1773054321000
  }
}
```

#### Failure mode sync
```json
{
  "type": "continueIfOneFails",
  "value": false
}
```

### Special non-JSON message
If the user lacks permission, the backend may send plain text:

```text
noPermission
```

The frontend treats this as a special case and shows an alert.

## Important backend behavior
- The WebSocket session is tied to a session-scoped `MCRWebCLIContainer`.
- Commands are queued and processed sequentially on a single worker thread.
- Queue messages contain only the first 100 visible commands, plus the total queue size.
- Command-related log events are captured from the worker thread and pushed to the browser.
- The backend may close the WebSocket after command processing is finished; the frontend reconnects lazily on the next outbound action.

## Permissions
Two layers protect the component:
- HTTP access to `WebCLI` is guarded by `MCRWebCLIPermission`
- WebSocket message handling also checks `use-webcli`

Required permission key:
- `use-webcli`

## Local browser state
These values are stored in `localStorage` by the frontend:
- `commandHistory`
- `historySize`
- `comHistorySize`
- `autoScroll`
- `continueIfOneFails`

Only `continueIfOneFails` is also sent to the backend.

## Verified build result
A local Grunt build in this module succeeded and produced:
- compiled JS under `target/classes/META-INF/resources/modules/webcli/build`
- copied browser dependencies under `target/classes/META-INF/resources/modules/webcli/node_modules`

## Testing tooling precedent in this repository
Useful nearby examples found during analysis:
- `mycore-mets` contains the clearest reusable frontend unit-test precedent in this repo: `@types/jasmine`, `grunt-contrib-jasmine`, `angular-mocks`, test TypeScript sources under `src/test/ts`, and a Maven `frontend-maven-plugin` execution that runs `grunt jasmine` during the `test` phase.
- `mycore-viewer` contains Selenium dependencies and integration-test wiring, which is useful as a browser-level precedent, but it is heavier than what `mycore-webcli` needs for first-step GUI unit tests.
- No repo-wide Jest, Karma, Vitest, Playwright, Cypress, or Puppeteer standard was found in sibling frontend modules.

## Related docs
- `PROTOCOL.md` – canonical WebSocket/message contract and protocol examples
- `TESTING.md` – frontend testing strategy without a running MyCoRe backend
- `TODO.md` – observations and improvement ideas found during analysis

