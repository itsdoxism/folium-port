# Milestone 0.0.10 — WebGPU buffers and indexed draws

Folium now has a real RenderPearl GPU-buffer path and indexed rendering bridge.

## Implemented

### GpuBuffer

`FoliumGpuBuffer` now implements Minecraft 26.3's `GpuBuffer` interface:

- size;
- usage flags;
- close/destroy lifecycle;
- inherited slice support through RenderPearl's `GpuBufferSlice`.

Mapped buffers are intentionally not implemented yet.

### Device buffer creation

```text
GpuDevice.createBuffer(label, usage, size)
        |
        v
FoliumGpuBuffer
        |
        v
GraphicsHost.createBuffer(...)
        |
        v
GPUDevice.createBuffer(...)
```

RenderPearl usage flags are translated to WebGPU usage bits for:

- MAP_READ
- MAP_WRITE
- COPY_DST
- COPY_SRC
- VERTEX
- INDEX
- UNIFORM
- INDIRECT

### Render-pass buffer binding

Folium now implements:

```text
RenderPass.setVertexBuffer(...)
RenderPass.setIndexBuffer(...)
RenderPass.drawIndexed(...)
```

These reach the corresponding WebGPU render-pass methods.

## Indexed-triangle proof

A development probe creates a real `GPUBuffer` containing three uint16
indices:

```text
0, 1, 2
```

Then it executes:

```text
setPipeline(...)
setIndexBuffer(..., uint16)
drawIndexed(3, 1, 0, 0, 0)
```

The bootstrap WGSL shader still uses `@builtin(vertex_index)`, so this proves
real index-buffer transport before Minecraft vertex layouts are introduced.

## Current missing buffer feature

The overload:

```text
GpuDevice.createBuffer(label, usage, ByteBuffer initialData)
```

still fails explicitly.

The next buffer milestone needs a TeaVM-safe upload path from Java
`ByteBuffer` to browser `GPUQueue.writeBuffer`.

That is important because Minecraft's real meshes will arrive as Java-side
buffer data rather than Folium-owned bootstrap arrays.

## Next major renderer target

After Java buffer upload works, the renderer bottleneck becomes:

1. map Minecraft `VertexFormat` to WebGPU vertex-buffer layouts;
2. translate RenderPearl pipeline state;
3. translate/compile Minecraft shaders for WebGPU;
4. bind textures, samplers and uniform buffers;
5. render the first actual Minecraft mesh.
