# Milestone 0.0.7 — render-loop skeleton

Folium can now carry RenderPearl's frontend state through a complete render-pass
lifecycle without invoking OpenGL/Vulkan/SDL.

The temporary flow is:

```text
Minecraft UI/world renderer
        |
FrontendCommandEncoder
        |
FoliumBackendCommandEncoder
        |
FoliumBackendRenderPass
        |
validate state, discard draw
```

## What works

- pipeline binding;
- uniform binding;
- push-constant validation on the RenderPearl frontend;
- vertex/index buffer binding;
- scissor state;
- indexed/non-indexed draw entry points;
- render-pass creation and submission.

## What is intentionally missing

Draw calls do not yet generate WebGPU commands.

This is useful because it lets Folium separate two questions:

1. **Can Minecraft 26.3 finish client startup without desktop-native APIs?**
2. **Can Folium reproduce each GPU operation in WebGPU?**

The first can now be tested much farther before the second is complete.

## Native type leak

RenderPearl exposes one `org.lwjgl.PointerBuffer` overload in its public render
pass API. The Gradle module currently carries LWJGL only as a compile-time
signature dependency. A browser-specific type shim or bytecode API patch will
remove this dependency before the full TeaVM reachability build.
