# TODO for `mycore-webcli`

## Current follow-up items

### 1. Expand Vue test coverage

Current tests cover transport path construction and protocol routing.

Still useful:

- command input history navigation
- placeholder tab-jump behavior
- settings panel interactions
- rendered log trimming
- queue truncation rendering

### 2. Decide whether `noPermission` should stay plain text

The backend still sends the special non-JSON frame:

- `noPermission`

This is preserved for compatibility, but a JSON error envelope would be cleaner.

### 3. Consider extracting the transport/store split further

The Vue app currently uses a transport class plus component-local state.

If the UI grows, move more state into a dedicated composable or store module.

### 4. Review popup-launch assumptions

The launch pad still opens the GUI in a popup window.

That behavior is intentional for parity with the old UI, but it should be confirmed as the preferred UX.
