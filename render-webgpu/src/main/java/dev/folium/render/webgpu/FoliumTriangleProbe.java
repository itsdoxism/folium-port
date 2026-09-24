package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.joml.Vector4f;

import java.util.Optional;

/**
 * Development-only proof that RenderPearl-style calls can issue a WebGPU draw.
 *
 * This deliberately uses Folium-owned WGSL instead of Minecraft shaders.
 */
public final class FoliumTriangleProbe {
    private FoliumTriangleProbe() {
    }

    public static void draw(
        GpuDevice device,
        GpuTextureView colorTarget,
        FoliumCompiledRenderPipeline pipeline
    ) {
        var encoder = device.createCommandEncoder();

        try (var pass = encoder.createRenderPass(
            () -> "Folium first triangle",
            colorTarget,
            Optional.of(new Vector4f(0.035f, 0.055f, 0.045f, 1.0f))
        )) {
            pass.setPipeline(pipeline);
            pass.draw(3, 1, 0, 0);
        }

        encoder.submit();
    }
}
