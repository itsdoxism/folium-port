# Browser input seam

Minecraft 26.3 normally receives keyboard, mouse, wheel and text input through
SDL event polling.

Folium replaces that path with a DOM-backed queue.

## Browser event sources

`BrowserInputHost` installs listeners for:

```text
keydown
keyup
pointerenter
pointermove
pointerdown
pointerup
wheel
focus
blur
```

The listeners maintain live key/button state and append typed events to the
Folium input queue.

## Keyboard mapping

Minecraft 26.3's `InputConstants.KEY_*` values are SDL scancodes.

Folium therefore maps browser `KeyboardEvent.code` values onto those same
numeric scancodes.

Examples:

```text
KeyA       -> 4
KeyW       -> 26
Digit1     -> 30
Space      -> 44
ArrowRight -> 79
ArrowLeft  -> 80
ArrowDown  -> 81
ArrowUp    -> 82
ControlLeft -> 224
ShiftLeft   -> 225
```

This lets the existing Minecraft keybinding system continue using its normal
key IDs.

Modifier bits are emitted in Minecraft's existing SDL-compatible layout.

## Minecraft dispatch

The patched:

```text
SDLEventHandler.pollEvents()
```

calls:

```text
FoliumInputPump.poll(minecraft, window)
```

which forwards events directly to existing handlers:

```text
KeyboardHandler.keyPress(...)
KeyboardHandler.textInput(...)
MouseHandler.onMove(...)
MouseHandler.onButton(...)
MouseHandler.onScroll(...)
MouseHandler.cursorEntered()
```

No `SDL_Event` object is allocated.

## Polled key state

`InputConstants.isKeyDown(int)` now reads the browser key-state set maintained
by `BrowserInputHost`.

## Pointer lock

`InputConstants.grabMouse(...)` and `releaseMouse(...)` are redirected to:

```text
canvas.requestPointerLock()
document.exitPointerLock()
```

through `BrowserWindowHost`.

## Current text-input limitation

Printable key text is currently derived from `KeyboardEvent.key`.

That covers normal Latin/Cyrillic typing, but full IME/preedit support still
needs a browser text-input element or composition-event bridge for:

```text
compositionstart
compositionupdate
compositionend
```

Minecraft's `TextInputManager` also still contains direct SDL calls and is a
remaining patch target.

## Remaining SDL input shell

The largest remaining input-related desktop dependencies are:

```text
TextInputManager
ClipboardManager
InputConstants.Type display-name fallback
MonitorManager
```

The next useful step is TextInputManager + ClipboardManager, because chat,
signs and text fields depend on them even after the event queue works.
