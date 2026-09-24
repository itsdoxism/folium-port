package dev.folium.runtime;

public final class FoliumMain {
    private FoliumMain() {
    }

    public static void main(String[] args) {
        if (!WebGpuProbe.prepareCanvas()) {
            WebGpuProbe.setStatus("Folium booted, but canvas was not found", false);
            return;
        }

        if (!WebGpuProbe.isAvailable()) {
            WebGpuProbe.setStatus("Folium WASM-GC → browser bridge OK · WebGPU unavailable", false);
            return;
        }

        WebGpuProbe.setStatus("Folium WASM-GC → requesting WebGPU device…", true);
        WebGpuProbe.renderFirstFrame();
    }
}
