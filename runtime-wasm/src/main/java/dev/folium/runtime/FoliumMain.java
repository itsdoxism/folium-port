package dev.folium.runtime;

public final class FoliumMain {
    private FoliumMain() {
    }

    public static void main(String[] args) {
        if (!WebGpuProbe.isReady()) {
            WebGpuProbe.setStatus("Folium WASM booted before WebGPU host state was ready", false);
            return;
        }

        if (WebGpuProbe.renderFirstFrame()) {
            WebGpuProbe.setStatus("Folium WASM-GC → preinitialized WebGPU frame OK", true);
        } else {
            WebGpuProbe.setStatus("Folium WebGPU frame submission failed", false);
        }
    }
}
