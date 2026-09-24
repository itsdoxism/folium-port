# Milestone 0.0.8 — full Minecraft reachability target

Folium now has a separate TeaVM target whose main class is Minecraft 26.3
itself:

```text
net.minecraft.client.main.Main
```

This target is intentionally distinct from the small Folium WebGPU smoke test.

## Build graph

```text
local official Minecraft 26.3 client JAR
        |
        v
:patcher:patchMinecraft
        |
        v
build/folium/folium-minecraft-26.3.jar
        |
        +-- Pandora Java dependency JARs
        +-- Folium render-webgpu
        +-- Folium minecraft-bridge
        +-- Folium platform-api
        |
        v
TeaVM WASM-GC
        |
        v
folium-client.wasm
```

## Pandora defaults

For a client installed at:

```text
~/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar
```

Folium infers the dependency root as:

```text
~/.local/share/PandoraLauncher/libraries
```

It excludes:

- the original Minecraft client JAR, because the patched JAR replaces it;
- native classifier JARs;
- source/javadoc JARs.

## Commands

```bash
export FOLIUM_MINECRAFT_JAR="$HOME/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar"

gradle :patcher:patchMinecraft
gradle :client-wasm:buildWasmGC --stacktrace
```

If the launcher layout differs:

```bash
export FOLIUM_MINECRAFT_LIBRARIES="/path/to/libraries"
```

## Why this milestone matters

The first failure from this target is no longer a guessed architecture issue.

It is the **actual next unsupported class/method in Minecraft's TeaVM reachable
graph**.

From this point forward, Folium can become an iterative port:

```text
compile
  -> first unsupported boundary
  -> patch/bridge it
  -> compile again
```

rather than manually porting the whole client in advance.
