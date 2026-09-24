package dev.folium.platform;

/**
 * Browser graphics state prepared before Minecraft's synchronous renderer boot.
 */
public interface GraphicsHost {
    boolean isWebGpuAvailable();

    boolean isDeviceReady();

    String adapterName();

    int deviceToken();
}
