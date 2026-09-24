package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;

/**
 * Backend command stream used by RenderPearl's FrontendCommandEncoder.
 *
 * CPU-visible buffer writes work today; actual WebGPU render/copy commands are
 * progressively attached here.
 */
public final class FoliumBackendCommandEncoder implements CommandEncoderBackend {
    @Override
    public void submit() {
        // CPU bootstrap commands are immediate.
    }

    @Override
    public TransientMemory transientMemory() {
        throw unsupported("transientMemory");
    }

    @Override
    public RenderPassBackend createRenderPass(RenderPassDescriptor descriptor) {
        throw unsupported("createRenderPass");
    }

    @Override
    public void submitRenderPass() {
        // No render pass can currently be created.
    }

    @Override
    public void clearColorTexture(GpuTexture texture, Vector4fc color) {
        // Deferred until textures have browser GPU handles.
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture,
        Vector4fc color,
        GpuTexture depthTexture,
        double depth
    ) {
        // Deferred until textures have browser GPU handles.
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture,
        Vector4fc color,
        GpuTexture depthTexture,
        double depth,
        int x,
        int y,
        int width,
        int height,
        int layer
    ) {
        // Deferred until textures have browser GPU handles.
    }

    @Override
    public void clearDepthTexture(GpuTexture texture, double depth) {
        // Deferred until textures have browser GPU handles.
    }

    @Override
    public void writeToBuffer(GpuBufferSlice target, ByteBuffer source) {
        ByteBuffer src = source.duplicate();

        try (GpuBufferSlice.MappedView mapped = target.map(false, true)) {
            ByteBuffer dst = mapped.data();
            if (src.remaining() > dst.remaining()) {
                throw new IllegalArgumentException(
                    "Source data exceeds Folium buffer slice"
                );
            }
            dst.put(src);
        }
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        try (
            GpuBufferSlice.MappedView srcMap = source.map(true, false);
            GpuBufferSlice.MappedView dstMap = target.map(false, true)
        ) {
            ByteBuffer src = srcMap.data();
            ByteBuffer dst = dstMap.data();

            if (src.remaining() > dst.remaining()) {
                throw new IllegalArgumentException(
                    "Source Folium buffer slice exceeds destination"
                );
            }

            dst.put(src);
        }
    }

    @Override
    public void writeToTexture(
        GpuTexture texture,
        ByteBuffer source,
        int mipLevel,
        int x,
        int y,
        int width,
        int height,
        int rowPitch
    ) {
        // Browser texture upload bridge comes next.
    }

    @Override
    public void copyBufferToTexture(
        GpuBufferSlice source,
        int sourceOffset,
        int bytesPerRow,
        int rowsPerImage,
        int layer,
        GpuTexture target,
        int mipLevel,
        int x,
        int y,
        int width,
        int height,
        int depth
    ) {
        // Browser copy bridge comes later.
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture source,
        GpuBuffer target,
        long targetOffset,
        Runnable completion,
        int bytesPerRow
    ) {
        if (completion != null) {
            completion.run();
        }
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture source,
        GpuBuffer target,
        long targetOffset,
        Runnable completion,
        int bytesPerRow,
        int mipLevel,
        int x,
        int y,
        int width
    ) {
        if (completion != null) {
            completion.run();
        }
    }

    @Override
    public void copyTextureToTexture(
        GpuTexture source,
        GpuTexture target,
        int sourceMipLevel,
        int targetMipLevel,
        int sourceX,
        int sourceY,
        int targetX,
        int targetY,
        int width
    ) {
        // Browser copy bridge comes later.
    }

    @Override
    public GpuFence createFence() {
        return new FoliumGpuFence();
    }

    @Override
    public void writeTimestamp(GpuQueryPool queryPool, int index) {
        // Timestamp queries are disabled in fallback DeviceFeatures.
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
            "Folium WebGPU backend has not implemented " + operation + " yet"
        );
    }
}
