package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;

public final class FoliumInputBridge {
    private FoliumInputBridge() {
    }

    public static boolean isKeyDown(int keyCode) {
        return FoliumRuntime.platform().input().keyDown(keyCode);
    }

    public static double mouseX() {
        return FoliumRuntime.platform().input().mouseX();
    }

    public static double mouseY() {
        return FoliumRuntime.platform().input().mouseY();
    }

    public static void grabMouse() {
        FoliumRuntime.platform().window().requestPointerLock();
    }

    public static void releaseMouse() {
        FoliumRuntime.platform().window().releasePointerLock();
    }
}
