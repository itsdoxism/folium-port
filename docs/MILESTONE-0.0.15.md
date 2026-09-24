# Milestone 0.0.15 — shader translation architecture

The Minecraft 26.3 rendering path has now been traced through its shader
frontend.

## Confirmed path

```text
ShaderSource
   |
   v
GLSL source + includes + ShaderDefines
   |
   v
GlslCompiler.compileToSpv(...)
   |
   v
SpvModule / SPIRVModule
   |
   v
backend pipeline creation
```

Folium inserts its browser-specific shader conversion after SPIR-V generation:

```text
SPIR-V -> Naga -> WGSL -> WebGPU
```

## Why SPIR-V is the seam

Minecraft's existing frontend already performs shader preprocessing and
compilation before handing shader modules to the graphics backend.

Using that output avoids reimplementing:

- Minecraft include handling;
- shader defines;
- GLSL preprocessing;
- much of GLSL semantic analysis.

## Added module

`shader-translator/` is a standalone Rust/WASM module using Naga with:

```text
spv-in
wgsl-out
```

Its first exported operation is:

```text
spv_to_wgsl(bytes)
```

which parses, validates and emits WGSL.

## Current status

The translator source is scaffolded but not compiled in the current execution
environment because Rust/Cargo is unavailable here.

The Java/WebGPU side is ready for the next integration step: load this WASM
translator beside the TeaVM runtime and pass translated WGSL into real
Minecraft pipeline creation.
