# Browser Window seam

Minecraft 26.3's `Window` constructor performs substantial SDL work:

- primary display lookup;
- native window creation;
- minimum-size configuration;
- monitor positioning;
- SDL window-position queries;
- fullscreen setup;
- framebuffer queries.

Folium replaces the main eight-argument constructor in browser builds.

## Browser state

The patched constructor initializes Minecraft's expected Java fields directly:

```text
eventHandler
monitorManager
handle = 1
width / height
windowedWidth / windowedHeight
framebufferWidth / framebufferHeight
x / y
focused
preferredFullscreenVideoMode
fullscreen state
cursor state
```

Framebuffer dimensions come from:

```text
BrowserWindowHost
  -> #folium-canvas.clientWidth/clientHeight
  -> devicePixelRatio
  -> canvas.width/canvas.height
```

and are clamped to Minecraft's normal minimum window dimensions.

## Patched methods

The patcher currently replaces or neutralizes:

```text
Window.<init>(..., int)
Window.getPlatform()
Window.queryFramebufferSize()
Window.refreshFramebufferSize()
Window.setTitle()
Window.setWindowMaxSize()
Window.close()
Window.setIcon(...)
Window.updateFullscreenIfChanged()
Window.changeFullscreenVideoMode()
Window.selectCursor(...)
Window.setFullscreen(...)
```

The title bridge writes to `document.title`.

Fullscreen and cursor selection are intentionally conservative no-ops until the
browser fullscreen/pointer-lock lifecycle is fully connected.

## Window handle

The patched Java object uses opaque handle `1L`.

Folium's WebGPU surface path treats this as an opaque browser window identity;
it is not an SDL/native pointer.

## Remaining direct SDL users

Replacing `Window` construction does not remove all SDL dependencies.

The next highest-priority classes are:

```text
SDLEventHandler
InputConstants
MouseHandler
TextInputManager
ClipboardManager
MonitorManager
```

Those must be redirected to DOM keyboard, pointer, wheel, text-input and
clipboard events before the normal Minecraft input loop can run in the browser.
