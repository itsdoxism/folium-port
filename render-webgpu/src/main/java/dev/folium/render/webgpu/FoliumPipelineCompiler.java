package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Browser-safe temporary replacement for RenderPearl PipelineBuilder.
 *
 * It preserves the public frontend pipeline object and declared resource
 * layout, but deliberately skips native shaderc/SPIR-V compilation until the
 * Folium shader bridge is ready.
 */
public final class FoliumPipelineCompiler {
    private FoliumPipelineCompiler() {
    }

    public static CompletableFuture<CompiledRenderPipeline.Pending> compile(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        Executor executor
    ) {
        List<BindGroupLayout.UniformDescription> uniforms =
            BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts());

        Object2IntOpenHashMap<String> uniformIndices =
            new Object2IntOpenHashMap<>();
        uniformIndices.defaultReturnValue(-1);

        for (int index = 0; index < uniforms.size(); index++) {
            String name = uniforms.get(index).name();
            if (!uniformIndices.containsKey(name)) {
                uniformIndices.put(name, index);
            }
        }

        FoliumBackendRenderPipeline backend =
            new FoliumBackendRenderPipeline(pipeline.getLocation().toString());

        CompiledRenderPipeline.Pending pending = () ->
            new FrontendRenderPipeline(
                pipeline.getLocation().toString(),
                backend,
                pipeline.getVertexFormatBindings(),
                uniformIndices,
                uniforms,
                pipeline.getColorTargetStates(),
                pipeline.wantsDepthTexture(),
                pipeline.pushConstantSize()
            );

        return CompletableFuture.completedFuture(pending);
    }
}
