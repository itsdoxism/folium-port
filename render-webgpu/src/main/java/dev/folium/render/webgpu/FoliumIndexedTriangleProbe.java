package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.folium.platform.GraphicsHost;
import org.joml.Vector4f;

import java.util.Optional;

public final class FoliumIndexedTriangleProbe {
    private FoliumIndexedTriangleProbe() {
    }

    public static void draw(
        GpuDevice device,
        GraphicsHost graphics,
        GpuTextureView colorTarget,
        FoliumCompiledRenderPipeline pipeline
    ) {
        try (FoliumGpuBuffer indices = FoliumGpuBuffer.bootstrapTriangleIndexBuffer(graphics)) {
            var encoder = device.createCommandEncoder();

            try (var pass = encoder.createRenderPass(
                () -> "Folium indexed triangle",
                colorTarget,
                Optional.of(new Vector4f(0.035f, 0.055f, 0.045f, 1.0f))
            )) {
                pass.setPipeline(pipeline);
                pass.setIndexBuffer(indices, IndexType.SHORT);
                pass.drawIndexed(3, 1, 0, 0, 0);
            }

            encoder.submit();
        }
    }
}
