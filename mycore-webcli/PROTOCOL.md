# `mycore-webcli` protocol reference

## Checklist
- Define the canonical frontend/backend contract for the WebCLI GUI
- Describe transport, paths, message envelopes, and state transitions
- Provide concrete request/response examples
- Highlight protocol details that matter for testing and mocking

## Scope
This document is the canonical reference for the browser/backend contract used by `mycore-webcli`.

It complements:
- `README.md` for module architecture and build context
- `TESTING.md` for unit and integration-style mocking strategies
- `TODO.md` for cleanup and hardening ideas around the protocol implementation

The protocol described here is implemented primarily by:
- frontend: `src/main/vue/webcli/src/services/webcliTransport.ts`
- backend endpoint: `src/main/java/org/mycore/webcli/resources/MCRWebCLIResourceSockets.java`
- backend message producers: `src/main/java/org/mycore/webcli/container/MCRWebCLIContainer.java`, `src/main/java/org/mycore/webcli/flow/MCRCommandListProcessor.java`, and `src/main/java/org/mycore/webcli/flow/MCRLogEventProcessor.java`

## Transport model

### Static HTTP resources
The browser first loads the GUI shell over HTTP from:
- `.../modules/webcli/gui/`

Static assets are then served from:
- `.../modules/webcli/gui/{filename}`

Examples:
- `.../modules/webcli/gui/index.html`
- `.../modules/webcli/gui/assets/index-*.js`
- `.../modules/webcli/gui/assets/index-*.css`

### Keepalive HTTP request
While the page is open, `index.html` performs a periodic keepalive request:
- `../../echo/ping`

This is not part of the command protocol itself, but it does matter for session longevity.

### Interactive transport: WebSocket
The command/control interface is WebSocket-based.

Server endpoint:
- `/ws/mycore-webcli/socket`

Frontend URL construction in `WebCliTransport`:
- use `ws://` when the page is served over HTTP
- use `wss://` when the page is served over HTTPS
- prepend the current host
- derive the base path from `window.location.pathname`
- append `/ws/mycore-webcli/socket`

## Connection lifecycle

### Opening
`WebCliTransport` opens the socket during frontend initialization.

### Sending
All interactive actions send JSON text frames.

### Receiving
Most inbound frames are JSON envelopes with a `type` property.

### Special non-JSON frame
If permission is denied, the backend sends the plain text frame:

```text
noPermission
```

The frontend treats this as a special case before attempting JSON parsing.

### Closing behavior
The backend may close the WebSocket with normal closure after the current processing run is finished.

Implication:
- the frontend is not permanently connected during idle periods
- reconnection is lazy and happens when the next outbound message is sent

## Permission model
The required permission is:
- `use-webcli`

Relevant checks:
- HTTP access to `WebCLI` is guarded by `MCRWebCLIPermission`
- WebSocket messages are checked in `MCRWebCLIResourceSockets.message(...)`

If the permission check fails after the socket is open, the backend sends:

```text
noPermission
```

## Message envelope conventions
Most messages use a top-level JSON object with:
- `type`: message kind
- `return`: payload for many backend-to-frontend responses
- other fields as needed, for example `command`, `value`, or `size`

This is a small message protocol rather than a REST API.

## Frontend -> backend messages

### 1. Request known commands
Used by:
- frontend startup via `WebCliTransport.getKnownCommands()`

Request:
```json
{ "type": "getKnownCommands" }
```

Expected effect:
- backend returns a command catalog grouped by command family

### 2. Run a command
Used by:
- `WebCliCommandInputComponent.execute()`

Request:
```json
{ "type": "run", "command": "process resource my-batch.txt" }
```

Expected effect:
- command is added to the session queue
- worker thread starts if needed
- queue/current-command/log events follow asynchronously

Validation note:
- frontend suppresses empty or undefined commands

### 3. Start log streaming
Used by:
- the Vue app when refresh is enabled

Request:
```json
{ "type": "startLog" }
```

Expected effect:
- backend attaches or reattaches a log subscriber for the session

### 4. Stop log streaming
Used by:
- the Vue app when refresh is disabled

Request:
```json
{ "type": "stopLog" }
```

Expected effect:
- backend stops forwarding log events for the current session

### 5. Clear queued commands
Used by:
- the Vue app when the queue-clear action is triggered

Request:
```json
{ "type": "clearCommandList" }
```

Expected effect:
- queue is cleared
- queue update and current-command reset are pushed back to the client

### 6. Set failure handling mode
Used by:
- the Vue settings panel

Request:
```json
{ "type": "continueIfOneFails", "value": true }
```

Expected effect:
- backend updates session behavior for subsequent command failures

## Backend -> frontend messages

### 1. Known command catalog
Response type:
- `getKnownCommands`

Shape:
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
          },
          {
            "command": "skip on error",
            "help": "Skip execution of failed command in case of error"
          }
        ]
      }
    ]
  }
}
```

Semantics:
- `name` is the command group shown in the dropdown
- each nested command entry provides the command syntax and tooltip/help text

### 2. Current running command
Response type:
- `currentCommand`

Shape:
```json
{
  "type": "currentCommand",
  "return": "process resource my-batch.txt"
}
```

Idle state:
```json
{
  "type": "currentCommand",
  "return": ""
}
```

Semantics:
- non-empty string means a command is currently executing
- empty string means no command is currently active

### 3. Queue update
Response type:
- `commandQueue`

Shape:
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

Important detail:
- `return` contains at most the first 100 commands
- `size` contains the total queue length

Example with truncation:
```json
{
  "type": "commandQueue",
  "return": ["first 100 commands only"],
  "size": 250
}
```

Semantics:
- use `size` for total queue count
- use `return` only for the visible preview list

### 4. Log event
Response type:
- `log`

Shape:
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

Error example:
```json
{
  "type": "log",
  "return": {
    "logLevel": "ERROR",
    "message": "Command 'import object foo' failed. Performing transaction rollback...",
    "exception": "java.lang.RuntimeException: ... stack trace ...",
    "time": 1773054321999
  }
}
```

Semantics:
- only log events from the WebCLI worker thread are forwarded
- the frontend currently renders `logLevel`, `message`, and `exception`
- the frontend currently ignores `time`

### 5. Failure handling mode sync
Response type:
- `continueIfOneFails`

Shape:
```json
{
  "type": "continueIfOneFails",
  "value": false
}
```

Semantics:
- this may be sent after the client toggles the setting
- it may also be sent when backend commands like `skip on error` or `cancel on error` change the mode server-side

## Typical command execution sequence
A realistic message flow for a single command can look like this.

### Step 1: frontend sends the command
```json
{ "type": "run", "command": "process resource batch.txt" }
```

### Step 2: backend publishes queue state
```json
{
  "type": "commandQueue",
  "return": ["process resource batch.txt"],
  "size": 1
}
```

### Step 3: backend marks it as current
```json
{
  "type": "currentCommand",
  "return": "process resource batch.txt"
}
```

### Step 4: backend streams logs
```json
{
  "type": "log",
  "return": {
    "logLevel": "INFO",
    "message": "Processing command:'process resource batch.txt' (0 left)",
    "exception": null,
    "time": 1773054321000
  }
}
```

### Step 5: backend may enqueue follow-up commands
```json
{
  "type": "commandQueue",
  "return": [
    "select values 123 456",
    "execute for selected set parent of {x} to myapp_container_00000001"
  ],
  "size": 2
}
```

### Step 6: backend returns to idle
```json
{
  "type": "currentCommand",
  "return": ""
}
```

```json
{
  "type": "commandQueue",
  "return": [],
  "size": 0
}
```

## Session and state model
The backend stores a session-scoped `MCRWebCLIContainer` under the key:
- `MCRWebCLI`

It owns:
- the queued commands
- the current command
- the current WebSocket session reference
- the log subscription
- the `continueIfOneFails` flag

Implications:
- the protocol is sessionful
- reconnects may attach a new socket to the same session container
- the browser should not assume all state is recreated from scratch on reconnect

## Error and edge conditions

### Empty command input
The frontend suppresses empty commands and sends nothing.

### Permission failure
Backend sends:
```text
noPermission
```
Frontend behavior:
- console message
- alert to the user
- stop retry attempts

### Socket not currently open
`WebCliTransport` retries a small number of times and may reopen the socket if needed.

### Backend closes socket after work completes
This is normal for the current implementation.
Testing and mocks should allow normal closure after a command run.

### Partial queue visibility
Mocks should preserve the distinction between:
- `return` as a visible queue preview
- `size` as the real queue length

## Mocking guidance derived from the protocol
If you are mocking the backend, your fake should ideally support:
- plain-text `noPermission`
- the six client request types
- the five JSON response/event types
- queue preview truncation plus independent total `size`
- idle transitions via `currentCommand: ""`
- optional normal socket closure after a scripted run

## Canonical test vectors
These are good protocol-level fixtures for unit and integration-style tests:
- command catalog with one group and two commands
- queue update with `size = 0`
- queue update with `size > 100`
- log event without exception
- log event with exception
- `continueIfOneFails` true/false
- `noPermission`
- reconnect after a closed socket

## Source of truth reminder
If implementation and documentation ever diverge, re-check these first:
- `src/main/vue/webcli/src/services/webcliTransport.ts`
- `src/main/java/org/mycore/webcli/resources/MCRWebCLIResourceSockets.java`
- `src/main/java/org/mycore/webcli/container/MCRWebCLIContainer.java`
