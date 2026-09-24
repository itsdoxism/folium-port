# Milestone 0.0.11 — Java ByteBuffer to WebGPU

Folium now has a direct upload path from Java NIO buffers into browser WebGPU.

## Working path

```text
Java ByteBuffer
      |
      v
TeaVM JSO
      |
      v
JavaScript ArrayBufferView
      |
      v
GPUQueue.writeBuffer(...)
      |
      v
GPUBuffer
```

TeaVM's modern NIO/JS interop can pass NIO buffers across the JSO boundary,
avoiding a mandatory Java byte-array serialization layer.

## Device initial-data buffers

The RenderPearl overload:

```text
GpuDevice.createBuffer(label, usage, ByteBuffer initialData)
```

now:

1. slices the Java buffer at its current position;
2. allocates a WebGPU-backed `FoliumGpuBuffer`;
3. adds COPY_DST when necessary;
4. uploads the slice through `GPUQueue.writeBuffer`;
5. returns the normal RenderPearl buffer wrapper.

## CommandEncoder writes

Folium also implements:

```text
CommandEncoder.writeToBuffer(GpuBufferSlice destination, ByteBuffer data)
```

The destination slice offset is preserved and the source length is validated
against the slice length.

## WebGPU alignment

WebGPU requires `queue.writeBuffer` destination offsets and write sizes to be
4-byte aligned.

Folium currently:

- rejects unaligned destination offsets explicitly;
- pads only the final upload tail when the Java buffer length is not divisible
  by four.

This keeps the common aligned path direct while still supporting short index
buffers such as three uint16 values.

## Indexed triangle proof is now Java-owned

The indexed triangle probe no longer asks JavaScript to create hard-coded
indices.

It constructs this data in Java:

```text
ByteBuffer.allocateDirect(6)
  -> uint16 0
  -> uint16 1
  -> uint16 2
```

and uploads it through the same `GpuDevice.createBuffer(..., ByteBuffer)`
path that future Minecraft mesh data will use.

## What this unlocks

Folium can now carry real Java-side mesh bytes into WebGPU buffers.

The next renderer work is therefore no longer basic byte transport. It is:

1. map Minecraft `VertexFormat` and `VertexFormatElement` to WebGPU vertex
   buffer layouts;
2. translate actual RenderPearl pipeline state;
3. handle shader source translation/compilation;
4. bind samplers, textures and uniform buffers;
5. draw the first real Minecraft mesh.
