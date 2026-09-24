# Folium

**Minecraft Java for the Web.**

Folium is an experimental browser porting project targeting **Minecraft Java Edition 26.3**. The goal is not to reimplement Minecraft gameplay in JavaScript. Instead, Folium aims to preserve the original client logic while replacing desktop-only platform boundaries with browser equivalents and compiling the portable Java side to WebAssembly.

## Current status

**Phase 0 — client boundary analysis**

The first milestone is intentionally small:

1. inspect the official 26.3 client JAR locally;
2. map native/desktop-only dependencies;
3. identify the minimum client bootstrap path;
4. define browser platform interfaces;
5. compile a tiny Folium bootstrap to WASM before attempting Minecraft startup.

The Minecraft 26.3 client JAR is **not** stored in this repository.

## Target architecture

```text
Minecraft 26.3 client logic
          |
          v
   Folium platform layer
   |       |       |
 render   input   network
   |       |       |
WebGL/  DOM/SDL  WebSocket
WebGPU  bridge   gateway
          |
          v
      TeaVM / WASM-GC
          |
          v
        Browser
```

## Local client JAR

Place your locally installed client at:

```text
local/minecraft-client-26.3.jar
```

For Pandora Launcher on Linux this may come from a path similar to:

```text
~/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar
```

Do not commit the JAR.

## Inspect the client

```bash
python tools/inspect-client.py local/minecraft-client-26.3.jar
```

The report is written to `reports/client-26.3.json`.

## Roadmap

- **0.0.1** — reproducible client inspection + dependency map
- **0.0.2** — browser platform API skeleton
- **0.0.3** — TeaVM/WASM-GC smoke test
- **0.0.4** — Minecraft bootstrap reaches first platform boundary
- **0.0.5** — canvas + input backend
- **0.0.6** — WebSocket multiplayer transport
- **0.1.0** — join and move inside a test server

## Legal note

Folium should contain only original Folium code, build tooling, and transformation/patch metadata. Minecraft binaries, decompiled source, and proprietary assets should not be committed to this repository.
