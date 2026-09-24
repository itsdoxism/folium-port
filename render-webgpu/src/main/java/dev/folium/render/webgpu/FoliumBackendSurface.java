package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.GpuSurfaceBackend;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Browser swapchain boundary. FrontendGpuSurface owns the public acquisition
 * state; this backend will eventually map configure/acquire/present to
 * GPUCanvasContext.
 */
public final class FoliumBackendSurface implements GpuSurfaceBackend {
    private final long windowToken;
    private final BooleanSupplier shouldClose;
    private GpuSurface.Configuration configuration;
    private boolean closed;

    public FoliumBackendSurface(long windowToken, BooleanSupplier shouldClose) {
        this.windowToken = windowToken;
        this.shouldClose = shouldClose;
    }

    @Override
    public void configure(GpuSurface.Configuration configuration) {
        ensureOpen();
        this.configuration = configuration;
    }

    @Override
    public boolean isSuboptimal() {
        return false;
    }

    @Override
    public void acquireNextTexture() {
        ensureOpen();

        if (configuration == null) {
            throw new IllegalStateException(
                "Folium surface must be configured before acquire"
            );
        }

        if (shouldClose.getAsBoolean()) {
            throw new IllegalStateException("Folium browser surface requested close");
        }
    }

    @Override
    public void blitFromTexture(
        CommandEncoderBackend encoder,
        GpuTextureView textureView
    ) {
        ensureOpen();
        // WebGPU canvas blit/present bridge is implemented after pipelines.
    }

    @Override
    public void present() {
        ensureOpen();
        // GPUCanvasContext presents the current texture implicitly per frame.
    }

    @Override
    public Collection<GpuSurface.PresentMode> supportedPresentModes() {
        return List.of(GpuSurface.PresentMode.FIFO);
    }

    @Override
    public void close() {
        closed = true;
    }

    public long windowToken() {
        return windowToken;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("FoliumBackendSurface is closed");
        }
    }
}
