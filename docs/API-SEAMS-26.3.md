# Minecraft 26.3 method-level seams

This note uses class-file metadata only. It does not store decompiled Minecraft
source.

## RenderPearl backend

`com.mojang.renderpearl.api.device.GpuBackend` is an interface with a small
surface:

```text
getName() -> String
loadLibrary() -> void
unloadLibrary() -> void
createWindow(String, int, int, long) -> long
createDevice(GpuDebugOptions) -> GpuDevice
```

This is a promising browser-port seam.

A Folium backend can potentially replace the native window handle represented
by the `long` with a browser-host token/adapter while keeping the higher
RenderPearl frontend intact.

## GpuDevice

The public device interface exposes operations for:

- creating a surface;
- creating command encoders;
- creating samplers;
- creating textures and texture views;
- creating GPU buffers;
- pipeline compilation;
- timestamp query pools;
- device information and shutdown.

That shape maps much more naturally to **WebGPU** than to old fixed-function
WebGL emulation.

The first Folium renderer experiment should therefore explore:

```text
RenderPearl GpuBackend
        |
        v
FoliumWebGpuBackend
        |
        v
Browser WebGPU
```

rather than attempting a global LWJGL shim.

## GpuSurface

The surface interface includes:

```text
configure(...)
currentConfiguration()
supportedPresentModes()
acquireNextTexture()
blitFromTexture(...)
present()
```

This resembles a modern swapchain/surface API and is another strong signal
that WebGPU is a suitable first target.

## Networking is different

`net.minecraft.network.Connection` directly extends Netty's
`SimpleChannelInboundHandler`.

It stores a Netty `Channel` and has Netty-facing methods such as:

```text
channelActive(...)
channelInactive(...)
channelRead0(...)
configurePacketHandler(...)
configureSerialization(...)
connect(...)
```

but also contains higher-level Minecraft-facing methods:

```text
send(Packet)
tick()
disconnect(...)
setupInboundProtocol(...)
setupOutboundProtocol(...)
initiateServerboundPlayConnection(...)
```

Therefore replacing only one socket call is not enough.

### Likely Folium strategy

Keep Minecraft packet/protocol logic but introduce a browser-specific
connection implementation or patch layer around the connection class.

The packet codec side still depends heavily on Netty `ByteBuf`, so Folium
will likely need either:

1. a TeaVM-compatible subset of Netty buffer APIs; or
2. targeted replacements for Minecraft's buffer wrappers.

The event-loop/socket/channel side should then become WebSocket-based.

## Current priority

Rendering now has a cleaner seam than networking.

Recommended implementation order:

1. TeaVM WASM-GC smoke build;
2. inspect RenderPearl backend constructor/selection path;
3. prototype `FoliumWebGpuBackend`;
4. delay audio;
5. tackle Netty buffer compatibility and WebSocket transport after visible
   rendering is proven.
