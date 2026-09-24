package dev.folium.bridge;

import net.minecraft.util.TimeSource;

/**
 * Browser-safe replacement for RenderSystem.initBackendSystem()'s SDL setup.
 *
 * The first implementation delegates to System.nanoTime(), which TeaVM can
 * later route to the browser clock. Keeping this behind a Minecraft-facing
 * bridge means the bytecode patch remains tiny and stable.
 */
public final class FoliumTime {
    private static final TimeSource.NanoTimeSource SOURCE = System::nanoTime;

    private FoliumTime() {
    }

    public static TimeSource.NanoTimeSource nanoTimeSource() {
        return SOURCE;
    }
}
