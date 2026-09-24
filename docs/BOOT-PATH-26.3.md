# Minecraft 26.3 browser boot path

This is based on class-file/bytecode inspection of a locally supplied official
26.3 client JAR. No Minecraft binaries or decompiled source are stored here.

## Backend selection

Minecraft's `PreferredGraphicsApi.getBackendsToTry()` directly constructs:

```text
GlBackend
VulkanBackend
```

If Vulkan is preferred it returns Vulkan first, otherwise OpenGL first.

For a Folium browser build the intended patch is conceptually:

```text
PreferredGraphicsApi.getBackendsToTry()
        |
        +-- desktop build -> GlBackend / VulkanBackend
        |
        +-- Folium build  -> FoliumWebGpuBackend
```

This gives us a clean RenderPearl insertion point.

## Important discovery: backend replacement is not enough

Before backend selection, Minecraft calls:

```text
RenderSystem.initBackendSystem()
```

In 26.3 that method directly initializes SDL and sets SDL platform metadata and
hints.

The `Window` class also directly calls SDL for monitor/window state, and
input code uses SDL independently of RenderPearl.

A scan found **27 Minecraft/Mojang classes** directly referencing
`org.lwjgl.sdl`:

```text
com.mojang.blaze3d.Blaze3D
com.mojang.blaze3d.platform.ClipboardManager
com.mojang.blaze3d.platform.InputConstants
com.mojang.blaze3d.platform.InputConstants$Type
com.mojang.blaze3d.platform.MacosUtil
com.mojang.blaze3d.platform.MessageBox
com.mojang.blaze3d.platform.Monitor
com.mojang.blaze3d.platform.MonitorManager
com.mojang.blaze3d.platform.NativeLibrariesBootstrap
com.mojang.blaze3d.platform.SDLEventHandler
com.mojang.blaze3d.platform.SdlDebug
com.mojang.blaze3d.platform.TextInputManager
com.mojang.blaze3d.platform.VideoMode
com.mojang.blaze3d.platform.Window
com.mojang.blaze3d.platform.cursor.CursorType
com.mojang.blaze3d.platform.cursor.CursorTypes
com.mojang.blaze3d.systems.RenderSystem
com.mojang.renderpearl.backend.opengl.GlBackend
com.mojang.renderpearl.backend.opengl.GlDevice
com.mojang.renderpearl.backend.opengl.GlSurface
com.mojang.renderpearl.backend.vulkan.VulkanBackend
com.mojang.renderpearl.backend.vulkan.VulkanGpuSurface
com.mojang.renderpearl.backend.vulkan.VulkanInstance
com.mojang.renderpearl.backend.vulkan.VulkanPhysicalDevice
net.minecraft.client.Minecraft
net.minecraft.client.MouseHandler
net.minecraft.client.input.InputQuirks
```

## Folium boot architecture

So Folium now has **two independent browser seams**:

### 1. Browser shell

Replace the SDL-facing platform behavior:

```text
RenderSystem.initBackendSystem
Window
MonitorManager
SDLEventHandler
InputConstants
MouseHandler
ClipboardManager
TextInputManager
        |
        v
Folium platform-api
        |
        v
DOM / Canvas / Pointer Lock / Clipboard / performance.now
```

### 2. Rendering

Keep RenderPearl's frontend and provide a browser backend:

```text
Minecraft renderer
      |
RenderPearl frontend
      |
GpuBackend / GpuDevice / GpuSurface
      |
FoliumWebGpuBackend
      |
WebGPU
```

## Why this is still promising

The renderer and the desktop shell are separable enough that we do not need to
rebuild Minecraft gameplay.

The main work is now clearly:

1. remove SDL from the reachable browser boot path;
2. make `Window` and input use Folium platform adapters;
3. implement the RenderPearl WebGPU device;
4. later replace Netty's socket/event-loop side while preserving protocol code.

## Immediate next target

Before attempting the full client, build a standalone browser test that proves:

- a canvas can be acquired;
- WebGPU adapter/device initialization works;
- a frame can be cleared/presented;
- the Java/WASM side can call that host bridge.

That becomes Folium's first real graphics milestone.
