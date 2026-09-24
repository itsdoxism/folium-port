# Milestone 0.0.7 — RenderPearl commands reach WebGPU

Folium now has its first direct command path from Minecraft 26.3's RenderPearl
interfaces to browser WebGPU objects.

## Working path

```text
GpuDevice.createTexture(...)
        |
        v
FoliumGpuTexture
        |
        v
GraphicsHost.createTexture(...)
        |
        v
GPUDevice.createTexture(...)

GpuDevice.createCommandEncoder()
        |
        v
FoliumCommandEncoder
        |
        +-- clearColorTexture(...)
        |       |
        |       v
        |   WebGPU render pass clear
        |
        +-- submit()
                |
                v
        GPUQueue.submit(...)
```

Browser resources are stored in a small token registry. Minecraft-facing Java
objects hold only opaque integer tokens, keeping JavaScript/WebGPU object types
out of the RenderPearl-facing modules.

## Texture support

The first bridge recognizes a deliberately small WebGPU-compatible subset of
Minecraft's `GpuFormat` values, including common color/float/depth formats.

Unsupported formats fail explicitly.

For this milestone, RenderPearl texture usage flags are not translated
bit-for-bit yet. The host allocates a safe WebGPU usage superset:

- COPY_SRC
- COPY_DST
- TEXTURE_BINDING
- RENDER_ATTACHMENT

This is temporary and should be tightened once the 26.3 usage constants are
mapped.

## CommandEncoder support

Implemented:

- `submit()`
- `clearColorTexture(...)`

Mapped but intentionally unsupported:

- render passes;
- depth clears;
- buffer writes/copies;
- image uploads;
- texture copies;
- fences;
- timestamp queries.

Every unsupported operation throws a named exception so the first Minecraft
boot attempt identifies the next missing contract precisely.

## Validation note

The uploaded Minecraft 26.3 client targets Java 25 class files. The current
analysis environment provides JDK 21, so the Minecraft-dependent module cannot
be fully javac-compiled here against that JAR.

To reduce source-level mismatch risk, Folium's method signatures were checked
directly against 26.3 class-file descriptors and `MethodParameters` metadata.

## Next milestone

The next useful step is `GpuTextureView` plus a minimal
`createRenderPass(...)` implementation.

That opens the path from a simple clear command to the render-pass model used
by Minecraft's real renderer:

```text
texture
  -> texture view
  -> RenderPass
  -> pipeline/bindings
  -> draw
  -> submit
```
