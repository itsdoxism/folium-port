package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;

public final class FoliumSystemBridge {
    private FoliumSystemBridge() {
    }

    public static double timeSeconds() {
        return FoliumRuntime.platform().clock().nanoTime() / 1_000_000_000.0;
    }
}
