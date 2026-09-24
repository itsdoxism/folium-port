# Folium shader translator

This module is the intended browser shader bridge for Minecraft 26.3.

The desktop RenderPearl frontend already performs:

```text
Minecraft GLSL
    |
    v
GlslCompiler
    |
    v
SPIR-V
```

Folium keeps that frontend contract and adds:

```text
SPIR-V
   |
   v
Naga
   |
   v
WGSL
   |
   v
GPUDevice.createShaderModule()
```

This avoids maintaining a custom GLSL-to-WGSL source translator.

## Why a separate WASM module?

TeaVM handles the Java client/runtime, while Naga is a mature Rust shader
translation library.

Keeping shader conversion in its own WASM module means:

- Minecraft Java code does not need a Rust FFI dependency;
- shader conversion can be tested independently;
- translated WGSL can be cached by SPIR-V hash;
- WebGPU receives ordinary WGSL source strings.

## Build target

The intended browser build target is:

```text
wasm32-unknown-unknown
```

with `wasm-bindgen` glue.

The current development environment does not contain a Rust toolchain, so this
module is scaffolded but has not yet been compiled here.
