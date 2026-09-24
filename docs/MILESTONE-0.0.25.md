# Milestone 0.0.25 — residual reachable SDL cleanup

Folium now patches the remaining SDL calls that were still reachable from ordinary browser startup and interaction after the main window/input/display work.

## Audit result

The official 26.3 client JAR still contains 27 classes with direct `org.lwjgl.sdl` references, but many are already transformed or belong exclusively to the unused desktop OpenGL/Vulkan backends.

The browser-relevant residuals identified were:

```text
NativeLibrariesBootstrap.loadLibraries()
Blaze3D.getTime()
CursorType.<clinit>()
CursorType.createStandardCursor(...)
CursorType.select()
InputQuirks.isQuitShortcutDown()
MouseHandler.resyncMousePosition()
```

## Native library bootstrap

`NativeLibrariesBootstrap.loadLibraries()` is now a browser no-op.

`isVulkanLoaderAvailable()` returns false.

This prevents `Main` from attempting to load SDL/OpenGL/OpenAL/STB/shaderc/Vulkan native libraries before the Folium runtime starts.

## Timing

`Blaze3D.getTime()` now returns:

```text
FoliumRuntime.platform().clock().nanoTime() / 1_000_000_000.0
```

Mouse/UI timing therefore uses the same browser monotonic clock as the patched renderer bootstrap.

## Cursor initialization

`CursorType.DEFAULT` is created as an opaque Java cursor object with handle `1L` instead of calling `SDL_GetDefaultCursor`.

`createStandardCursor(...)` returns its fallback cursor and `select()` becomes a no-op. Browser cursor selection is already intentionally handled outside SDL.

## Input quirks

`InputQuirks.isQuitShortcutDown()` returns false in the browser build instead of polling `SDL_GetModState`.

Browser/page quit behavior is owned by the browser, not Minecraft's native desktop shortcut handling.

## Mouse resync

`MouseHandler.resyncMousePosition()` now copies the live pointer coordinates maintained by `BrowserInputHost` instead of calling:

```text
SDL_GetGlobalMouseState
SDL_GetWindowPosition
SDL_GetMouseState
```

## Verification

Against the supplied Minecraft 26.3 client JAR, every new patch target matched exactly once.

## Remaining direct SDL references

The notable unpatched utility paths are now lower priority:

```text
Blaze3D.openUri(...)
MessageBox
MacosUtil
```

`SdlDebug` is no longer reached by the patched `RenderSystem.initBackendSystem()` path.

The desktop OpenGL/Vulkan backend classes may still physically reference SDL in the transformed local JAR, but `PreferredGraphicsApi` never selects those backends in the Folium browser build.

## Next milestone

The next cleanup should replace desktop utility behavior with browser equivalents:

```text
open URI -> window.open / location
message box -> browser overlay/dialog
macOS SDL hints -> no-op
```

After that, the next major blockers are no longer SDL shell issues; they are WebGPU feature completeness and network/socket replacement.
