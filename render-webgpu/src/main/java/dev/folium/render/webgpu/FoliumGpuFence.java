package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.commands.GpuFence;

/**
 * Bootstrap fence. CPU-side Folium operations are synchronous for now, so a
 * newly-created fence is immediately complete.
 */
public final class FoliumGpuFence implements GpuFence {
    private boolean closed;

    @Override
    public boolean awaitCompletion(long timeout) {
        return !closed;
    }

    @Override
    public void close() {
        closed = true;
    }
}
