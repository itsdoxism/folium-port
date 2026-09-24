# Milestone 0.0.22 — DOM input queue replaces SDL event polling

Folium now has a browser-native input path for Minecraft 26.3.

## Working path

```text
DOM keyboard / pointer / wheel events
        |
        v
BrowserInputHost
        |
        v
Folium InputEvent queue
        |
        v
patched SDLEventHandler.pollEvents()
        |
        v
FoliumInputPump
        |
        +-- KeyboardHandler
        +-- MouseHandler
        |
        v
existing Minecraft input and keybind logic
```

## Implemented event types

- key press;
- key release;
- key repeat;
- printable text;
- mouse/pointer movement;
- mouse buttons;
- wheel;
- pointer enter;
- focus/blur notifications.

## Browser state

The input host also maintains live sets for:

```text
keys currently down
mouse buttons currently down
mouse x/y
accumulated wheel delta
```

`InputConstants.isKeyDown` now uses this state instead of
`SDL_GetKeyboardState`.

## Mouse capture

Minecraft's normal mouse-grab calls now map to browser pointer lock rather than
SDL relative-mouse mode.

## Exact patch targets

The bytecode patcher now transforms:

```text
SDLEventHandler.pollEvents()V
SDLEventHandler.flushInputEvents()V

InputConstants.isKeyDown(I)Z
InputConstants.grabMouse(Window,DD)V
InputConstants.releaseMouse(Window,DD)V
```

These method descriptors match the supplied 26.3 client JAR.

## What this removes

Normal input polling no longer requires:

- `SDL_Event.malloc`;
- `SDL_PollEvent`;
- `SDL_PumpEvents`;
- `SDL_FlushEvents`;
- SDL keyboard-state polling;
- SDL mouse-grab calls.

## Next blocker

Text fields still depend on the SDL-oriented:

```text
TextInputManager
ClipboardManager
```

The next milestone should bridge browser composition/text-input state and
`navigator.clipboard`, removing the remaining SDL calls needed for ordinary
chat and GUI text editing.
