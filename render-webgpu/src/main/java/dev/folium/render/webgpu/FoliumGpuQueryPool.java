package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.commands.GpuQueryPool;

import java.util.OptionalLong;

public final class FoliumGpuQueryPool implements GpuQueryPool {
    private final int size;
    private boolean closed;

    public FoliumGpuQueryPool(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("Negative query-pool size");
        }
        this.size = size;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public OptionalLong getValue(int index) {
        checkIndex(index);
        return OptionalLong.empty();
    }

    @Override
    public OptionalLong[] getValues(int start, int count) {
        if (start < 0 || count < 0 || start + count > size) {
            throw new IndexOutOfBoundsException();
        }

        OptionalLong[] values = new OptionalLong[count];
        for (int i = 0; i < count; i++) {
            values[i] = OptionalLong.empty();
        }
        return values;
    }

    @Override
    public void close() {
        closed = true;
    }

    private void checkIndex(int index) {
        if (closed) {
            throw new IllegalStateException("FoliumGpuQueryPool is closed");
        }
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException(index);
        }
    }
}
