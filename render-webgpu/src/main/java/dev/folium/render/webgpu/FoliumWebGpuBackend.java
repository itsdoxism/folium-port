package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;

/**
 * Browser implementation hook for Minecraft 26.3's RenderPearl backend API.
 *
 * WebGPU initialization is asynchronous in the browser, so Folium performs it
 * before Minecraft starts. RenderPearl's existing frontend remains intact and
 * wraps the Folium backend device.
 */
public final class FoliumWebGpuBackend implements GpuBackend {
    @Override
    public String getName() {
        return "Folium WebGPU";
    }

    @Override
    public void loadLibrary() {
        // No native dynamic library loader exists in the browser.
    }

    @Override
    public void unloadLibrary() {
        // Browser GPU lifetime is managed by the host/device layer.
    }

    @Override
    public long createWindow(String title, int width, int height, long flags) {
        // Opaque browser-window token, not a native pointer.
        return 1L;
    }

    @Override
    public GpuDevice createDevice(GpuDebugOptions debugOptions) {
        return new FrontendGpuDevice(new FoliumBackendDevice(debugOptions));
    }
}
