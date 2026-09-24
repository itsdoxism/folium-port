# RenderPearl notes

Minecraft Java 26.3 contains a substantial rendering layer under
`com.mojang.renderpearl`.

The class inventory is useful for Folium because there is an explicit API and
frontend/backend split instead of only direct OpenGL calls.

## Public-facing GPU API

The 26.3 JAR contains types including:

```text
com.mojang.renderpearl.api.device.GpuBackend
com.mojang.renderpearl.api.device.GpuDevice
com.mojang.renderpearl.api.device.GpuSurface
com.mojang.renderpearl.api.commands.CommandEncoder
com.mojang.renderpearl.api.commands.RenderPass
com.mojang.renderpearl.api.pipeline.RenderPipeline
com.mojang.renderpearl.api.buffers.GpuBuffer
com.mojang.renderpearl.api.textures.GpuTexture
```

There is also a lower-level backend API:

```text
com.mojang.renderpearl.backend.api.GpuDeviceBackend
com.mojang.renderpearl.backend.api.GpuSurfaceBackend
com.mojang.renderpearl.backend.api.CommandEncoderBackend
com.mojang.renderpearl.backend.api.RenderPassBackend
com.mojang.renderpearl.backend.api.BackendRenderPipeline
```

and a frontend layer:

```text
com.mojang.renderpearl.frontend.FrontendGpuDevice
com.mojang.renderpearl.frontend.FrontendGpuSurface
com.mojang.renderpearl.frontend.FrontendCommandEncoder
com.mojang.renderpearl.frontend.FrontendRenderPass
```

## Existing desktop implementations

Two concrete backend families are present:

```text
backend/opengl/*
backend/vulkan/*
```

The OpenGL backend references LWJGL OpenGL and SDL. The Vulkan backend is
larger and references LWJGL Vulkan and SDL.

## Folium hypothesis

The first rendering experiment should target a third backend:

```text
RenderPearl frontend
       |
       +-- OpenGL backend   (desktop)
       +-- Vulkan backend   (desktop)
       +-- Folium Web backend
                |
                +-- WebGPU first candidate
                +-- WebGL2 fallback, if practical
```

This is a hypothesis until method-level contracts are mapped, but it is a much
better seam than globally pretending browser WebGL is desktop LWJGL.

## Next inspection work

- dump method signatures of `GpuBackend`, `GpuDevice`, `GpuSurface`;
- identify where Minecraft chooses OpenGL vs Vulkan backend;
- identify shader representation before the backend boundary;
- determine whether the frontend assumes SPIR-V-specific behavior;
- decide whether WebGPU or WebGL2 is the smaller first implementation.
