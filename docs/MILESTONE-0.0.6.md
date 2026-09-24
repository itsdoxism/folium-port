# Milestone 0.0.6 — bootstrap GPU resources

Minecraft 26.3 initializes GPU-side support objects before creating the main
window.

The observed order is:

```text
RenderSystem.initRenderer(device)
        |
        +-- DynamicGpuData()
        |      |
        |      +-- MappableRingBuffer
        |              |
        |              +-- GpuDevice.createBuffer(...)
        |
        +-- SamplerCache.initialize()
               |
               +-- GpuDevice.createSampler(...)
```

Folium now supplies both primitives:

- `FoliumGpuBuffer` — CPU-backed bootstrap buffer with mapped slices;
- `FoliumGpuSampler` — RenderPearl sampler descriptor.

The CPU buffer is intentionally temporary. Its purpose is to preserve Minecraft
startup semantics while the WebGPU host bridge is still being built.

## Why CPU-backed first

Minecraft creates several mapped ring buffers during renderer initialization,
before any actual frame is submitted. A functional Java-side buffer object
lets the boot path continue without prematurely requiring the complete WebGPU
command/upload implementation.

Later, `MappedView.close()` becomes the natural dirty-range upload point for
WebGPU buffers.

## Next expected boundary

After renderer bootstrap succeeds, Minecraft proceeds through:

```text
DeviceInfo
Window
GpuSurface
SDLEventHandler
TextInputManager
MouseHandler / KeyboardHandler
```

The first real rendering/resource initialization after that is expected to
require textures, command encoders, and pipeline/shader compilation.
