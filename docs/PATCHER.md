# Folium client patcher

Folium never stores or redistributes the Minecraft client JAR.

Instead, a local official client JAR is transformed reproducibly during the
developer build.

## Input

For Pandora Launcher, a typical 26.3 path is:

```text
~/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar
```

## Verify patch targets

Before patching:

```bash
python tools/verify-patch-targets.py \
  "$HOME/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar"
```

The verifier checks the exact classes, method names and JVM descriptors used by
the patcher.

## Run

```bash
gradle :patcher:run --args="\
$HOME/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar \
$PWD/local/minecraft-client-26.3-folium.jar"
```

The original JAR is never modified in place.

## Current patch set

### ShaderManager

`loadConfigs(ResourceManager)`:

- starts a new raw-include staging snapshot;
- commits the snapshot on its single successful return.

`loadInclude(Identifier, Resource, ImmutableMap.Builder)`:

- captures raw GLSL include source before Minecraft wraps it in native
  shaderc-backed state.

### PreferredGraphicsApi

`getBackendsToTry()` is replaced with a one-element array containing:

```text
FoliumWebGpuBackend
```

so the browser build never selects desktop OpenGL or Vulkan.

### RenderSystem

`initBackendSystem()` is completely replaced.

Desktop 26.3 initializes SDL here and returns a clock backed by
`SDL_GetTicksNS`.

The patched client instead calls:

```text
FoliumBackendBootstrap.initBackendSystem()
```

which returns a `TimeSource.NanoTimeSource` backed by:

```text
FoliumRuntime.platform().clock().nanoTime()
```

No SDL metadata, hints, initialization, error calls or SDL timer calls remain
reachable through this method.

## Browser platform bootstrap

`FoliumMain` now installs `BrowserFoliumPlatform` before renderer startup.

The first real platform service is:

```text
BrowserClockHost
  nanoTime()          -> performance.now() * 1_000_000
  currentTimeMillis() -> Date.now()
```

Graphics is provided by `BrowserGraphicsHost`.

Window, network, storage and audio remain explicit incomplete subsystems so
they fail visibly rather than silently pretending to work.

## Mojang JAR signatures

The official 26.3 client JAR is signed.

Any bytecode transformation invalidates those signatures, so the patcher:

- removes `META-INF/*.SF`;
- removes `META-INF/*.RSA`, `*.DSA` and `*.EC`;
- rewrites the manifest to retain normal launch metadata.

## Drift safety

Every patch targets an exact method name and JVM descriptor.

The patcher refuses to emit an output JAR when a required class/method is
missing or its expected shape changes.

## Determinism

JAR entry timestamps are reset while copying, and the patcher prints SHA-256
hashes for both input and output.

## Next patch targets

The next browser-shell targets are the SDL-facing window and event classes:

```text
Window
MonitorManager
SDLEventHandler
InputConstants
MouseHandler
```

The first useful goal is a browser-backed window shell that can supply canvas
size, title and pointer-lock state without constructing any SDL window.
