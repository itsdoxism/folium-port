package dev.folium.runtime;

import dev.folium.platform.FoliumRuntime;

public final class FoliumMain {
    private FoliumMain() {
    }

    public static void main(String[] args) {
        if (!FoliumRuntime.isInstalled()) {
            FoliumRuntime.install(new BrowserFoliumPlatform());
        }

        if (!WebGpuProbe.isReady()) {
            WebGpuProbe.setStatus(
                "Folium WASM booted before WebGPU host state was ready",
                false
            );
            return;
        }

        if (WebGpuProbe.renderFirstFrame()) {
            WebGpuProbe.setStatus(
                "Folium platform installed · WebGPU frame OK",
                true
            );
        } else {
            WebGpuProbe.setStatus(
                "Folium WebGPU frame submission failed",
                false
            );
        }
    }
}
