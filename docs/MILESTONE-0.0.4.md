# Milestone 0.0.4 — first WebGPU frame

Folium now has a host-side WebGPU bootstrap invoked from Java/WASM.

## Target flow

```text
FoliumMain.java
      |
      v
TeaVM WASM-GC
      |
      v
@JSBody host bridge
      |
      +-- navigator.gpu.requestAdapter()
      +-- adapter.requestDevice()
      +-- canvas.getContext("webgpu")
      +-- context.configure(...)
      +-- command encoder
      +-- render pass clear
      +-- queue.submit(...)
```

## Pass condition

The page status becomes:

```text
Folium WASM-GC → WebGPU first frame OK
```

and the canvas is presented by WebGPU.

The JS host keeps the initial adapter/device/context in:

```text
globalThis.__foliumWebGpu
```

only as a temporary bootstrap handle.

## Why the WebGPU setup is still in JavaScript

This milestone tests the path, not the final API design.

Implementing RenderPearl's complete `GpuDevice`, `GpuSurface`,
`CommandEncoder`, texture and buffer contracts before proving browser WebGPU
initialization would create too much code with no validated foundation.

After this milestone passes, the next task is to wrap these host handles from
Java and start implementing the smallest RenderPearl subset needed to create a
surface and present frames.

## Browser requirements

Use a browser with WebGPU enabled and serve the files over HTTP/HTTPS.

Do not use `file://`.
