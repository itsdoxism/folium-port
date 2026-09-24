package dev.folium.render.webgpu;

import dev.folium.platform.FoliumRuntime;
import net.minecraft.util.TimeSource;

/**
 * Browser replacement for RenderSystem.initBackendSystem().
 *
 * Desktop Minecraft initializes SDL here and returns SDL_GetTicksNS as its
 * nano-time source. Folium has no SDL layer, so browser time comes from the
 * installed platform clock.
 */
public final class FoliumBackendBootstrap {
    private FoliumBackendBootstrap() {
    }

    public static TimeSource.NanoTimeSource initBackendSystem() {
        if (!FoliumRuntime.isInstalled()) {
            throw new IllegalStateException(
                "Folium platform must be installed before RenderSystem backend init"
            );
        }

        return () -> FoliumRuntime.platform().clock().nanoTime();
    }
}
