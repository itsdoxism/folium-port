# Folium architecture

## Principle

Folium is a **platform port**, not a Minecraft clone.

We keep portable Minecraft client logic wherever possible and isolate code that assumes a desktop JVM, native libraries, operating-system files, or raw sockets.

## Initial platform boundaries

### Window and input

Minecraft 26.3 contains SDL-facing client platform code. Folium will eventually replace the desktop window/event boundary with browser APIs:

- HTML canvas
- Pointer Lock
- KeyboardEvent / MouseEvent
- Gamepad API where useful
- Clipboard API

### Rendering

Do not begin by rewriting the renderer.

First map calls crossing from `com.mojang.blaze3d` into native graphics APIs. Folium should expose a narrow graphics backend and progressively implement it over WebGL2 or WebGPU.

### Networking

Browsers cannot open Minecraft's raw TCP connection directly.

Target topology:

```text
Folium browser client
      |
      | wss://
      v
Folium/Mellow gateway
      |
      | Minecraft protocol / TCP
      v
Velocity / backend server
```

### Storage

Desktop filesystem assumptions must become browser storage/fetch abstractions. Multiplayer-only boot is the first priority; integrated-server and singleplayer storage are explicitly out of scope for the first playable milestone.

### Native dependencies

Any JNI/JNA/native library boundary is considered a port blocker until one of these is true:

1. the feature is unnecessary for multiplayer boot and can be disabled;
2. a pure-Java implementation exists and TeaVM can compile it;
3. Folium provides a browser implementation;
4. the feature is moved behind a JS/WASM host bridge.

## Development phases

### Phase 0 — inventory

Produce a machine-readable report from the official client JAR:

- classes/packages;
- native-facing package references;
- platform classes;
- Java version;
- Minecraft protocol/version metadata.

### Phase 1 — Folium platform API

Create small interfaces for browser-hosted services instead of patching browser calls throughout Minecraft.

### Phase 2 — WASM smoke test

Compile Folium-owned Java code to WASM-GC with TeaVM. This proves the toolchain independently of Minecraft.

### Phase 3 — bootstrap slicing

Start from `net.minecraft.client.Main` and discover the first unsupported dependency chain. Patch one boundary at a time.

### Phase 4 — render and input

Reach a visible canvas, then menu/title rendering.

### Phase 5 — multiplayer

Replace raw networking with a WebSocket gateway and target a controlled test server.

## Non-goals for v0.1

- singleplayer/integrated server;
- mod loader compatibility;
- shaders;
- broad third-party server compatibility;
- redistribution of Minecraft binaries or assets.
