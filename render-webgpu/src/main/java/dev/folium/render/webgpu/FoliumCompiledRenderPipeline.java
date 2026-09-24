package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import dev.folium.platform.GraphicsHost;

/**
 * Temporary pipeline wrapper used to prove RenderPearl -> WebGPU draw calls.
 *
 * The bootstrap pipeline uses Folium-owned WGSL. Minecraft shader translation
 * is a later milestone.
 */
public final class FoliumCompiledRenderPipeline implements CompiledRenderPipeline {
    private final GraphicsHost graphics;
    private final int token;
    private boolean closed;

    private FoliumCompiledRenderPipeline(GraphicsHost graphics, int token) {
        this.graphics = graphics;
        this.token = token;
    }

    public static FoliumCompiledRenderPipeline bootstrapTriangle(
        GraphicsHost graphics,
        String colorFormat
    ) {
        return new FoliumCompiledRenderPipeline(
            graphics,
            graphics.createBootstrapTrianglePipeline(colorFormat)
        );
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            graphics.destroyPipeline(token);
            closed = true;
        }
    }

    int token() {
        if (closed) {
            throw new IllegalStateException("Folium pipeline is closed");
        }
        return token;
    }
}
