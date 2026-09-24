package dev.folium.runtime;

import dev.folium.platform.GraphicsHost;
import org.teavm.jso.JSBody;

public final class BrowserGraphicsHost implements GraphicsHost {
    @Override
    @JSBody(script = "return !!globalThis.__foliumHostState?.webGpuAvailable;")
    public native boolean isWebGpuAvailable();

    @Override
    @JSBody(script = "return !!globalThis.__foliumHostState?.deviceReady;")
    public native boolean isDeviceReady();

    @Override
    @JSBody(script = "return globalThis.__foliumHostState?.adapterName || '';")
    public native String adapterName();

    @Override
    @JSBody(script = "return globalThis.__foliumHostState?.deviceToken || 0;")
    public native int deviceToken();

    @Override
    @JSBody(
        params = {"label", "usage", "format", "width", "height", "depthOrLayers", "mipLevels"},
        script = """
            return globalThis.__foliumWebGpuBridge.createTexture(
                label, usage, format, width, height, depthOrLayers, mipLevels
            );
        """
    )
    public native int createTexture(
        String label,
        int usage,
        String format,
        int width,
        int height,
        int depthOrLayers,
        int mipLevels
    );

    @Override
    @JSBody(
        params = {"textureToken"},
        script = "globalThis.__foliumWebGpuBridge.destroyTexture(textureToken);"
    )
    public native void destroyTexture(int textureToken);

    @Override
    @JSBody(script = "return globalThis.__foliumWebGpuBridge.createCommandEncoder();")
    public native int createCommandEncoder();

    @Override
    @JSBody(
        params = {"encoderToken", "textureToken", "red", "green", "blue", "alpha"},
        script = """
            globalThis.__foliumWebGpuBridge.clearColorTexture(
                encoderToken, textureToken, red, green, blue, alpha
            );
        """
    )
    public native void clearColorTexture(
        int encoderToken,
        int textureToken,
        float red,
        float green,
        float blue,
        float alpha
    );

    @Override
    @JSBody(
        params = {"encoderToken"},
        script = "globalThis.__foliumWebGpuBridge.submitCommandEncoder(encoderToken);"
    )
    public native void submitCommandEncoder(int encoderToken);
}
