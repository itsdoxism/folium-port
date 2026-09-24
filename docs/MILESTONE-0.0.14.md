# Milestone 0.0.14 — RenderPipeline state translation

Folium now translates Minecraft 26.3 RenderPearl pipeline metadata into a
WebGPU-oriented pipeline-state model.

## Mapped state

### Primitive topology

```text
POINTS           -> point-list
LINES            -> line-list
DEBUG_LINES      -> line-list
DEBUG_LINE_STRIP -> line-strip
TRIANGLES        -> triangle-list
TRIANGLE_STRIP   -> triangle-strip
```

`TRIANGLE_FAN` and `QUADS` require topology/index expansion because WebGPU
has no direct primitive mode for them.

### Culling

RenderPearl exposes culling as a boolean. Folium currently maps:

```text
false -> none
true  -> back
```

with counter-clockwise front faces.

### Polygon mode

```text
FILL -> supported
WIREFRAME -> unsupported in core WebGPU
```

Wireframe will need geometry/index expansion or a shader-based technique.

### Color target state

Folium translates:

- target format;
- RGBA write mask;
- optional blend function;
- separate color/alpha blend equations;
- blend factors and operations.

### Depth state

Folium translates:

- compare operation;
- depth write enable;
- depth bias constant;
- depth bias slope scale.

The first implementation uses `depth24plus` as the browser depth target
format until surface/depth attachment format selection is generalized.

### Vertex bindings

Every RenderPearl vertex binding is passed through
`FoliumVertexFormatMapper`, producing one WebGPU vertex-buffer layout per
binding.

## Remaining gap

This milestone creates a translated **descriptor model**, but Minecraft's real
shader modules are still not translated.

The next renderer milestone is to take a real `RenderPipeline`, serialize
this state into the browser host, and build a `GPURenderPipeline` using a
temporary compatible shader.

That will validate real Minecraft pipeline-state transport before the shader
translation problem is tackled directly.
