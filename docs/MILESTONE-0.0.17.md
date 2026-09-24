# Milestone 0.0.17 — real pipeline compilation path

Folium now connects Minecraft 26.3's `GpuDevice.compilePipeline(...)` contract
to the browser shader translator and WebGPU pipeline creation.

## Working path

```text
RenderPipeline
    |
    +-- vertex shader id
    +-- fragment shader id
    +-- ShaderDefines
    +-- pipeline state
    |
    v
ShaderSource.getShader(...)
    |
    v
FoliumShaderPreprocessor
    |
    +-- apply ShaderDefines
    +-- reject unresolved includes
    |
    v
browser shader translator
    |
    v
Naga GLSL -> WGSL
    |
    v
GPUDevice.createShaderModule(...)
    |
    v
GPUDevice.createRenderPipeline(...)
    |
    v
CompiledRenderPipeline.Pending
```

## Important correction

Minecraft's desktop `GlslCompiler` cannot be reused in TeaVM because it calls
native LWJGL shaderc.

Folium therefore now compiles preprocessed GLSL directly with Naga in its own
Rust/WASM module.

## Browser preboot

The shader translator is loaded beside WebGPU during page startup.

If the wasm-bindgen package has not been built yet, Folium keeps the diagnostics
runtime alive and reports that the shader translator package is missing instead
of crashing during preboot.

## Current hard stop

Many Minecraft shaders use:

```text
#include <minecraft:...>
```

The stock `ShaderSource.CachedIncludeSource` stores a native shaderc include
result and does not expose raw include text for browser preprocessing.

The next renderer task is therefore to patch shader resource reload so Folium
retains raw include source strings and recursively expands includes before Naga
translation.

After that, `compilePipeline(...)` can attempt real core Minecraft shaders
instead of only include-free sources.
