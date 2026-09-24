# Folium patcher

The patcher transforms a **user-supplied local Minecraft 26.3 client JAR** at
build time.

It does not download, embed, or commit Minecraft binaries.

## Current transforms

### PreferredGraphicsApi

Replaces the desktop backend preference list:

```text
OpenGL / Vulkan
```

with:

```text
FoliumWebGpuBackend
```

### RenderSystem.initBackendSystem

Removes the early SDL initialization path and returns Folium's browser-safe
nano-time source instead.

## Run

```bash
gradle :patcher:run --args="$FOLIUM_MINECRAFT_JAR build/folium-minecraft-26.3.jar"
```

The generated JAR belongs in local build output and must not be committed.

## Important

This is only the first pair of patches. Minecraft still has SDL references in
`Window`, input, clipboard, monitor and event code. Those become later
targeted transforms once the browser boot path reaches them.
