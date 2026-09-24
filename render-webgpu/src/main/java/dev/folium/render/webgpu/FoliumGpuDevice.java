package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.DeviceFeatures;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.DeviceLimits;
import com.mojang.renderpearl.api.device.DeviceType;
import com.mojang.renderpearl.api.device.HintsAndWorkarounds;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * First contract-complete shell for RenderPearl's public GpuDevice API.
 *
 * Only surface creation is functional at this stage. Unsupported methods fail
 * loudly so Minecraft reachability tests reveal the next required primitive.
 */
public final class FoliumGpuDevice implements GpuDevice {
    private static final DeviceInfo FALLBACK_DEVICE_INFO = new DeviceInfo(
        "Folium WebGPU Adapter",
        "Browser",
        "WebGPU",
        true,
        "Folium WebGPU",
        1.0F,
        new DeviceLimits(
            1,
            256,
            8192,
            256L * 1024L * 1024L,
            1,
            8,
            1
        ),
        new DeviceFeatures(
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            false
        ),
        Collections.emptySet(),
        new HintsAndWorkarounds(
            false,
            false,
            false,
            true
        ),
        DeviceType.OTHER
    );

    private final GpuDebugOptions debugOptions;
    private boolean closed;

    public FoliumGpuDevice(GpuDebugOptions debugOptions) {
        this.debugOptions = debugOptions;
    }

    @Override
    public GpuSurface createSurface(long window, BooleanSupplier shouldClose) {
        ensureOpen();
        return new FoliumGpuSurface(window, shouldClose);
    }

    @Override
    public CommandEncoder createCommandEncoder() {
        ensureOpen();
        throw unsupported("createCommandEncoder");
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
        Supplier label,
        int usage,
        GpuFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    ) {
        ensureOpen();
        throw unsupported("createTexture");
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
        throw unsupported("createTexture");
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture) {
        ensureOpen();
        throw unsupported("createTextureView");
    }

    @Override
    public GpuTextureView createTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        ensureOpen();
        throw unsupported("createTextureView");
    }

    @Override
    public GpuBuffer createBuffer(Supplier label, int usage, long size) {
        ensureOpen();
        throw unsupported("createBuffer");
    }

    @Override
    public GpuBuffer createBuffer(Supplier label, int usage, ByteBuffer initialData) {
        ensureOpen();
        throw unsupported("createBuffer");
    }

    @Override
    public List getLastDebugMessages() {
        return Collections.emptyList();
    }

    @Override
    public boolean isDebuggingEnabled() {
        return false;
    }

    @Override
    public CompletableFuture compilePipeline(
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
    public GpuQueryPool createTimestampQueryPool(int count) {
        ensureOpen();
        throw unsupported("createTimestampQueryPool");
    }

    @Override
    public DeviceInfo getDeviceInfo() {
        ensureOpen();
        return FALLBACK_DEVICE_INFO;
    }

    public GpuDebugOptions debugOptions() {
        return debugOptions;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("FoliumGpuDevice is closed");
        }
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
            "Folium WebGPU backend has not implemented " + operation + " yet"
        );
    }
}
