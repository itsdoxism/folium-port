package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import dev.folium.platform.GraphicsHost;

public final class FoliumGpuBuffer implements GpuBuffer {
    private final GraphicsHost graphics;
    private final int token;
    private final long size;
    private final int usage;
    private boolean closed;

    FoliumGpuBuffer(GraphicsHost graphics, int token, long size, int usage) {
        this.graphics = graphics;
        this.token = token;
        this.size = size;
        this.usage = usage;
    }

    static FoliumGpuBuffer bootstrapTriangleIndexBuffer(GraphicsHost graphics) {
        return new FoliumGpuBuffer(
            graphics,
            graphics.createBootstrapTriangleIndexBuffer(),
            6L,
            USAGE_INDEX | USAGE_COPY_DST
        );
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public int usage() {
        return usage;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public GpuBufferSlice.MappedView map(
        long offset,
        long length,
        boolean read,
        boolean write
    ) {
        throw new UnsupportedOperationException(
            "Folium WebGPU: mapped buffers are not implemented yet"
        );
    }

    @Override
    public void close() {
        if (!closed) {
            graphics.destroyBuffer(token);
            closed = true;
        }
    }

    int token() {
        if (closed) {
            throw new IllegalStateException("Folium buffer is closed");
        }
        return token;
    }
}
