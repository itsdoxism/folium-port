package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;
import dev.folium.platform.FoliumRuntime;

/**
 * Browser implementation hook for Minecraft 26.3's RenderPearl backend API.
 *
 * WebGPU adapter/device creation is asynchronous, while RenderPearl's
 * createDevice() contract is synchronous. Folium therefore requires the
 * browser launcher to prepare WebGPU before Minecraft renderer boot.
 */
public final class FoliumWebGpuBackend implements GpuBackend {
    @Override
    public String getName() {
        return "Folium WebGPU";
    }

    @Override
    public void loadLibrary() {
        // No native library load in the browser.
    }

    @Override
    public void unloadLibrary() {
        // Browser GPU lifetime is owned by the Folium host.
    }

    @Override
    public long createWindow(String title, int width, int height, long flags) {
        // Opaque browser-window token. SDL/native window handles do not exist.
        return 1L;
    }

    @Override
    public GpuDevice createDevice(GpuDebugOptions debugOptions) throws BackendCreationException {
        if (!FoliumRuntime.isInstalled()) {
            throw new BackendCreationException(
                "Folium platform was not installed before renderer boot",
                BackendCreationException.Reason.PLATFORM_ERROR
            );
        }

        var graphics = FoliumRuntime.platform().graphics();

        if (!graphics.isWebGpuAvailable()) {
            throw new BackendCreationException(
                "WebGPU is unavailable in this browser",
                BackendCreationException.Reason.OTHER
            );
        }

        if (!graphics.isDeviceReady()) {
            throw new BackendCreationException(
                "WebGPU device was not preloaded before synchronous RenderPearl boot",
                BackendCreationException.Reason.PLATFORM_ERROR
            );
        }

        throw new BackendCreationException(
            "Folium WebGPU host is ready, but GpuDevice wrapper is not implemented yet",
            BackendCreationException.Reason.OTHER
        );
    }
}
