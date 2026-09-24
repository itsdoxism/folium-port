# Minecraft 26.3 SDL reachability audit

The official client JAR contains 27 classes with direct `org.lwjgl.sdl`
references.

That raw count overstates the browser problem because Folium already patches
several classes and never selects the desktop OpenGL/Vulkan backends.

## Already redirected

```text
ClipboardManager
InputConstants
Monitor
MonitorManager
SDLEventHandler
TextInputManager
VideoMode
Window
RenderSystem
```

## Desktop backend only

These remain in the local transformed JAR but are not selected by the Folium
browser backend:

```text
GlBackend
GlDevice
GlSurface
VulkanBackend
VulkanGpuSurface
VulkanInstance
VulkanPhysicalDevice
```

## Reachable residuals found by the audit

The next browser-relevant SDL calls are:

```text
NativeLibrariesBootstrap.loadLibraries()
    <- net.minecraft.client.main.Main

Blaze3D.getTime()
    <- MouseHandler and UI timing

CursorType.<clinit>()
    -> SDL_GetDefaultCursor

CursorType.createStandardCursor(...)
    -> SDL_CreateSystemCursor

InputQuirks.isQuitShortcutDown()
    -> SDL_GetModState

MouseHandler.resyncMousePosition()
    -> SDL_GetGlobalMouseState
    -> SDL_GetWindowPosition
    -> SDL_GetMouseState
```

These are patched in milestone 0.0.25.

## Lower-priority residuals

```text
Blaze3D.openUri
MacosUtil
MessageBox
SdlDebug
```

`SdlDebug` was previously called from the original
`RenderSystem.initBackendSystem`, whose complete body is already replaced.

`MacosUtil` is platform-specific and is not part of the normal browser path.

`MessageBox` and `Blaze3D.openUri` are still useful browser utility targets
for a later cleanup pass.

## Result

After the current patch set, SDL references can still physically exist in
unused desktop classes, but normal browser startup no longer needs the native
SDL library merely because those classes are present in the transformed JAR.
