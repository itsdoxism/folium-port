package dev.folium.render.webgpu;

import com.mojang.renderpearl.backend.api.BackendRenderPipeline;

/**
 * Placeholder backend pipeline used while Folium's GLSL->SPIR-V->WGSL path is
 * being implemented.
 */
public final class FoliumBackendRenderPipeline implements BackendRenderPipeline {
    private final String name;
    private boolean closed;

    public FoliumBackendRenderPipeline(String name) {
        this.name = name;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }

    public String name() {
        return name;
    }
}
