# Milestone 0.0.5 — async WebGPU preboot

## Problem

RenderPearl exposes a synchronous device factory:

```text
GpuBackend.createDevice(...) -> GpuDevice
```

WebGPU exposes asynchronous initialization:

```text
await navigator.gpu.requestAdapter()
await adapter.requestDevice()
```

Trying to perform those awaits inside RenderPearl's synchronous method would
fight both APIs.

## Folium solution

Move asynchronous browser initialization before Minecraft starts:

```text
HTML launcher
    |
    +-- await requestAdapter()
    +-- await requestDevice()
    +-- configure canvas
    +-- store host state
    |
    v
start TeaVM / Minecraft
    |
    v
FoliumWebGpuBackend.createDevice()
    |
    +-- synchronously inspect prepared host
    +-- return Java wrapper around prepared device
```

The temporary host state is exposed at:

```text
globalThis.__foliumHostState
```

Java accesses only a tiny typed `GraphicsHost` abstraction.

## Why this matters

This removes a major impedance mismatch before the Minecraft client is in the
WASM reachability graph.

The eventual launcher can also use this phase to check:

- browser feature support;
- adapter availability;
- limits/features;
- canvas setup;
- future audio unlock state;
- persistent storage availability.

## Next milestone

Implement the first Java `GpuDevice` wrapper around the prepared host token.

Initially, unsupported RenderPearl methods may fail explicitly. The target is
to implement enough of:

```text
GpuDevice
GpuSurface
CommandEncoder
GpuTexture/View
```

to clear/present a frame through the same RenderPearl calls Minecraft uses.
