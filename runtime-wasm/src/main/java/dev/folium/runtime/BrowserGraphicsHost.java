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
}
