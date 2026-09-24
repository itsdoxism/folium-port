# Browser display / monitor seam

Minecraft 26.3 normally discovers displays and video modes through SDL.

Folium replaces that model with one synthetic browser display.

## Synthetic display

The browser build exposes:

```text
id:           1
name:         Folium Browser Display
position:     0, 0
color bits:   8 / 8 / 8
refresh rate: 60 Hz
size:         current browser canvas framebuffer
```

The current mode and sole supported mode are the same.

This is intentionally conservative. Browser APIs do not expose a desktop-style
enumeration of native monitor modes comparable to SDL.

## Patched behavior

`MonitorManager`:

- initializes with one synthetic monitor;
- ignores display connect/disconnect/mode-change callbacks;
- returns the synthetic monitor for lookups;
- returns it from `findBestMonitor(Window)`.

`Monitor`:

- native monitor creation is redirected to the synthetic display;
- native monitor-name lookup returns a browser display label.

`VideoMode`:

- normal Java constructors remain;
- the SDL-specific `VideoMode(SDL_DisplayMode)` constructor is removed from
  the patched class because browser code no longer has a caller for it.

## Fullscreen

Browser fullscreen remains a separate browser-shell concern. The synthetic
monitor exists to satisfy Minecraft's display model and renderer startup, not
to emulate desktop display-mode switching.
