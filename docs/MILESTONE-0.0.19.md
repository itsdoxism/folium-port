# Milestone 0.0.19 — reproducible Minecraft bytecode patching

Folium now contains an actual Java bytecode patcher rather than only adapter
classes and documented patch points.

## Technology

The patcher uses ASM 9.10.1 and transforms a locally supplied Minecraft client
JAR.

No Minecraft class files are stored in the Folium repository.

## First transformed classes

```text
net.minecraft.client.renderer.ShaderManager
net.minecraft.client.PreferredGraphicsApi
```

## ShaderManager transformation

The transformed resource reload path now feeds raw include strings into
Folium's browser-safe include registry before Minecraft wraps those strings in
native shaderc objects.

That closes the missing integration point from milestone 0.0.18.

## Backend selection transformation

The desktop backend choice is replaced with:

```text
FoliumWebGpuBackend
```

for the patched browser client.

This prevents the normal path from constructing RenderPearl OpenGL/Vulkan
backend instances.

## Safety characteristics

The patcher:

- refuses in-place modification;
- matches exact JVM descriptors;
- checks expected method shape;
- deletes partial output on failure;
- emits deterministic entry timestamps;
- prints input and output SHA-256 hashes.

## Next target

The next client transformation removes the first SDL dependency from the
reachable browser boot graph:

```text
RenderSystem.initBackendSystem()
```

After that comes `Window`, event/input handling and the remaining SDL-facing
shell.
