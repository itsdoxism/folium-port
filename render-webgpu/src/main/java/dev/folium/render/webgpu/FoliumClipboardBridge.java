package dev.folium.render.webgpu;

import dev.folium.runtime.BrowserClipboardBridge;

public final class FoliumClipboardBridge {
    private FoliumClipboardBridge() {
    }

    public static String getClipboard() {
        BrowserClipboardBridge.refreshClipboard();
        return BrowserClipboardBridge.getClipboard();
    }

    public static void setClipboard(String text) {
        BrowserClipboardBridge.setClipboard(text);
    }
}
