package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.folium.platform.GraphicsHost;

public final class FoliumGpuTextureView implements GpuTextureView {
    private final GraphicsHost graphics;
    private final FoliumGpuTexture texture;
    private final int token;
    private final int baseMipLevel;
    private final int mipLevels;

    private boolean closed;

    FoliumGpuTextureView(
        GraphicsHost graphics,
        FoliumGpuTexture texture,
        int token,
        int baseMipLevel,
        int mipLevels
    ) {
        this.graphics = graphics;
        this.texture = texture;
        this.token = token;
        this.baseMipLevel = baseMipLevel;
        this.mipLevels = mipLevels;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public GpuTexture texture() {
        return texture;
    }

    @Override
    public int baseMipLevel() {
        return baseMipLevel;
    }

    @Override
    public int mipLevels() {
        return mipLevels;
    }

    @Override
    public int getWidth(int mipLevel) {
        validateRelativeMip(mipLevel);
        return texture.getWidth(baseMipLevel + mipLevel);
    }

    @Override
    public int getHeight(int mipLevel) {
        validateRelativeMip(mipLevel);
        return texture.getHeight(baseMipLevel + mipLevel);
    }

    @Override
    public void close() {
        if (!closed) {
            graphics.releaseTextureView(token);
            closed = true;
        }
    }

    int token() {
        if (closed) {
            throw new IllegalStateException("Folium texture view is closed");
        }
        return token;
    }

    private void validateRelativeMip(int mipLevel) {
        if (mipLevel < 0 || mipLevel >= mipLevels) {
            throw new IllegalArgumentException("Mip level outside Folium texture view");
        }
    }
}
