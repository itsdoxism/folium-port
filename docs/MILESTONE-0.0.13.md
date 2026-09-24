# Milestone 0.0.13 — real Java vertex buffers

Folium now has a real Java-owned vertex-buffer draw path using Minecraft 26.3
RenderPearl vertex layout metadata.

## Working path

```text
Java ByteBuffer
   |
   v
GpuDevice.createBuffer(...)
   |
   v
GPUBuffer

RenderPearl VertexFormat
   |
   v
FoliumVertexFormatMapper
   |
   v
WebGPU vertex-buffer layout
   |
   v
GPURenderPipeline

RenderPass.setVertexBuffer(...)
   |
   v
GPURenderPassEncoder.setVertexBuffer(...)

RenderPass.draw(3, 1, 0, 0)
   |
   v
GPURenderPassEncoder.draw(...)
```

## Probe layout

The first translated layout is intentionally simple:

```text
stride = 16 bytes

offset 0   RGB32_FLOAT   Position
offset 12  RGBA8_UNORM   Color
```

The vertex data is built in Java as an interleaved direct `ByteBuffer`.

## Why this is important

Earlier triangle probes still generated positions in WGSL using
`@builtin(vertex_index)`.

This milestone removes that shortcut.

Vertex position and color now come from Java-side bytes uploaded through the
same buffer path future Minecraft meshes will use.

## VertexFormat translation

Folium maps:

```text
vertexSize -> arrayStride
stepRate   -> vertex / instance
element.offset -> GPUVertexAttribute.offset
element.format -> GPUVertexFormat
element order  -> shaderLocation
```

Unsupported RenderPearl formats fail explicitly rather than silently selecting
an incompatible WebGPU format.

## What remains before a real Minecraft mesh

The transport and vertex-layout path now exist.

The major renderer blockers are:

1. translate real `RenderPipeline` state;
2. translate Minecraft shader sources into WebGPU-compatible shaders;
3. implement bind groups for textures/samplers/uniform buffers;
4. support depth/stencil attachments;
5. handle Minecraft's actual vertex-format combinations and topology edge
   cases such as quads/fans where WebGPU has no direct primitive mode.

Once those are in place, Folium can attempt the first actual Minecraft-rendered
geometry instead of Folium-owned probe geometry.
