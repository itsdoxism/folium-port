package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public final class FoliumIndexedTriangleProbe {
    private FoliumIndexedTriangleProbe() {
    }

    public static void draw(
        GpuDevice device,
        GpuTextureView colorTarget,
        FoliumCompiledRenderPipeline pipeline
    ) {
        ByteBuffer indexData = ByteBuffer.allocateDirect(6)
            .order(ByteOrder.LITTLE_ENDIAN);

        indexData.putShort((short) 0);
        indexData.putShort((short) 1);
        indexData.putShort((short) 2);
        indexData.flip();

        try (GpuBuffer indices = device.createBuffer(
            () -> "Folium Java ByteBuffer triangle indices",
            GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
            indexData
        )) {
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
