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

    int createCommandEncoder();

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
