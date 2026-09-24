package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormatElement;
import dev.folium.platform.GraphicsHost;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;

/**
 * First proof that Java-owned interleaved vertex bytes are interpreted through
 * a RenderPearl VertexFormat translated to a WebGPU vertex-buffer layout.
 */
public final class FoliumVertexBufferTriangleProbe {
    private FoliumVertexBufferTriangleProbe() {
    }

    public static void draw(
        GpuDevice device,
        GraphicsHost graphics,
        GpuTextureView colorTarget,
        String colorFormat
    ) {
        VertexFormat format = new VertexFormat(
            List.of(
                new VertexFormatElement("Position", 0, GpuFormat.RGB32_FLOAT),
                new VertexFormatElement("Color", 12, GpuFormat.RGBA8_UNORM)
            ),
            16,
            0
        );

        FoliumVertexLayout layout = FoliumVertexFormatMapper.map(format);
        String layoutJson = FoliumVertexLayoutJson.encode(layout);

        try (
            FoliumCompiledRenderPipeline pipeline =
                FoliumCompiledRenderPipeline.vertexColor(graphics, layoutJson, colorFormat);
            GpuBuffer vertices = device.createBuffer(
                () -> "Folium Java vertex triangle",
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                createVertexData()
            )
        ) {
            var encoder = device.createCommandEncoder();

            try (var pass = encoder.createRenderPass(
                () -> "Folium vertex-buffer triangle",
                colorTarget,
                Optional.of(new Vector4f(0.035f, 0.055f, 0.045f, 1.0f))
            )) {
                pass.setPipeline(pipeline);
                pass.setVertexBuffer(0, vertices.slice());
                pass.draw(3, 1, 0, 0);
            }

            encoder.submit();
        }
    }

    private static ByteBuffer createVertexData() {
        ByteBuffer data = ByteBuffer.allocateDirect(16 * 3)
            .order(ByteOrder.LITTLE_ENDIAN);

        putVertex(data, 0.0f, 0.62f, 0.0f, 115, 242, 168, 255);
        putVertex(data, -0.58f, -0.46f, 0.0f, 64, 148, 250, 255);
        putVertex(data, 0.58f, -0.46f, 0.0f, 235, 107, 163, 255);

        data.flip();
        return data;
    }

    private static void putVertex(
        ByteBuffer data,
        float x,
        float y,
        float z,
        int r,
        int g,
        int b,
        int a
    ) {
        data.putFloat(x);
        data.putFloat(y);
        data.putFloat(z);
        data.put((byte) r);
        data.put((byte) g);
        data.put((byte) b);
        data.put((byte) a);
    }
}
