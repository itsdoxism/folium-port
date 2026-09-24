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
```

## Verification

Run the exact-descriptor verifier before patching:

```bash
python tools/verify-patch-targets.py /path/to/minecraft-client-26.3.jar
```

Target drift aborts patch generation rather than producing a partially compatible client.

## Signed JAR handling

The official client JAR is signed. Folium removes invalidated signature metadata and emits a clean local launch manifest after transformation.

## Remaining utility cleanup

The main SDL shell is now redirected. Remaining browser-utility targets include:

```text
Blaze3D.openUri
MessageBox
MacosUtil
```

These are lower risk than the earlier boot blockers and can be replaced with browser-native behavior in a focused utility patch pass.
