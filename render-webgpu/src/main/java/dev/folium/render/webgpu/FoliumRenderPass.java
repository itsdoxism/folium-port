package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.folium.platform.GraphicsHost;
import org.lwjgl.PointerBuffer;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.function.Supplier;

public final class FoliumRenderPass implements RenderPass {
    private final GraphicsHost graphics;
    private final int token;
    private final int width;
    private final int height;
    private boolean closed;

    FoliumRenderPass(GraphicsHost graphics, int token, int width, int height) {
        this.graphics = graphics;
        this.token = token;
        this.width = width;
        this.height = height;
    }

    @Override
    public void pushDebugGroup(Supplier<String> label) {
        ensureOpen();
        graphics.pushRenderPassDebugGroup(token, label.get());
    }

    @Override
    public void popDebugGroup() {
        ensureOpen();
        graphics.popRenderPassDebugGroup(token);
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {
        throw unsupported("writeTimestamp");
    }

    @Override
    public void setPipeline(CompiledRenderPipeline pipeline) {
        ensureOpen();
        if (!(pipeline instanceof FoliumCompiledRenderPipeline foliumPipeline)) {
            throw new IllegalArgumentException(
                "Folium render pass received a non-Folium pipeline"
            );
        }
        graphics.setRenderPassPipeline(token, foliumPipeline.token());
    }

    @Override
    public void setUniform(String name, GpuTextureView textureView, GpuSampler sampler) {
        throw unsupported("setUniform(texture)");
    }

    @Override
    public void setUniform(String name, GpuBuffer value) {
        throw unsupported("setUniform(buffer)");
    }

    @Override
    public void setUniform(String name, GpuBufferSlice value) {
        throw unsupported("setUniform(bufferSlice)");
    }

    @Override
    public void pushConstants(ByteBuffer value) {
        throw unsupported("pushConstants");
    }

    @Override
    public void enableScissor(int x, int y, int width, int height) {
        ensureOpen();
        graphics.setRenderPassScissor(token, x, y, width, height);
    }

    @Override
    public void disableScissor() {
        ensureOpen();
        graphics.setRenderPassScissor(token, 0, 0, width, height);
    }

    @Override
    public void setVertexBuffer(int slot, GpuBufferSlice vertexBuffer) {
        ensureOpen();

        if (!(vertexBuffer.buffer() instanceof FoliumGpuBuffer buffer)) {
            throw new IllegalArgumentException(
                "Folium render pass received a non-Folium vertex buffer"
            );
        }

        graphics.setRenderPassVertexBuffer(
            token,
            slot,
            buffer.token(),
            vertexBuffer.offset(),
            vertexBuffer.length()
        );
    }

    @Override
    public void setIndexBuffer(GpuBuffer indexBuffer, IndexType indexType) {
        ensureOpen();

        if (!(indexBuffer instanceof FoliumGpuBuffer buffer)) {
            throw new IllegalArgumentException(
                "Folium render pass received a non-Folium index buffer"
            );
        }

        graphics.setRenderPassIndexBuffer(
            token,
            buffer.token(),
            indexType == IndexType.SHORT ? "uint16" : "uint32"
        );
    }

    @Override
    public void drawIndexed(
        int indexCount,
        int instanceCount,
        int firstIndex,
        int vertexOffset,
        int firstInstance
    ) {
        ensureOpen();
        graphics.drawIndexedRenderPass(
            token,
            indexCount,
            instanceCount,
            firstIndex,
            vertexOffset,
            firstInstance
        );
    }

    @Override
    public void multiDrawIndexed(
        IntBuffer drawParameters,
        int instanceCount,
        int firstInstance,
        int drawCount
    ) {
        throw unsupported("multiDrawIndexed(parameters)");
    }

    @Override
    public void multiDrawIndexed(
        PointerBuffer firstIndexOffsets,
        IntBuffer indexCounts,
        IntBuffer vertexOffsets,
        int drawCount
    ) {
        throw unsupported("multiDrawIndexed(buffers)");
    }

    @Override
    public void drawIndexedIndirect(GpuBufferSlice commands, int drawCount) {
        throw unsupported("drawIndexedIndirect");
    }

    @Override
    public <T> void drawMultipleIndexed(
        Collection<RenderPass.Draw<T>> draws,
        GpuBuffer defaultIndexBuffer,
        IndexType defaultIndexType,
        Collection<String> dynamicUniforms,
        T uniformArgument
    ) {
        throw unsupported("drawMultipleIndexed");
    }

    @Override
    public void draw(
        int vertexCount,
        int instanceCount,
        int firstVertex,
        int firstInstance
    ) {
        ensureOpen();
        graphics.drawRenderPass(
            token,
            vertexCount,
            instanceCount,
            firstVertex,
            firstInstance
        );
    }

    @Override
    public void multiDraw(
        IntBuffer drawParameters,
        int instanceCount,
        int firstInstance,
        int drawCount
    ) {
        throw unsupported("multiDraw(parameters)");
    }

    @Override
    public void multiDraw(
        IntBuffer firstVertices,
        IntBuffer vertexCounts,
        int drawCount
    ) {
        throw unsupported("multiDraw(buffers)");
    }

    @Override
    public void drawIndirect(GpuBufferSlice commands, int drawCount) {
        throw unsupported("drawIndirect");
    }

    @Override
    public void close() {
        if (!closed) {
            graphics.endRenderPass(token);
            closed = true;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Folium render pass is closed");
        }
    }

    private static UnsupportedOperationException unsupported(String operation) {
        return new UnsupportedOperationException(
            "Folium WebGPU: " + operation + " is not implemented yet"
        );
    }
}
