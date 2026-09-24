# Milestone 0.0.20 — first SDL boot dependency removed

Folium now patches the first SDL-heavy method out of Minecraft 26.3's reachable
browser boot path.

## Patched method

```text
com.mojang.blaze3d.systems.RenderSystem.initBackendSystem()
```

Desktop Minecraft performs SDL initialization here.

Folium replaces the complete method body with:

```text
FoliumBackendBootstrap.initBackendSystem()
    -> FoliumRuntime.platform().clock().nanoTime()
```

and returns the same Minecraft `TimeSource.NanoTimeSource` abstraction the
caller expects.

## Browser runtime installation

The TeaVM runtime now installs:

```text
BrowserFoliumPlatform
```

at startup.

Available today:

```text
graphics -> BrowserGraphicsHost
clock    -> BrowserClockHost
```

The browser clock uses:

```text
performance.now()  -> monotonic nanosecond-scale source
Date.now()         -> wall-clock milliseconds
```

## Also fixed

The old WebGPU smoke probe still looked for the historical
`globalThis.__foliumWebGpu` object.

The current preboot runtime uses:

```text
globalThis.__foliumHostState
```

The probe has been updated to the active host state, so the runtime/bootstrap
code now agrees on one WebGPU state object.

## What remains

This does **not** mean Folium is SDL-free yet.

The remaining desktop shell includes direct SDL references in:

- Window;
- Monitor / MonitorManager;
- SDLEventHandler;
- InputConstants;
- MouseHandler;
- ClipboardManager;
- TextInputManager.

The next milestone should replace `Window` construction/state with a
browser-backed shell over the existing HTML canvas.

That will remove the next major block preventing Minecraft's normal renderer
startup from reaching the Folium WebGPU backend.
