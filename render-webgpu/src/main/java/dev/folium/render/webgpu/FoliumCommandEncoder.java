package dev.folium.render.webgpu;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;

/**
 * CPU bootstrap implementation of RenderPearl's command encoder.
 *
 * Resource writes that can be represented in Java are performed immediately.
 * Draw/render-pass operations remain explicit blockers until the WebGPU command
 * bridge is implemented.
 */
public final class FoliumCommandEncoder implements CommandEncoder {
    @Override
    public void submit() {
        // CPU bootstrap operations are immediate.
    }

    @Override
    public TransientMemory transientMemory() {
        throw unsupported("transientMemory");
    }

    @Override
    public RenderPass createRenderPass(RenderPassDescriptor descriptor) {
        throw unsupported("createRenderPass");
    }

    @Override
    public void clearColorTexture(GpuTexture texture, Vector4fc color) {
        // Metadata-only textures have no CPU pixel backing yet.
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture,
        Vector4fc color,
        GpuTexture depthTexture,
        double depth
    ) {
        // Deferred to the WebGPU resource bridge.
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
        // Deferred to the WebGPU resource bridge.
    }

    @Override
    public void clearDepthTexture(GpuTexture texture, double depth) {
        // Deferred to the WebGPU resource bridge.
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
    public void writeToTexture(GpuTexture texture, NativeImage image) {
        // Texture upload bridge comes next.
    }

    @Override
    public void writeToTexture(
        GpuTexture texture,
        NativeImage image,
        int mipLevel,
        int x,
        int y,
        int width
    ) {
        // Texture upload bridge comes next.
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
        // Texture upload bridge comes next.
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
        // WebGPU copy bridge comes later.
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
        // WebGPU copy bridge comes later.
    }

    @Override
    public GpuFence createFence() {
        return new FoliumGpuFence();
    }

    @Override
    public void writeTimestamp(GpuQueryPool queryPool, int index) {
        // Timestamp queries are not enabled in the bootstrap backend.
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
            "Folium command encoder has not implemented " + operation + " yet"
        );
    }
}
