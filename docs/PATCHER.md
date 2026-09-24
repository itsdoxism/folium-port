# Folium client patcher

Folium transforms a local official Minecraft client JAR; the original JAR is never stored in the repository or modified in place.

## Current transformed areas

The 26.3 browser patch set now covers:

```text
ShaderManager        -> raw shader include capture
PreferredGraphicsApi -> FoliumWebGpuBackend only
RenderSystem         -> browser clock, no SDL init
Window               -> browser canvas shell
SDLEventHandler      -> DOM event queue
InputConstants       -> browser key state / pointer lock
TextInputManager     -> hidden textarea / composition
ClipboardManager     -> browser clipboard cache
Monitor              -> synthetic browser display
MonitorManager       -> one synthetic monitor
VideoMode            -> remove SDL_DisplayMode constructor
```

## Verification

Run:

```bash
python tools/verify-patch-targets.py /path/to/minecraft-client-26.3.jar
```

Every patch target is matched by exact JVM method descriptor. Target drift aborts the patch instead of emitting a partially compatible browser client.

## Signed JAR handling

The official client JAR is signed. Since bytecode transformation invalidates those signatures, Folium strips signature metadata and emits a clean launch manifest in the patched local artifact.

## Next audit

With window/input/display SDL paths redirected, the next step is to scan the transformed-reachable platform graph for remaining direct `org/lwjgl/sdl` references and patch only those still reachable from normal browser startup.
