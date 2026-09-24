package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.folium.platform.GraphicsHost;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Minimal browser surface state.
 *
 * The actual WebGPU canvas context lives on the browser host. This Java object
 * mirrors RenderPearl lifecycle/configuration until texture acquisition is
 * implemented.
 */
public final class FoliumGpuSurface implements GpuSurface {
    private final GraphicsHost graphics;
    private final long windowHandle;
    private final BooleanSupplier vsync;

    private Configuration configuration;
    private boolean acquired;
    private boolean closed;

    FoliumGpuSurface(GraphicsHost graphics, long windowHandle, BooleanSupplier vsync) {
        this.graphics = graphics;
        this.windowHandle = windowHandle;
        this.vsync = vsync;
    }

    @Override
    public void configure(Configuration configuration) {
        ensureOpen();
        this.configuration = configuration;
    }

    @Override
    public Optional<Configuration> currentConfiguration() {
        return Optional.ofNullable(configuration);
    }

    @Override
    public Collection<PresentMode> supportedPresentModes() {
        // WebGPU canvas presentation is browser-controlled. FIFO is the
        // conservative RenderPearl-facing mode for the first implementation.
        return List.of(PresentMode.FIFO);
    }

    @Override
    public boolean isSuboptimal() {
        return false;
    }

    @Override
    public boolean isAcquired() {
        return acquired;
    }

    @Override
    public void acquireNextTexture() {
        ensureOpen();
        ensureConfigured();

        if (acquired) {
            throw new IllegalStateException("Folium surface texture already acquired");
        }

        if (!graphics.isDeviceReady()) {
            throw new IllegalStateException("Folium WebGPU device is no longer ready");
        }

        acquired = true;
    }

    @Override
    public void blitFromTexture(CommandEncoder encoder, GpuTextureView textureView) {
        ensureOpen();
        ensureConfigured();
        throw new UnsupportedOperationException(
            "Folium WebGPU: surface blit is not implemented yet"
        );
    }

    @Override
    public void present() {
        ensureOpen();
        ensureConfigured();

        if (!acquired) {
            throw new IllegalStateException("Folium surface present without acquire");
        }

        // Browser WebGPU presentation happens when commands touching the
        // current canvas texture are submitted. For now this method only
        // mirrors the RenderPearl lifecycle.
        acquired = false;
    }

    @Override
    public void close() {
        closed = true;
        acquired = false;
        configuration = null;
    }

    public long windowHandle() {
        return windowHandle;
    }

    public boolean requestedVsync() {
        return vsync.getAsBoolean();
    }

    private void ensureConfigured() {
        if (configuration == null) {
            throw new IllegalStateException("Folium surface is not configured");
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Folium surface is closed");
        }
    }
}
