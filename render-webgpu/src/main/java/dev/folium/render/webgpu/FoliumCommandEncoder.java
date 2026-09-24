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
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.folium.platform.GraphicsHost;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

public final class FoliumCommandEncoder implements CommandEncoder {
    private final GraphicsHost graphics;
    private final int token;
    private boolean submitted;

    FoliumCommandEncoder(GraphicsHost graphics) {
        this.graphics = graphics;
        this.token = graphics.createCommandEncoder();
    }

    @Override
    public void submit() {
        ensureActive();
        graphics.submitCommandEncoder(token);
        submitted = true;
    }

    @Override
    public TransientMemory transientMemory() {
        throw unsupported("transientMemory");
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> label,
        GpuTextureView colorAttachment,
        Optional<Vector4fc> clearColor
    ) {
        throw unsupported("createRenderPass(color)");
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> label,
        GpuTextureView colorAttachment,
        Optional<Vector4fc> clearColor,
        GpuTextureView depthAttachment,
        OptionalDouble clearDepth
    ) {
        throw unsupported("createRenderPass(color, depth)");
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> label,
        GpuTextureView colorAttachment,
        Optional<Vector4fc> clearColor,
        GpuTextureView depthAttachment,
        OptionalDouble clearDepth,
        RenderPass.RenderArea renderArea
    ) {
        throw unsupported("createRenderPass(area)");
    }

    @Override
    public RenderPass createRenderPass(RenderPassDescriptor descriptor) {
        throw unsupported("createRenderPass(descriptor)");
    }

    @Override
    public void clearColorTexture(GpuTexture texture, Vector4fc color) {
        ensureActive();
        FoliumGpuTexture foliumTexture = requireTexture(texture);
        graphics.clearColorTexture(
            token,
            foliumTexture.token(),
            color.x(),
            color.y(),
            color.z(),
            color.w()
        );
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture,
        Vector4fc color,
        GpuTexture depthTexture,
        double depth
    ) {
        throw unsupported("clearColorAndDepthTextures");
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
        throw unsupported("clearColorAndDepthTextures(region)");
    }

    @Override
    public void clearDepthTexture(GpuTexture texture, double depth) {
        throw unsupported("clearDepthTexture");
    }

    @Override
    public void writeToBuffer(GpuBufferSlice slice, ByteBuffer data) {
        throw unsupported("writeToBuffer");
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice destination) {
        throw unsupported("copyToBuffer");
    }

    @Override
    public void writeToTexture(GpuTexture texture, NativeImage image) {
        throw unsupported("writeToTexture(image)");
    }

    @Override
    public void writeToTexture(
        GpuTexture texture,
        NativeImage image,
        int mipLevel,
        int x,
        int y,
        int layer
    ) {
        throw unsupported("writeToTexture(image, region)");
    }

    @Override
    public void writeToTexture(
        GpuTexture texture,
        ByteBuffer data,
        int mipLevel,
        int x,
        int y,
        int width,
        int height
    ) {
        throw unsupported("writeToTexture(buffer)");
    }

    @Override
    public void copyBufferToTexture(
        GpuBufferSlice source,
        int bytesPerRow,
        int rowsPerImage,
        int sourceOffset,
        int sourceLayer,
        GpuTexture destination,
        int mipLevel,
        int x,
        int y,
        int width,
        int height,
        int destinationLayer
    ) {
        throw unsupported("copyBufferToTexture");
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture source,
        GpuBuffer destination,
        long destinationOffset,
        Runnable callback,
        int mipLevel
    ) {
        throw unsupported("copyTextureToBuffer");
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture source,
        GpuBuffer destination,
        long destinationOffset,
        Runnable callback,
        int mipLevel,
        int x,
        int y,
        int width,
        int height
    ) {
        throw unsupported("copyTextureToBuffer(region)");
    }

    @Override
    public void copyTextureToTexture(
        GpuTexture source,
        GpuTexture destination,
        int sourceX,
        int sourceY,
        int destinationX,
        int destinationY,
        int width,
        int height,
        int layer
    ) {
        throw unsupported("copyTextureToTexture");
    }

    @Override
    public GpuFence createFence() {
        throw unsupported("createFence");
    }

    @Override
    public void writeTimestamp(GpuQueryPool queryPool, int queryIndex) {
        throw unsupported("writeTimestamp");
    }

    private void ensureActive() {
        if (submitted) {
            throw new IllegalStateException("Folium command encoder already submitted");
        }
    }

    private static FoliumGpuTexture requireTexture(GpuTexture texture) {
        if (!(texture instanceof FoliumGpuTexture foliumTexture)) {
            throw new IllegalArgumentException(
                "Folium command encoder received a non-Folium texture"
            );
        }
        return foliumTexture;
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
            "Folium WebGPU: " + operation + " is not implemented yet"
        );
    }
}
