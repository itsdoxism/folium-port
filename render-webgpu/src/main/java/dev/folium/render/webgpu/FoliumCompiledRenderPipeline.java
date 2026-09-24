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

    public static FoliumCompiledRenderPipeline vertexColor(
        GraphicsHost graphics,
        String layoutJson,
        String colorFormat
    ) {
        return new FoliumCompiledRenderPipeline(
            graphics,
            graphics.createVertexColorPipeline(layoutJson, colorFormat)
        );
    }

    public static FoliumCompiledRenderPipeline fromRenderPipeline(
        GraphicsHost graphics,
        com.mojang.renderpearl.api.pipeline.RenderPipeline pipeline
    ) {
        FoliumPipelineState state = FoliumPipelineStateMapper.map(pipeline);
        return new FoliumCompiledRenderPipeline(
            graphics,
            graphics.createPipelineFromState(FoliumPipelineStateJson.encode(state))
        );
    }

    public static FoliumCompiledRenderPipeline fromMinecraftShaders(
        GraphicsHost graphics,
        com.mojang.renderpearl.api.pipeline.RenderPipeline pipeline,
        String vertexGlsl,
        String fragmentGlsl
    ) {
        FoliumPipelineState state = FoliumPipelineStateMapper.map(pipeline);
        return new FoliumCompiledRenderPipeline(
            graphics,
            graphics.createPipelineFromGlsl(
                FoliumPipelineStateJson.encode(state),
                vertexGlsl,
                fragmentGlsl
            )
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
