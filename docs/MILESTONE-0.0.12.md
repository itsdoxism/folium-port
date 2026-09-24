# Milestone 0.0.12 — Minecraft VertexFormat mapping

Folium now translates Minecraft 26.3's RenderPearl vertex metadata into a
WebGPU-oriented vertex-buffer layout model.

## 26.3 source contract

Class-file inspection shows that `VertexFormat` exposes:

```text
getVertexSize()  -> int
getStepRate()    -> int
getElements()    -> List<VertexFormatElement>
```

Each `VertexFormatElement` exposes:

```text
name()
offset()
format() -> GpuFormat
```

This maps naturally to WebGPU:

```text
VertexFormat.getVertexSize()
    -> GPUVertexBufferLayout.arrayStride

VertexFormat.getStepRate()
    -> stepMode

ordered VertexFormatElement
    -> GPUVertexAttribute[]
       shaderLocation
       offset
       format
```

## Step-rate policy

Folium currently maps:

```text
0 -> vertex
1 -> instance
```

Rates greater than one fail explicitly because WebGPU's vertex layout exposes
only `vertex` or `instance` step modes rather than arbitrary divisors.

## Supported attribute shapes

The mapper covers WebGPU-compatible scalar/vector forms for:

- 8-bit integer / normalized values;
- 16-bit integer / normalized values;
- float16 x2/x4;
- 32-bit integer x1/x2/x3/x4;
- float32 x1/x2/x3/x4.

Formats with no direct WebGPU vertex-attribute equivalent fail explicitly.
That includes several three-component packed 8/16-bit formats.

## Validation

Every attribute is checked against the RenderPearl vertex stride:

```text
offset + format.blockSize() <= vertexSize
```

so malformed or incorrectly translated layouts stop before pipeline creation.

## Why this matters

Folium can already transport Java mesh bytes into `GPUBuffer` objects.

This milestone provides the missing description of how those bytes should be
interpreted by WebGPU.

The next renderer milestone is to pass these translated layouts into actual
WebGPU pipeline creation and replace the bootstrap `vertex_index` triangle
with a real Java-owned vertex buffer.

After that, the remaining major renderer work is Minecraft shader and pipeline
state translation.
