# Milestone 0.0.18 — raw shader include expansion

Folium now has a browser-safe replacement for Minecraft 26.3's native
shaderc include path.

## Confirmed ShaderManager behavior

Minecraft's shader reload flow reads include text in:

```text
ShaderManager.loadInclude(...)
```

and immediately converts it into:

```text
ShaderSource.CachedIncludeSource
```

which stores only a native shaderc include-result pointer.

Folium must therefore capture the raw string before that conversion.

## Added registry

`FoliumShaderIncludeRegistry` maintains two snapshots:

```text
staging
active
```

A resource reload performs:

```text
beginReload()
  -> stage(include id, raw GLSL)
  -> stage(...)
  -> commitReload()
```

The complete map is atomically replaced at commit time so removed resource-pack
includes cannot remain as stale entries.

## Added reload hook

`FoliumShaderReloadHook` provides the small surface a future
`ShaderManager` patch needs:

```text
beginReload()
captureInclude(...)
commitReload()
abortReload()
```

It reproduces Minecraft's include ID mapping through
`ShaderManager.SHADER_INCLUDE_CONVERTER`.

## Recursive expansion

`FoliumShaderIncludeExpander` supports:

- namespaced includes such as `minecraft:fog.glsl`;
- default-namespace includes;
- nested includes;
- missing-include diagnostics;
- cycle detection;
- a depth guard.

`FoliumGpuDevice.compilePipeline(...)` now runs shader source through this
expander before applying `ShaderDefines` and sending GLSL to the browser
translator.

## Validation against the supplied 26.3 client JAR

The include checker found:

```text
core shaders:       63
raw includes:       17
missing includes:    0
include cycles:      0
max include depth:   3
```

So the recursive expansion model covers every core shader include relationship
present in that client JAR.

## Remaining integration point

The registry implementation is ready, but Minecraft-owned
`ShaderManager.loadInclude(...)` still needs a Folium patch injection so its
raw resource text is staged during normal client reload.

Once that hook is inserted, real core shader source can reach:

```text
Minecraft GLSL
  -> recursive includes
  -> ShaderDefines
  -> Naga GLSL -> WGSL
  -> WebGPU shader modules
```

The next milestone is the patch mechanism itself: a reproducible Folium
bytecode/source transformation for the 26.3 client JAR instead of manually
modifying Minecraft classes.
