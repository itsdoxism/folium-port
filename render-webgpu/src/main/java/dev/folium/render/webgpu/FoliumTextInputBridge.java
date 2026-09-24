package dev.folium.render.webgpu;

import dev.folium.runtime.BrowserTextInputBridge;

public final class FoliumTextInputBridge {
    private FoliumTextInputBridge() {
    }

    public static void startTextInput() {
        BrowserTextInputBridge.start();
    }

    public static void stopTextInput() {
        BrowserTextInputBridge.stop();
    }

    public static boolean isTextInputActive() {
        return BrowserTextInputBridge.isActive();
    }
}
