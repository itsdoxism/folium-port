# Milestone 0.0.16 — browser shader compiler seam

Folium's shader architecture has been corrected after inspecting the actual
Minecraft 26.3 compiler implementation.

## Important finding

`GlslCompiler.compileToSpv(...)` depends on native LWJGL shaderc.

That dependency cannot live in a TeaVM browser graph.

## New translator

The Rust/WASM translator now consumes **preprocessed GLSL** directly:

```text
glsl_to_wgsl(source, stage)
```

using Naga's GLSL frontend and WGSL backend.

## Browser loader

`runtime-wasm/web/folium-shader-translator.js` loads the wasm-bindgen package
and exposes:

```text
globalThis.__foliumShaderTranslator.glslToWgsl(...)
```

for the WebGPU host.

## Remaining source-side task

Minecraft's stock `CachedIncludeSource` is native-shaderc-oriented and does
not provide raw include text back to Java.

The next patch therefore needs to retain raw shader include resources during
`ShaderManager` reload and recursively expand them for Folium.

Once that registry exists, `FoliumGpuDevice.compilePipeline(...)` can perform:

```text
RenderPipeline shader IDs
    -> raw GLSL
    -> expand includes
    -> apply ShaderDefines
    -> glsl_to_wgsl
    -> GPUShaderModule
    -> GPURenderPipeline
```
