package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.textures.GpuTexture;

/**
 * Metadata-first texture used while Folium wires RenderPearl resources to
 * browser WebGPU objects.
 */
public final class FoliumGpuTexture implements GpuTexture {
    private final String label;
    private final int usage;
    private final GpuFormat format;
    private final int width;
    private final int height;
    private final int depthOrLayers;
    private final int mipLevels;
    private boolean closed;

    public FoliumGpuTexture(
        String label,
        int usage,
        GpuFormat format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    ) {
        this.label = label;
        this.usage = usage;
        this.format = format;
        this.width = width;
        this.height = height;
        this.depthOrLayers = depthOrLayers;
        this.mipLevels = mipLevels;
    }

    @Override
    public int getWidth(int mipLevel) {
        return mipDimension(width, mipLevel);
    }

    @Override
    public int getHeight(int mipLevel) {
        return mipDimension(height, mipLevel);
    }

    @Override
    public int getDepthOrLayers() {
        return depthOrLayers;
    }

    @Override
    public int getMipLevels() {
        return mipLevels;
    }

    @Override
    public GpuFormat getFormat() {
        return format;
    }

    @Override
    public int usage() {
        return usage;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
    }

    private static int mipDimension(int value, int mipLevel) {
        if (mipLevel < 0) {
            throw new IllegalArgumentException("Negative mip level");
        }
        return Math.max(1, value >> Math.min(mipLevel, 30));
    }
}
