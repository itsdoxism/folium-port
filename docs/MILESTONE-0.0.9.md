# Milestone 0.0.9 — first triangle draw path

Folium now has a complete non-indexed draw path from a RenderPearl-style
`RenderPass` into WebGPU.

## Working path

```text
FoliumCompiledRenderPipeline
        |
        v
RenderPass.setPipeline(...)
        |
        v
GPURenderPassEncoder.setPipeline(...)

RenderPass.draw(3, 1, 0, 0)
        |
        v
GPURenderPassEncoder.draw(...)
        |
        v
CommandEncoder.submit()
        |
        v
GPUQueue.submit(...)
```

## Bootstrap pipeline

The first pipeline intentionally uses Folium-owned WGSL and
`@builtin(vertex_index)`.

That means the proof triangle needs no vertex buffer yet.

This is deliberate: the goal of this milestone is to validate the draw-command
and pipeline-binding bridge independently from Minecraft shader translation and
buffer layout work.

## What this does not prove yet

The bootstrap pipeline is **not** Minecraft's real pipeline compiler.

Still missing:

- translation of Minecraft/RenderPearl shader sources to WGSL or another
  WebGPU-compatible representation;
- real `RenderPipeline` state mapping;
- vertex format mapping;
- `GpuBuffer` and `GpuBufferSlice` WebGPU backing;
- indexed draws;
- texture/sampler bind groups;
- push constants/uniform strategy;
- depth/stencil pipeline state.

## 26.3 buffer flags mapped during analysis

The client declares:

```text
USAGE_MAP_READ               = 1
USAGE_MAP_WRITE              = 2
USAGE_HINT_CLIENT_STORAGE    = 4
USAGE_COPY_DST               = 8
USAGE_COPY_SRC               = 16
USAGE_VERTEX                 = 32
USAGE_INDEX                  = 64
USAGE_UNIFORM                = 128
USAGE_UNIFORM_TEXEL_BUFFER   = 256
USAGE_INDIRECT_PARAMETERS    = 512
```

These give Folium a direct next step for translating RenderPearl buffer usage
to WebGPU `GPUBufferUsage`.

## Next milestone

Implement `FoliumGpuBuffer` and buffer-slice support, then wire:

```text
GpuDevice.createBuffer(...)
RenderPass.setVertexBuffer(...)
RenderPass.setIndexBuffer(...)
RenderPass.drawIndexed(...)
```

After that, the remaining major renderer blocker becomes real Minecraft
pipeline/shader compilation rather than basic GPU command transport.
