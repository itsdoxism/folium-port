package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Minimal browser surface implementation.
 *
 * The swapchain itself still lives in the browser host. This class currently
 * models RenderPearl's surface lifecycle so Minecraft can progress farther
 * before a real texture bridge is required.
 */
public final class FoliumGpuSurface implements GpuSurface {
    private final long windowToken;
    private final BooleanSupplier shouldClose;

    private Configuration configuration;
    private boolean acquired;
    private boolean closed;

    public FoliumGpuSurface(long windowToken, BooleanSupplier shouldClose) {
        this.windowToken = windowToken;
        this.shouldClose = shouldClose;
    }

    @Override
    public void configure(Configuration configuration) {
        ensureOpen();
        this.configuration = configuration;
    }

    @Override
    public Optional currentConfiguration() {
        return Optional.ofNullable(configuration);
    }

    @Override
    public Collection supportedPresentModes() {
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

        if (shouldClose.getAsBoolean()) {
            throw new IllegalStateException("Folium browser surface requested close");
        }

        if (configuration == null) {
            throw new IllegalStateException("FoliumGpuSurface must be configured before acquire");
        }

        acquired = true;
    }

    @Override
    public void blitFromTexture(CommandEncoder encoder, GpuTextureView textureView) {
        ensureOpen();
        throw new UnsupportedOperationException(
            "Folium surface blit bridge is not implemented yet"
        );
    }

    @Override
    public void present() {
        ensureOpen();

        if (!acquired) {
            throw new IllegalStateException("No Folium surface texture is acquired");
        }

        acquired = false;
    }

    @Override
    public void close() {
        closed = true;
        acquired = false;
    }

    public long windowToken() {
        return windowToken;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("FoliumGpuSurface is closed");
        }
    }
}
