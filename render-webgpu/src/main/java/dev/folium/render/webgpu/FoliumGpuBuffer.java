package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

/**
 * Minimal CPU-backed RenderPearl buffer.
 *
 * This is deliberately not the final WebGPU implementation. It lets Minecraft
 * construct its early dynamic UBO ring buffers while Folium brings the host
 * WebGPU buffer bridge online incrementally.
 */
public final class FoliumGpuBuffer implements GpuBuffer {
    private final Supplier<String> label;
    private final int usage;
    private final long size;
    private ByteBuffer storage;
    private boolean closed;

    public FoliumGpuBuffer(Supplier<String> label, int usage, long size) {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Unsupported Folium buffer size: " + size);
        }

        this.label = label;
        this.usage = usage;
        this.size = size;
        this.storage = ByteBuffer.allocate((int) size);
    }

    public FoliumGpuBuffer(Supplier<String> label, int usage, ByteBuffer initialData) {
        this(label, usage, initialData.remaining());

        ByteBuffer source = initialData.duplicate();
        ByteBuffer destination = storage.duplicate();
        destination.put(source);
        destination.flip();
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
        ensureOpen();

        if (offset < 0 || length < 0 || offset + length > size) {
            throw new IndexOutOfBoundsException(
                "Folium buffer map outside range: offset=" + offset
                    + ", length=" + length + ", size=" + size
            );
        }

        ByteBuffer view = storage.duplicate();
        view.position(Math.toIntExact(offset));
        view.limit(Math.toIntExact(offset + length));
        ByteBuffer slice = view.slice();

        GpuBufferSlice bufferSlice = new GpuBufferSlice(this, offset, length);
        return new GpuBufferSlice.MappedView(bufferSlice, slice, () -> {
            // CPU-backed bootstrap buffer: writes are already visible.
            // The WebGPU implementation will upload dirty ranges here.
        });
    }

    @Override
    public void close() {
        closed = true;
        storage = null;
    }

    public String debugLabel() {
        return label == null ? "" : label.get();
    }

    private void ensureOpen() {
        if (closed || storage == null) {
            throw new IllegalStateException("FoliumGpuBuffer is closed");
        }
    }
}
