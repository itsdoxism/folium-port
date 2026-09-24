# Minecraft 26.3 shader path

## Client-side shader frontend

Minecraft 26.3 contains:

```text
ShaderSource
GlslCompiler
PipelineBuilder
SPIRVModule
SpvUtil
```

The relevant contracts are:

```text
ShaderSource.getShader(Identifier, ShaderType) -> String

GlslCompiler.compileToSpv(
    String name,
    String source,
    ShaderType type,
    ShaderDefines defines,
    ShaderSource sourceProvider
) -> SpvModule
```

`PipelineBuilder` consumes a `RenderPipeline` and `ShaderSource`, then
builds the backend pipeline asynchronously.

## Shader assets

The official 26.3 client JAR contains core, include and post shader resources.

Inspection of the core vertex/fragment shaders shows:

- GLSL 330;
- explicit location qualifiers;
- shared include files;
- shader defines;
- texture samplers and uniforms.

The proprietary shader files are not copied into this repository.

## Folium seam

Folium should preserve Minecraft's existing frontend processing where possible:

```text
Minecraft shader resource
        |
        +-- includes
        +-- ShaderDefines
        |
        v
GlslCompiler
        |
        v
SPIR-V
        |
        v
Folium shader translator WASM
        |
        v
WGSL
        |
        v
WebGPU shader module
```

This seam is preferable to a custom textual GLSL rewriter because RenderPearl
has already resolved Minecraft-specific preprocessing before SPIR-V reaches the
backend boundary.

## Browser constraint

Browser WebGPU shader modules consume WGSL source. SPIR-V therefore cannot be
passed straight to `GPUDevice.createShaderModule()`.

Folium's shader translator module uses Naga's SPIR-V frontend and WGSL backend.

## Cache key

The intended shader cache key is based on:

```text
SPIR-V bytes
+ shader stage
+ translator version
```

so repeated pipelines sharing the same shader module do not translate it more
than once.

## Next step

Integrate the translator WASM module into `folium-host.js` and expose a host
operation conceptually equivalent to:

```text
translateSpirv(byteBuffer) -> WGSL string
```

Then connect `FoliumGpuDevice.compilePipeline(...)` to:

1. obtain Minecraft's compiled SPIR-V modules;
2. translate vertex and fragment stages;
3. combine them with `FoliumPipelineStateMapper`;
4. create the final WebGPU render pipeline.
