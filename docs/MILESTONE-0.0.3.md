# Milestone 0.0.3 — WASM/browser bridge

The first real browser milestone is deliberately smaller than Minecraft boot.

## Pass condition

Folium-owned Java code compiled by TeaVM to WASM-GC must:

1. load in a normal browser;
2. call JavaScript through TeaVM JSO;
3. find and size an HTML canvas;
4. detect `navigator.gpu`;
5. report the result back into the page.

This proves:

```text
Java
  ↓
TeaVM
  ↓
WASM-GC
  ↓
JSO bridge
  ↓
Browser APIs
```

before Minecraft classes enter the reachable graph.

## Build

```bash
gradle :runtime-wasm:buildWasmGC
```

Serve the repository root over HTTP; do not open the HTML using `file://`.

For example:

```bash
python -m http.server 8080
```

Then open:

```text
http://localhost:8080/runtime-wasm/web/
```

## Expected result

On a WebGPU-capable browser:

```text
Folium WASM-GC → browser bridge OK · WebGPU available
```

If WebGPU is disabled or unavailable, the bridge itself should still pass and
report that WebGPU is unavailable.

## Next milestone

0.0.4 will request a real WebGPU adapter/device and clear the canvas through a
small host bridge.

Only after that works do we begin implementing RenderPearl's `GpuDevice`
contract.
