# Folium client patcher

Folium transforms a local official Minecraft client JAR into a browser-oriented build without storing or modifying the original JAR in place.

## Current transformed areas

```text
ShaderManager              -> raw shader include capture
PreferredGraphicsApi       -> FoliumWebGpuBackend only
RenderSystem               -> browser clock, no SDL init
Window                     -> browser canvas shell
SDLEventHandler            -> DOM event queue
InputConstants             -> browser key state / pointer lock
TextInputManager           -> hidden textarea / composition
ClipboardManager           -> browser clipboard cache
Monitor / MonitorManager   -> synthetic browser display
VideoMode                  -> remove SDL_DisplayMode constructor
NativeLibrariesBootstrap   -> no native-library loading
Blaze3D.getTime            -> browser clock
CursorType                 -> browser-safe opaque cursors
InputQuirks                -> no SDL modifier polling
MouseHandler.resync        -> browser pointer state
Blaze3D.openUri/openPath   -> browser external navigation
MessageBox                 -> browser dialogs
MacosUtil                  -> browser no-op
```

## Verification

Run the exact-descriptor verifier before patching:

```bash
python tools/verify-patch-targets.py /path/to/minecraft-client-26.3.jar
```

Target drift aborts patch generation rather than producing a partially compatible client.

## Signed JAR handling

The official client JAR is signed. Folium removes invalidated signature metadata and emits a clean local launch manifest after transformation.

## Next major subsystem

The desktop SDL shell is no longer the primary blocker. The next large browser boundary is Netty/TCP networking, followed by the remaining WebGPU pipeline/binding completeness work.
