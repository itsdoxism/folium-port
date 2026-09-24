# Milestone 0.0.24 — synthetic browser display replaces SDL monitor enumeration

Folium now removes Minecraft 26.3's normal SDL monitor/video-mode discovery from the browser startup path.

## Working model

```text
Browser canvas framebuffer
        |
        v
FoliumDisplayBootstrap
        |
        v
Synthetic Monitor
        |
        v
MonitorManager / Window fullscreen queries
```

## Synthetic display

The browser build currently exposes one display:

```text
id:           1
name:         Folium Browser Display
position:     0,0
color bits:   8 / 8 / 8
refresh rate: 60 Hz
mode count:   1
size:         current canvas framebuffer
```

The current mode and preferred mode are the same object.

## Patched classes

### Monitor

```text
tryCreate(int)
queryMonitorName(int)
```

are redirected to Folium's synthetic display model.

### MonitorManager

Its constructor now initializes an `Int2ObjectMap` containing one synthetic monitor.

Display-connect/disconnect/mode-change callbacks become no-ops, while `getMonitor`, `addDisplay`, and `findBestMonitor` return the browser display.

### VideoMode

The SDL-specific constructor:

```text
VideoMode(SDL_DisplayMode)
```

is removed from the transformed class because the browser path no longer has a caller for it.

The ordinary Java integer/float constructors remain unchanged.

## Verification

Against the supplied Minecraft 26.3 client JAR, every new patch target matched exactly once:

```text
Monitor.tryCreate
Monitor.queryMonitorName
MonitorManager.<init>
MonitorManager.onDisplayConnected
MonitorManager.onDisplayDisconnected
MonitorManager.onDisplayModeChanged
MonitorManager.addDisplay
MonitorManager.getMonitor
MonitorManager.findBestMonitor
VideoMode.<init>(SDL_DisplayMode)
```

## SDL shell status

The normal browser boot path now avoids SDL for:

- renderer bootstrap;
- native window creation;
- monitor enumeration;
- framebuffer sizing;
- event polling;
- keyboard/mouse state;
- pointer lock;
- text input;
- clipboard.

## Next target

The next useful pass is a remaining-SDL reachability audit over Minecraft 26.3 after the current patch set.

Likely targets include native-library bootstrap and small platform utility calls that are no longer conceptually necessary in a browser build.
