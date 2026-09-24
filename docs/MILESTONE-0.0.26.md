# Milestone 0.0.26 — SDL utility cleanup

Folium now replaces the remaining browser-relevant desktop utility calls that still used SDL/native desktop behavior.

## Blaze3D external navigation

The patched methods:

```text
Blaze3D.openUri(URI)
Blaze3D.openPath(Path)
```

now call the browser window host.

`openUri` uses `window.open(..., '_blank', 'noopener,noreferrer')`.

`openPath` converts the Java `Path` to a URI first. Browser security rules may block local-file navigation; Folium logs that failure instead of invoking a native desktop opener.

## MessageBox

```text
MessageBox.error(String)
MessageBox.errorWithContinue(String)
```

now use browser dialogs through `WindowHost`.

The current implementation maps to `alert` / `confirm` as a simple compatibility layer. A custom in-canvas overlay can replace these later without touching Minecraft bytecode again.

## MacosUtil

The following desktop-only methods are now browser no-ops:

```text
MacosUtil.disableCloseWindowMenuItem()
MacosUtil.setFullscreenMenuVisibility(boolean)
MacosUtil.setCtrlClickEmulatesRightClick(boolean)
```

They are irrelevant in the browser environment.

## Platform API

`WindowHost` now owns:

```text
openExternal(uri)
showError(message)
showErrorWithContinue(message)
```

so render-side patch bridges remain independent of TeaVM/browser implementation details.

## Verification

Against the supplied Minecraft 26.3 client JAR, all new utility patch targets matched exactly once.

## SDL status

At this point the normal browser boot and interaction path no longer depends on SDL for:

- native library startup;
- renderer timer/bootstrap;
- window creation;
- monitor/display state;
- keyboard/mouse/wheel events;
- pointer lock;
- text input;
- clipboard;
- cursor initialization;
- external URI launching;
- error dialogs;
- macOS window-menu helpers.

Unused desktop OpenGL/Vulkan classes may still physically contain SDL references in the transformed local JAR, but Folium never selects those backends.

## Next major blocker

The next major browser boundary is networking.

Minecraft 26.3's `Connection` still extends Netty `SimpleChannelInboundHandler` and assumes native/socket-oriented Netty channels.

The next milestone should establish:

```text
Minecraft packet/protocol codecs
        |
        v
Folium network transport seam
        |
        v
WebSocket (WSS)
        |
        v
gateway/proxy
        |
        v
normal Minecraft TCP backend
```

The goal is to preserve Minecraft packet encoding/decoding while replacing the socket/event-loop transport layer.
