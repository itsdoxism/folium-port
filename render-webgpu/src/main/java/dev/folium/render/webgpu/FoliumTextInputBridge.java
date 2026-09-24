package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;

public final class FoliumTextInputBridge {
    private FoliumTextInputBridge() {
    }

    public static void startTextInput() {
        FoliumRuntime.platform().input().startTextInput();
    }

    public static void stopTextInput() {
        FoliumRuntime.platform().input().stopTextInput();
    }

    public static boolean isTextInputActive() {
        return FoliumRuntime.platform().input().textInputActive();
    }
}
