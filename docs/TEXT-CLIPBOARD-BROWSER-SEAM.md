# Browser text input and clipboard seam

Minecraft 26.3 uses SDL-backed `TextInputManager` and `ClipboardManager`.
Folium replaces both for browser builds.

## Text input

Browser text entry is driven by a hidden textarea.

```text
TextInputManager.startTextInput(owner)
        |
        v
FoliumTextInputBridge
        |
        v
InputHost.startTextInput()
        |
        v
BrowserTextInputBridge
        |
        v
hidden textarea focus
```

The textarea listens for `compositionend` and `input`, then emits normal Folium `TEXT` events back into the input queue.

The patched class preserves Minecraft's Java-side state: `owner`, `textInputEnabled`, and text-input-area coordinates. The SDL text-input-area application itself becomes a no-op because the hidden browser textarea is not positioned over the canvas.

## Clipboard

Minecraft's clipboard API is synchronous, while the browser Clipboard API is permission-gated and asynchronous. Folium therefore uses a synchronous cached mirror.

Reads return the cached browser clipboard string and opportunistically refresh it with `navigator.clipboard.readText()` and paste events.

Writes update the cache immediately, then attempt `navigator.clipboard.writeText(text)` asynchronously.

## Platform boundary

Clipboard and text-input operations are exposed through `InputHost`, keeping `render-webgpu` independent from `runtime-wasm` and avoiding a module dependency cycle.

## Current limitations

Browser clipboard permissions can prevent direct clipboard access. In that case the cache still works for clipboard events seen inside the page, but may lag behind an external system clipboard change until the next paste/read opportunity.

Text-input-area positioning is not yet used for mobile virtual-keyboard placement.
