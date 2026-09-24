# Milestone 0.0.8 — RenderPearl render passes reach WebGPU

Folium now supports the first real RenderPearl render-pass lifecycle.

## Working path

```text
GpuDevice.createTexture(...)
        |
        v
FoliumGpuTexture
        |
        v
GpuDevice.createTextureView(...)
        |
        v
FoliumGpuTextureView
        |
        v
CommandEncoder.createRenderPass(...)
        |
        v
FoliumRenderPass
        |
        +-- optional clear
        +-- scissor
        +-- debug groups
        +-- close() -> GPURenderPassEncoder.end()
        |
        v
CommandEncoder.submit()
        |
        v
GPUQueue.submit(...)
```

## Implemented texture-view operations

- full-texture views;
- mip-range views;
- width/height queries relative to the view;
- explicit close/release lifecycle.

Browser-side `GPUTextureView` objects are stored behind opaque integer tokens.

## Implemented render-pass operations

- color-only render pass creation;
- clear or load behavior depending on RenderPearl's optional clear color;
- debug-group push/pop;
- scissor rectangle enable;
- scissor reset to full attachment size;
- pass close/end.

Depth attachments are intentionally not wired yet.

## Why this matters

This is the first point where Folium is no longer issuing only standalone
commands. It can now mirror the same command structure that Minecraft's real
renderer uses:

```text
begin pass
  -> configure pass
  -> eventually bind pipeline/buffers
  -> draw
end pass
submit
```

## Current hard stop

Actual draw calls still require:

- `CompiledRenderPipeline`;
- vertex buffers;
- index buffers;
- bind groups/uniform bindings;
- shader translation/compilation.

The next useful milestone is therefore:

1. implement `GpuBuffer` and `GpuBufferSlice`;
2. map RenderPearl buffer usage flags to WebGPU;
3. implement `setVertexBuffer`;
4. implement a minimal compiled pipeline wrapper;
5. issue the first triangle through `RenderPass.draw(...)`.

At that point, Folium will have a true RenderPearl-to-WebGPU draw path rather
than only clears.
