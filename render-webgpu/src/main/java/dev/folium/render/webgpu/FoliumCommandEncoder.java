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
        GpuTextureView colorTexture,
        Optional<Vector4fc> clearColor
    ) {
        ensureActive();
        FoliumGpuTextureView view = requireTextureView(colorTexture);

        Vector4fc color = clearColor.orElse(null);
        int passToken = graphics.beginColorRenderPass(
            token,
            view.token(),
            color != null,
            color == null ? 0.0f : color.x(),
            color == null ? 0.0f : color.y(),
            color == null ? 0.0f : color.z(),
            color == null ? 0.0f : color.w()
        );

        FoliumRenderPass pass = new FoliumRenderPass(
            graphics,
            passToken,
            view.getWidth(0),
            view.getHeight(0)
        );

        if (label != null) {
            pass.pushDebugGroup(label);
        }

        return pass;
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> label,
        GpuTextureView colorTexture,
        Optional<Vector4fc> clearColor,
        GpuTextureView depthTexture,
        OptionalDouble clearDepth
    ) {
        throw unsupported("createRenderPass(color, depth)");
    }

    @Override
    public RenderPass createRenderPass(
        Supplier<String> label,
        GpuTextureView colorTexture,
        Optional<Vector4fc> clearColor,
        GpuTextureView depthTexture,
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
    public void clearColorTexture(GpuTexture colorTexture, Vector4fc clearColor) {
        ensureActive();
        FoliumGpuTexture texture = requireTexture(colorTexture);
        graphics.clearColorTexture(
            token,
            texture.token(),
            clearColor.x(),
            clearColor.y(),
            clearColor.z(),
            clearColor.w()
        );
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture,
        Vector4fc clearColor,
        GpuTexture depthTexture,
        double clearDepth
    ) {
        throw unsupported("clearColorAndDepthTextures");
    }

    @Override
    public void clearColorAndDepthTextures(
        GpuTexture colorTexture,
        Vector4fc clearColor,
        GpuTexture depthTexture,
        double clearDepth,
        int regionX,
        int regionY,
        int regionWidth,
        int regionHeight,
        int mipLevel
    ) {
        throw unsupported("clearColorAndDepthTextures(region)");
    }

    @Override
    public void clearDepthTexture(GpuTexture depthTexture, double clearDepth) {
        throw unsupported("clearDepthTexture");
    }

    @Override
    public void writeToBuffer(GpuBufferSlice destination, ByteBuffer data) {
        throw unsupported("writeToBuffer");
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        throw unsupported("copyToBuffer");
    }

    @Override
    public void writeToTexture(GpuTexture destination, NativeImage source) {
        throw unsupported("writeToTexture(image)");
    }

    @Override
    public void writeToTexture(
        GpuTexture destination,
        NativeImage source,
        int mipLevel,
        int depthOrLayer,
        int destX,
        int destY
    ) {
        throw unsupported("writeToTexture(image, region)");
    }

    @Override
    public void writeToTexture(
        GpuTexture destination,
        ByteBuffer source,
        int mipLevel,
        int depthOrLayer,
        int destX,
        int destY,
        int width,
        int height
    ) {
        throw unsupported("writeToTexture(buffer)");
    }

    @Override
    public void copyBufferToTexture(
        GpuBufferSlice source,
        int sourceX,
        int sourceY,
        int sourceWidth,
        int sourceHeight,
        GpuTexture destination,
        int destinationX,
        int destinationY,
        int copyWidth,
        int copyHeight,
        int mipLevel,
        int arrayLayer
    ) {
        throw unsupported("copyBufferToTexture");
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture source,
        GpuBuffer destination,
        long offset,
        Runnable callback,
        int mipLevel
    ) {
        throw unsupported("copyTextureToBuffer");
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture source,
        GpuBuffer destination,
        long offset,
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
        int mipLevel,
        int destX,
        int destY,
        int sourceX,
        int sourceY,
        int width,
        int height
    ) {
        throw unsupported("copyTextureToTexture");
    }

    @Override
    public GpuFence createFence() {
        throw unsupported("createFence");
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {
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

    private static FoliumGpuTextureView requireTextureView(GpuTextureView view) {
        if (!(view instanceof FoliumGpuTextureView foliumView)) {
            throw new IllegalArgumentException(
                "Folium command encoder received a non-Folium texture view"
            );
        }
        return foliumView;
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
            "Folium WebGPU: " + operation + " is not implemented yet"
        );
    }
}
