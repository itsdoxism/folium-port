# Milestone 0.0.6 — first RenderPearl wrappers

Folium now crosses the first real Minecraft rendering API boundary.

## Implemented

`FoliumWebGpuBackend.createDevice()` now returns a real Java object that
implements Minecraft 26.3's `GpuDevice` interface:

```text
GpuBackend
   |
   v
FoliumWebGpuBackend
   |
   v
FoliumGpuDevice
   |
   v
FoliumGpuSurface
```

The surface currently implements:

- configuration state;
- current configuration;
- a conservative FIFO presentation mode;
- acquire/present lifecycle state;
- close lifecycle.

The remaining GPU operations fail explicitly instead of silently pretending to
work.

## Why explicit failure is useful

At this stage we want the first Minecraft renderer boot attempt to stop at the
**first genuinely missing GPU operation**.

That turns the port into an incremental process:

```text
boot
  -> first missing call
  -> implement that WebGPU contract
  -> boot again
  -> next missing call
```

instead of attempting hundreds of RenderPearl methods speculatively.

## Exact 26.3 GpuDevice surface

The client interface includes:

- `createSurface`
- `createCommandEncoder`
- `createSampler`
- `createTexture`
- `createTextureView`
- `createBuffer`
- debug message access
- `compilePipeline`
- timestamp query pools
- device info
- lifecycle close

## Important next dependency

A visible RenderPearl-driven frame requires `CommandEncoder`, and that
interface immediately reaches:

- render passes;
- texture clears;
- buffer copies/writes;
- texture uploads;
- fences/query pools.

So the next practical implementation is not the entire encoder. It is:

1. a host command-encoder token;
2. `clearColorTexture`;
3. `submit`;
4. a minimal texture/view wrapper for the current canvas texture.

That is enough to prove a RenderPearl call can cause a real WebGPU command
submission.
