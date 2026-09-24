package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.folium.platform.GraphicsHost;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class FoliumGpuDevice implements GpuDevice {
    private final GraphicsHost graphics;
    private boolean closed;

    public FoliumGpuDevice(GraphicsHost graphics) {
        if (!graphics.isDeviceReady()) {
            throw new IllegalStateException("WebGPU device is not ready");
        }
        this.graphics = graphics;
    }

    @Override
    public GpuSurface createSurface(long windowHandle, BooleanSupplier vsync) {
        ensureOpen();
        return new FoliumGpuSurface(graphics, windowHandle, vsync);
    }

    @Override
    public CommandEncoder createCommandEncoder() {
        ensureOpen();
        return new FoliumCommandEncoder(graphics);
    }

    @Override
    public GpuSampler createSampler(
        AddressMode addressModeU,
        AddressMode addressModeV,
        FilterMode minFilter,
        FilterMode magFilter,
        int maxAnisotropy,
        OptionalDouble lodBias
    ) {
        ensureOpen();
        throw unsupported("createSampler");
    }

    @Override
    public GpuTexture createTexture(
        Supplier<String> label,
        int usage,
        GpuFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    ) {
        return createTexture(
            label == null ? "Folium texture" : label.get(),
            usage,
            format,
            width,
            height,
            depthOrLayers,
            mipLevels
        );
    }

    @Override
    public GpuTexture createTexture(
        String label,
        int usage,
        GpuFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    ) {
        ensureOpen();

        if (width <= 0 || height <= 0 || depthOrLayers <= 0 || mipLevels <= 0) {
            throw new IllegalArgumentException("Invalid Folium texture dimensions/mip count");
        }

        int token = graphics.createTexture(
            label,
            usage,
            format.name(),
            width,
            height,
            depthOrLayers,
            mipLevels
        );

        return new FoliumGpuTexture(
            graphics,
            token,
            label,
            usage,
            format,
            width,
            height,
            depthOrLayers,
            mipLevels
        );
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        ensureOpen();
        throw unsupported("createTextureView(texture)");
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        ensureOpen();
        throw unsupported("createTextureView(texture, mip)");
    }

    @Override
    public GpuBuffer createBuffer(Supplier<String> label, int usage, long size) {
        ensureOpen();
        throw unsupported("createBuffer(size)");
    }

    @Override
    public GpuBuffer createBuffer(Supplier<String> label, int usage, ByteBuffer initialData) {
        ensureOpen();
        throw unsupported("createBuffer(data)");
    }

    @Override
    public List<String> getLastDebugMessages() {
        return List.of();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public CompletableFuture<CompiledRenderPipeline.Pending> compilePipeline(
        RenderPipeline pipeline,
        ShaderSource shaderSource,
        Executor executor
    ) {
        ensureOpen();
        return CompletableFuture.failedFuture(unsupported("compilePipeline"));
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public GpuQueryPool createTimestampQueryPool(int capacity) {
        ensureOpen();
        throw unsupported("createTimestampQueryPool");
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        ensureOpen();
        throw unsupported("getDeviceInfo");
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Folium WebGPU device is closed");
        }
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
            "Folium WebGPU: " + operation + " is not implemented yet"
        );
    }
}
