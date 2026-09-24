# Minecraft 26.3 shader path

Minecraft 26.3 ships core shaders as GLSL 330 source files.

Examples in the client assets begin with:

```glsl
#version 330
#extension GL_ARB_separate_shader_objects : require
```

The RenderPearl frontend contains:

```text
GlslCompiler
SPIRVModule
PipelineBuilder
```

and the compiler path references LWJGL shaderc APIs.

The desktop OpenGL backend also contains a `GlPipelineRecompiler` that
decompiles backend shader modules before compiling them as OpenGL shader
objects.

## Browser consequence

WebGPU's browser API creates shader modules from **WGSL source text**.

Therefore Folium cannot simply pass Minecraft's SPIR-V module directly into
`GPUDevice.createShaderModule()`.

The browser backend needs its own shader path.

## Candidate strategy

Keep Minecraft's original GLSL assets and preprocessing semantics, but replace
the native shaderc/SPIR-V path in Folium builds:

```text
Minecraft GLSL source
        |
include + define preprocessing
        |
        v
Folium shader translation
        |
        v
WGSL
        |
        v
GPUDevice.createShaderModule()
```

A practical implementation can use a browser/WASM shader translation library
rather than attempting to write a GLSL-to-WGSL compiler inside Folium.

## Native API leaks found so far

RenderPearl's API is mostly backend-neutral, but a few signatures leak native
dependencies:

- `RenderPass.multiDrawIndexed(...)` references `org.lwjgl.PointerBuffer`
- `ShaderSource.CachedIncludeSource` references a shaderc result type
- `GlslCompiler` directly references shaderc callbacks/handles

These should be handled with narrow Folium patches/shims rather than carrying
all of LWJGL into the browser runtime.

## Updated renderer milestones

1. preinitialize WebGPU before Java main;
2. implement RenderPearl device/surface lifecycle;
3. implement buffers/textures and command encoding;
4. replace native shader compilation with GLSL -> WGSL;
5. compile a simple Minecraft RenderPipeline;
6. render the first Minecraft-owned draw call.
