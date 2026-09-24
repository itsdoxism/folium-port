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

If loading throws, the previous active snapshot remains intact. A future reload
starts a fresh staging map.

`loadInclude(Identifier, Resource, ImmutableMap.Builder)`:

- calls `FoliumShaderReloadHook.captureInclude(...)` before Minecraft converts
  the include to its native shaderc-backed representation.

### PreferredGraphicsApi

`getBackendsToTry()` is replaced with a one-element array containing:

```text
FoliumWebGpuBackend
```

so browser builds do not instantiate the desktop OpenGL or Vulkan backends.

## Mojang JAR signatures

The official 26.3 client JAR is signed.

Any bytecode transformation invalidates those signatures, so the patcher:

- removes `META-INF/*.SF`;
- removes `META-INF/*.RSA`, `*.DSA` and `*.EC`;
- rewrites the manifest to retain only the normal launch metadata.

Without this step a modified signed JAR can fail verification at class-load
time.

## Drift safety

Every patch targets an exact method name **and JVM descriptor**.

The patcher refuses to emit an output JAR when:

- a required class is missing;
- a target method is missing;
- a method descriptor changes;
- `ShaderManager.loadConfigs` no longer has the expected single return shape.

This is intentional. A Minecraft update should fail loudly instead of producing
a subtly corrupted client.

## Determinism

JAR entry timestamps are reset while copying, and the patcher prints SHA-256
hashes for both input and output.

## Next patch

The next target is:

```text
RenderSystem.initBackendSystem()
```

which currently initializes SDL directly.

Folium will replace that method with a browser-safe bridge returning
Minecraft's `TimeSource.NanoTimeSource` without touching SDL.
