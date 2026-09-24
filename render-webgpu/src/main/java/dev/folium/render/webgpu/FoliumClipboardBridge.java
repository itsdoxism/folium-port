package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;

public final class FoliumClipboardBridge {
    private FoliumClipboardBridge() {
    }

    public static String getClipboard() {
        return FoliumRuntime.platform().input().getClipboard();
    }

    public static void setClipboard(String text) {
        FoliumRuntime.platform().input().setClipboard(text);
    }
}
