package dev.folium.platform;

import java.nio.ByteBuffer;

public interface GraphicsHost {
    boolean isWebGpuAvailable();
    boolean isDeviceReady();
    String adapterName();
    int deviceToken();

    int createTexture(String label, int usage, String format, int width, int height, int depthOrLayers, int mipLevels);
    void destroyTexture(int textureToken);
    int createTextureView(int textureToken, int baseMipLevel, int mipLevels);
    void releaseTextureView(int textureViewToken);

    int createBuffer(String label, int usage, long size);
    void writeBuffer(int bufferToken, long offset, ByteBuffer data);
    int createBootstrapTriangleIndexBuffer();
    void destroyBuffer(int bufferToken);

    int createCommandEncoder();
    int beginColorRenderPass(int encoderToken, int textureViewToken, boolean clear, float red, float green, float blue, float alpha);
    void pushRenderPassDebugGroup(int renderPassToken, String label);
    void popRenderPassDebugGroup(int renderPassToken);
    void setRenderPassScissor(int renderPassToken, int x, int y, int width, int height);

    int createBootstrapTrianglePipeline(String colorFormat);
    void destroyPipeline(int pipelineToken);
    void setRenderPassPipeline(int renderPassToken, int pipelineToken);
    void setRenderPassVertexBuffer(int renderPassToken, int slot, int bufferToken, long offset, long length);
    void setRenderPassIndexBuffer(int renderPassToken, int bufferToken, String indexFormat);
    void drawRenderPass(int renderPassToken, int vertexCount, int instanceCount, int firstVertex, int firstInstance);
    void drawIndexedRenderPass(int renderPassToken, int indexCount, int instanceCount, int firstIndex, int baseVertex, int firstInstance);

    void endRenderPass(int renderPassToken);
    void clearColorTexture(int encoderToken, int textureToken, float red, float green, float blue, float alpha);
    void submitCommandEncoder(int encoderToken);
}
