package dev.folium.render.webgpu;

import java.util.List;

public record FoliumVertexLayout(
    int arrayStride,
    StepMode stepMode,
    List<Attribute> attributes
) {
    public enum StepMode {
        VERTEX,
        INSTANCE
    }

    public record Attribute(
        int shaderLocation,
        int offset,
        String webGpuFormat,
        String sourceName
    ) {
    }
}
