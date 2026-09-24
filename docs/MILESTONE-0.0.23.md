# Milestone 0.0.23 — browser text input and clipboard

Folium now removes the SDL dependency from Minecraft 26.3 text input and clipboard operations.

## Patched classes

```text
TextInputManager
ClipboardManager
```

## Text input path

```text
Minecraft GUI focus
    |
    v
TextInputManager.startTextInput(...)
    |
    v
BrowserTextInputBridge
    |
    v
hidden textarea
    |
    +-- compositionend
    +-- input
    |
    v
Folium TEXT event
    |
    v
KeyboardHandler.textInput(...)
```

The patch keeps Minecraft's owner/focus state while replacing SDL text-input startup and shutdown.

## Clipboard path

```text
ClipboardManager.getClipboard()
    -> synchronous Folium cache

ClipboardManager.setClipboard(text)
    -> immediate cache update
    -> best-effort navigator.clipboard.writeText(text)
```

The cache is refreshed through paste events and asynchronous browser clipboard reads when permitted.

## Exact patch targets

The patcher now verifies:

```text
TextInputManager.<init>(Window)V
TextInputManager.setTextInputArea(IIII)V
TextInputManager.applyTextInputArea()V
TextInputManager.startTextInput(Object)V
TextInputManager.stopTextInput(Object)V
TextInputManager.stopTextInput()V
TextInputManager.onTextInputFocusChange(Object,Z)V

ClipboardManager.getClipboard()String
ClipboardManager.setClipboard(String)V
```

These descriptors match the supplied Minecraft 26.3 client class files.

## SDL input status

The normal browser interaction path now avoids SDL for event polling, keyboard state, mouse motion/buttons, wheel input, pointer lock, text input, and clipboard.

## Next desktop-shell blocker

The next significant SDL-dependent area is display/monitor management:

```text
Monitor
MonitorManager
VideoMode
```

Minecraft still constructs monitor state during startup and fullscreen/display queries can reach SDL even though the patched Window no longer creates a native window.

The next milestone should replace monitor enumeration with a synthetic browser display based on the canvas/screen dimensions.
