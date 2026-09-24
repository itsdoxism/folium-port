package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * State-validating bootstrap render pass.
 *
 * Draws are intentionally discarded until the WebGPU command bridge is wired,
 * but Minecraft can bind pipelines, uniforms, buffers, scissors and issue draw
 * commands without falling back into desktop APIs.
 */
public final class FoliumBackendRenderPass implements RenderPassBackend {
    private final Map<Integer, Object> uniforms = new HashMap<>();
    private final GpuBufferSlice[] vertexBuffers = new GpuBufferSlice[16];

    private BackendRenderPipeline pipeline;
    private GpuBuffer indexBuffer;
    private IndexType indexType;
    private boolean scissorEnabled;
    private boolean closed;

    @Override
    public void pushDebugGroup(Supplier<String> label) {
        ensureOpen();
    }

    @Override
    public void popDebugGroup() {
        ensureOpen();
    }

    @Override
    public void setPipeline(BackendRenderPipeline pipeline) {
        ensureOpen();
        if (pipeline == null || pipeline.isClosed()) {
            throw new IllegalArgumentException("Folium cannot bind a null/closed pipeline");
        }
        this.pipeline = pipeline;
    }

    @Override
    public void setUniform(int index, Object value) {
        ensureOpen();
        uniforms.put(index, value);
    }

    @Override
    public void pushConstants(ByteBuffer values) {
        ensureOpen();
        // FrontendRenderPass already validates the declared push-constant size.
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        ensureOpen();
        scissorEnabled = true;
    }

    @Override
    public void disableScissor() {
        ensureOpen();
        scissorEnabled = false;
    }

    @Override
    public void setVertexBuffer(int slot, GpuBufferSlice buffer) {
        ensureOpen();
        if (slot < 0 || slot >= vertexBuffers.length) {
            throw new IndexOutOfBoundsException(slot);
        }
        vertexBuffers[slot] = buffer;
    }

    @Override
    public void setIndexBuffer(GpuBuffer buffer, IndexType type) {
        ensureOpen();
        this.indexBuffer = buffer;
        this.indexType = type;
    }

    @Override
    public void drawIndexed(
        int indexCount,
        int instanceCount,
        int firstIndex,
        int baseVertex,
        int firstInstance
    ) {
        validateDraw(true);
    }

    @Override
    public void multiDrawIndexed(
        IntBuffer counts,
        int instanceCount,
        int firstIndex,
        int baseVertex
    ) {
        validateDraw(true);
    }

    @Override
    public void multiDrawIndexed(
        PointerBuffer offsets,
        IntBuffer counts,
        IntBuffer baseVertices,
        int instanceCount
    ) {
        validateDraw(true);
    }

    @Override
    public void drawIndexedIndirect(GpuBufferSlice commands, int drawCount) {
        validateDraw(true);
    }

    @Override
    public void draw(int vertexCount, int instanceCount, int firstVertex, int firstInstance) {
        validateDraw(false);
    }

    @Override
    public void multiDraw(
        IntBuffer counts,
        int instanceCount,
        int firstVertex,
        int firstInstance
    ) {
        validateDraw(false);
    }

    @Override
    public void multiDraw(IntBuffer firstVertices, IntBuffer counts, int instanceCount) {
        validateDraw(false);
    }

    @Override
    public void drawIndirect(GpuBufferSlice commands, int drawCount) {
        validateDraw(false);
    }

    @Override
    public void writeTimestamp(GpuQueryPool queryPool, int index) {
        ensureOpen();
    }

    public void finish() {
        closed = true;
    }

    public boolean scissorEnabled() {
        return scissorEnabled;
    }

    private void validateDraw(boolean indexed) {
        ensureOpen();

        if (pipeline == null) {
            throw new IllegalStateException("Folium draw issued without a pipeline");
        }

        if (indexed && (indexBuffer == null || indexType == null)) {
            throw new IllegalStateException("Folium indexed draw issued without an index buffer");
        }

        // Actual WebGPU draw encoding is the next rendering milestone.
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Folium render pass is closed");
        }
    }
}
