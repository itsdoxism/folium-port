package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;

public final class FoliumGpuTextureView implements GpuTextureView {
    private final GpuTexture texture;
    private final int baseMipLevel;
    private final int mipLevels;
    private boolean closed;

    public FoliumGpuTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        this.texture = texture;
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
        return texture.getWidth(baseMipLevel + mipLevel);
    }

    @Override
    public int getHeight(int mipLevel) {
        return texture.getHeight(baseMipLevel + mipLevel);
    }

    @Override
    public void close() {
        closed = true;
    }
}
