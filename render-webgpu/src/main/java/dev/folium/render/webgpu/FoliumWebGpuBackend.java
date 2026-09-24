package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;

/**
 * Browser implementation hook for Minecraft 26.3's RenderPearl backend API.
 *
 * WebGPU initialization is asynchronous in the browser, so Folium performs it
 * before Minecraft starts. By the time RenderPearl calls createDevice(), the
 * browser host is expected to be ready.
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
        // Opaque Folium browser-window token. This is not a native pointer.
        return 1L;
    }

    @Override
    public GpuDevice createDevice(GpuDebugOptions debugOptions) {
        return new FoliumGpuDevice(debugOptions);
    }
}
