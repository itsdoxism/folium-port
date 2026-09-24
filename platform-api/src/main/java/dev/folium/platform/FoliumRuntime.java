package dev.folium.platform;

import java.util.Objects;

/**
 * Process-global Folium host installed by the browser bootstrap before
 * Minecraft enters its synchronous initialization path.
 */
public final class FoliumRuntime {
    private static FoliumPlatform platform;

    private FoliumRuntime() {
    }

    public static void install(FoliumPlatform value) {
        platform = Objects.requireNonNull(value, "platform");
    }

    public static boolean isInstalled() {
        return platform != null;
    }

    public static FoliumPlatform platform() {
        if (platform == null) {
            throw new IllegalStateException("Folium platform has not been installed");
        }
        return platform;
    }
}
