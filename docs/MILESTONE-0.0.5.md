# Milestone 0.0.5 — RenderPearl contract shell

This milestone moves Folium from a standalone WebGPU probe toward a real
Minecraft rendering backend.

## Included

- WebGPU adapter/device/context initialization happens **before** Java main.
- `FoliumWebGpuBackend` implements RenderPearl's `GpuBackend`.
- `FoliumGpuDevice` implements the complete public `GpuDevice` method
  surface, initially failing loudly for unimplemented GPU primitives.
- `FoliumGpuSurface` implements configuration/acquire/present lifecycle state.

## Why WebGPU initializes before Minecraft

RenderPearl's backend creation API is synchronous:

```text
GpuBackend.createDevice(...)
```

but browser WebGPU initialization is asynchronous:

```text
navigator.gpu.requestAdapter()
adapter.requestDevice()
```

Folium resolves this mismatch by performing browser GPU initialization in the
HTML/JS bootstrap before TeaVM starts Minecraft.

The resulting browser handles are stored in temporary host state:

```text
globalThis.__foliumWebGpu
```

Later modules will replace direct global access with a typed Folium host bridge.

## Next blocker

The first call expected to fail once Minecraft reaches real rendering is one
of:

- command encoder creation;
- buffer creation;
- texture creation;
- pipeline compilation.

That failure is useful: it tells us the exact primitive Minecraft requests
first, so implementation can proceed from the real boot path instead of
building the entire GPU API blindly.
