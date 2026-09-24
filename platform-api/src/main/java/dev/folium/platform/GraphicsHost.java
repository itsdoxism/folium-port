package dev.folium.platform;

/**
 * Browser graphics state prepared before Minecraft's synchronous renderer boot.
 *
 * Resource methods use opaque integer tokens so the Minecraft-facing modules
 * do not depend on JavaScript object types.
 */
public interface GraphicsHost {
    boolean isWebGpuAvailable();

    boolean isDeviceReady();

    String adapterName();

    int deviceToken();

    int createTexture(
        String label,
        int usage,
        String format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    );

    void destroyTexture(int textureToken);

    int createTextureView(int textureToken, int baseMipLevel, int mipLevels);

    void releaseTextureView(int textureViewToken);

    int createCommandEncoder();

    int beginColorRenderPass(
        int encoderToken,
        int textureViewToken,
        boolean clear,
        float red,
        float green,
        float blue,
        float alpha
    );

    void pushRenderPassDebugGroup(int renderPassToken, String label);

    void popRenderPassDebugGroup(int renderPassToken);

    void setRenderPassScissor(
        int renderPassToken,
        int x,
        int y,
        int width,
        int height
    );

    void endRenderPass(int renderPassToken);

    void clearColorTexture(
        int encoderToken,
        int textureToken,
        float red,
        float green,
        float blue,
        float alpha
    );

    void submitCommandEncoder(int encoderToken);
}
