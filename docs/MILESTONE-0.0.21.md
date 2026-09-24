# Milestone 0.0.21 — Minecraft Window backed by the browser canvas

Folium now transforms Minecraft 26.3's main `Window` construction path into a
browser-canvas shell.

## Working path

```text
new Minecraft Window(...)
        |
        v
patched Window constructor
        |
        v
BrowserWindowHost
        |
        v
#folium-canvas
```

No SDL window is created by the patched constructor.

## Browser host

`BrowserWindowHost` currently provides:

- framebuffer width;
- framebuffer height;
- title updates;
- pointer-lock query;
- pointer-lock request/release.

Framebuffer dimensions account for `devicePixelRatio`.

## Startup SDL traps removed

In addition to constructor replacement, Folium bypasses SDL-dependent startup
operations for:

- window icon setup;
- fullscreen updates;
- fullscreen mode changes;
- cursor selection;
- native max-size configuration;
- native window destruction.

## Patch verification

Every Window method transformed in this milestone was checked against the
supplied 26.3 client JAR.

Each exact method name + JVM descriptor matched exactly once.

## Current browser boot graph

```text
FoliumMain
  -> install BrowserFoliumPlatform
  -> RenderSystem uses BrowserClockHost
  -> PreferredGraphicsApi selects FoliumWebGpuBackend
  -> Minecraft Window uses BrowserWindowHost
  -> WebGPU canvas
```

This removes the major SDL window-construction block from normal renderer
startup.

## Next blocker

Minecraft still polls SDL events through `SDLEventHandler` and translates SDL
key/mouse state through `InputConstants` and `MouseHandler`.

The next milestone is a DOM event queue that exposes:

```text
keydown / keyup
pointer move
mouse buttons
wheel
focus
text input
pointer lock
```

to Minecraft without constructing or polling `SDL_Event`.
