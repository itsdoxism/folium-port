# Minecraft 26.3 browser-boundary scan

This document records the first automated scan of a locally supplied official
Minecraft Java Edition 26.3 client JAR. No Minecraft binaries or decompiled
source are stored in this repository.

## JAR facts

- Minecraft version: **26.3**
- protocol: **777**
- Java runtime target reported by `version.json`: **25**
- class count: **11,383**
- dependency scanner parse errors: **0**

## External dependency surface

The first scan found **1,116 distinct external class references**.

Largest groups included:

| family | distinct referenced classes |
| --- | ---: |
| `it.unimi.dsi` | 219 |
| LWJGL Vulkan | 166 |
| Google Guava | 103 |
| Mojang DataFixerUpper | 73 |
| Mojang Authlib | 54 |
| Mojang Brigadier | 48 |
| LWJGL SDL | 40 |
| Netty channel | 39 |
| Apache Commons | 33 |
| Mojang Serialization | 30 |
| Netty handler | 28 |
| Gson | 26 |
| LWJGL OpenGL | 18 |
| LWJGL OpenAL | 16 |

Across the whole graph the scanner currently sees:

- **LWJGL:** 283 distinct external class references
- **Netty:** 92
- **SDL:** 40
- **OpenAL:** 16

These numbers measure referenced class names, not runtime call frequency.

## Window/input boundary

Minecraft 26.3 references SDL through LWJGL. Direct users include:

- `com.mojang.blaze3d.platform.Window`
- `com.mojang.blaze3d.platform.SDLEventHandler`
- `com.mojang.blaze3d.platform.InputConstants`
- `com.mojang.blaze3d.platform.ClipboardManager`
- `com.mojang.blaze3d.platform.Monitor`
- `net.minecraft.client.Minecraft`

This is the first clear Folium substitution boundary.

## Rendering boundary

26.3 contains Mojang's `com.mojang.renderpearl` layer with distinct OpenGL and
Vulkan backends.

Observed OpenGL-side classes include:

- `com.mojang.renderpearl.backend.opengl.GlBackend`
- `GlDevice`
- `GlCommandEncoder`
- `GlRenderPipeline`

The Vulkan backend is substantially larger and directly references Vulkan
LWJGL classes.

### Initial direction

For the first browser render path, Folium should **not port Vulkan**.

The useful question is whether a web implementation can sit behind the same
RenderPearl-level abstractions as the existing OpenGL/Vulkan backends. If yes,
that is a much cleaner target than emulating every LWJGL OpenGL call globally.

## Networking boundary

Netty appears throughout packet codecs because Minecraft uses Netty's
`ByteBuf` heavily, not only for opening TCP sockets.

That means "replace Netty with WebSocket" is too coarse.

The likely split is:

1. preserve or substitute the buffer/codec API used by packet serialization;
2. replace only the connection/event-loop/socket boundary with a WebSocket
   transport;
3. keep Minecraft packet logic above that boundary intact.

Relevant connection-facing classes include:

- `net.minecraft.client.gui.screens.ConnectScreen`
- `net.minecraft.client.multiplayer.ServerStatusPinger`
- `net.minecraft.client.multiplayer.LegacyServerPinger`

## Audio boundary

The client still references LWJGL OpenAL in the Blaze3D audio package.

Initial Folium should allow audio to be disabled until visible rendering and
multiplayer transport work. WebAudio can become a later backend.

## Next engineering target

1. prove Folium-owned Java compiles to TeaVM WASM-GC;
2. map RenderPearl backend interfaces in more detail;
3. map the network connection boundary separately from Netty `ByteBuf`;
4. begin a small browser-host implementation behind `platform-api`;
5. only then attempt to pull Minecraft classes into TeaVM reachability.
