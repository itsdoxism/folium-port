# RenderSystem SDL boot seam

Minecraft 26.3's:

```text
RenderSystem.initBackendSystem()
```

performs desktop SDL initialization and returns a
`TimeSource.NanoTimeSource`.

The original path references:

- SDL app metadata;
- SDL hints;
- `SDL_Init`;
- SDL error reporting;
- `SDL_GetTicksNS`.

Folium replaces this entire method for browser builds.

## Browser replacement

```text
RenderSystem.initBackendSystem()
        |
        v
FoliumBackendBootstrap.initBackendSystem()
        |
        v
FoliumRuntime.platform().clock().nanoTime()
```

This removes SDL initialization from the reachable browser boot path while
preserving Minecraft's expected nano-time abstraction.

The platform must already be installed before this method runs. Failing early
is preferable to silently falling back to a desktop clock and hiding an invalid
browser boot sequence.

## Remaining SDL shell

This patch only removes the first SDL dependency.

Direct SDL references still exist in window/input classes including:

- Window
- Monitor / MonitorManager
- SDLEventHandler
- InputConstants
- MouseHandler
- ClipboardManager
- TextInputManager

Those are the next browser-shell patch targets.
