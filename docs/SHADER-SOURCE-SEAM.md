# Browser-safe shader source seam

## Correction to the first shader plan

Minecraft 26.3's desktop `GlslCompiler` is not browser-portable.

Although it outputs SPIR-V, it does so through LWJGL shaderc:

```text
GlslCompiler
  -> org.lwjgl.util.shaderc
  -> native shaderc
```

Therefore Folium cannot simply keep that compiler unchanged under TeaVM.

## Revised path

Folium takes over before native shaderc:

```text
Minecraft ShaderSource
        |
        +-- raw GLSL
        +-- ShaderDefines
        +-- raw include sources
        |
        v
Folium preprocessing
        |
        v
Naga GLSL frontend
        |
        v
Naga IR
        |
        v
WGSL
        |
        v
GPUShaderModule
```

## Include problem

The stock `ShaderSource` API exposes:

```text
getShader(...) -> String
getInclude(...) -> CachedIncludeSource
```

but `CachedIncludeSource` is designed around shaderc and stores a native
include-result pointer rather than browser-usable raw include text.

Minecraft's `ShaderManager` originally has the raw include text while loading
resources, before it wraps the include in `CachedIncludeSource`.

Therefore the Folium browser patch must retain raw include strings in a
browser-safe registry during shader resource reload.

The intended registry contract is conceptually:

```text
shader id + stage -> raw GLSL
include id        -> raw include GLSL
```

The registry then recursively expands `#include <minecraft:...>` before Naga
is called.

## Defines

`ShaderDefines.asSourceDirectives()` is browser-safe Java code and can be
inserted immediately after the GLSL `#version` line.

## Current preprocessor

`FoliumShaderPreprocessor`:

- inserts RenderPipeline shader defines;
- checks that all includes were expanded first;
- fails with the unresolved include identifier when the source registry patch
  is incomplete.

This keeps shader failures deterministic while the raw-include registry is
being implemented.
