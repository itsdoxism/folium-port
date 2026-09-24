package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.device.DeviceFeatures;
import com.mojang.renderpearl.api.device.DeviceInfo;
import com.mojang.renderpearl.api.device.DeviceLimits;
import com.mojang.renderpearl.api.device.DeviceType;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.HintsAndWorkarounds;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Actual RenderPearl backend boundary for Folium.
 *
 * Mojang's FrontendGpuDevice remains intact and wraps this object, preserving
 * RenderPearl validation/resource semantics while Folium replaces only the
 * native GPU implementation.
 */
public final class FoliumBackendDevice implements GpuDeviceBackend {
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

    public FoliumBackendDevice(GpuDebugOptions debugOptions) {
        this.debugOptions = debugOptions;
    }

    @Override
    public GpuSurfaceBackend createSurface(long window, BooleanSupplier shouldClose) {
        ensureOpen();
        return new FoliumBackendSurface(window, shouldClose);
    }

    @Override
    public CommandEncoderBackend createCommandEncoder() {
        ensureOpen();
        return new FoliumBackendCommandEncoder();
    }

    @Override
    public GpuSampler createSampler(
        AddressMode addressModeU,
        AddressMode addressModeV,
        FilterMode minFilter,
        FilterMode magFilter,
        int maxAnisotropy,
        OptionalDouble maxLod
    ) {
        ensureOpen();
        return new FoliumGpuSampler(
            addressModeU,
            addressModeV,
            minFilter,
            magFilter,
            maxAnisotropy,
            maxLod
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
        return new FoliumGpuTexture(
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
    public GpuTextureView createTextureView(
        GpuTexture texture,
        int baseMipLevel,
        int mipLevels
    ) {
        ensureOpen();
        return new FoliumGpuTextureView(texture, baseMipLevel, mipLevels);
    }

    @Override
    public GpuBuffer createBuffer(Supplier<String> label, int usage, long size) {
        ensureOpen();
        return new FoliumGpuBuffer(label, usage, size);
    }

    @Override
    public GpuBuffer createBuffer(
        Supplier<String> label,
        int usage,
        ByteBuffer initialData
    ) {
        ensureOpen();
        return new FoliumGpuBuffer(label, usage, initialData);
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
    public BackendRenderPipeline.Pending compilePipeline(
        BackendRenderPipeline.CreateInfo createInfo
    ) {
        ensureOpen();

        // The RenderPearl frontend has already converted Minecraft pipeline
        // metadata to backend CreateInfo by this point. Shader translation to
        // WGSL is the next Folium milestone.
        return BackendRenderPipeline.Pending.NULL;
    }

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public GpuQueryPool createTimestampQueryPool(int count) {
        ensureOpen();
        return new FoliumGpuQueryPool(count);
    }

    @Override
    public long getTimestampCalibrationOffset() {
        return 0L;
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
            throw new IllegalStateException("FoliumBackendDevice is closed");
        }
    }
}
