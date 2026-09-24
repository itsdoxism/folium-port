package dev.folium.render.webgpu;

import com.mojang.renderpearl.api.device.BackendCreationException;
import com.mojang.renderpearl.api.device.GpuBackend;
import com.mojang.renderpearl.api.device.GpuDebugOptions;
import com.mojang.renderpearl.api.device.GpuDevice;

/**
 * Browser implementation hook for Minecraft 26.3's RenderPearl backend API.
 *
 * This class deliberately implements only the very top RenderPearl seam first.
 * Device/surface/command implementations will be added incrementally.
 */
public final class FoliumWebGpuBackend implements GpuBackend {
    @Override
    public String getName() {
        return "Folium WebGPU";
    }

    @Override
    public void loadLibrary() {
        // No native loader in the browser.
        // WebGPU capability checks belong in createDevice().
    }

    @Override
    public void unloadLibrary() {
        // Browser-owned GPU resources are released by the device/surface layer.
    }

    @Override
    public long createWindow(String title, int width, int height, long flags) {
        /*
         * RenderPearl models its platform window as a native long handle.
         * Folium cannot expose a real SDL/native pointer, so browser builds
         * will patch the SDL Window shell and interpret this value as an opaque
         * Folium window token.
         */
        return 1L;
    }

    @Override
    public GpuDevice createDevice(GpuDebugOptions debugOptions) throws BackendCreationException {
        throw new BackendCreationException(
            "Folium WebGPU device bootstrap is not implemented yet",
            BackendCreationException.Reason.OTHER
        );
    }
}
