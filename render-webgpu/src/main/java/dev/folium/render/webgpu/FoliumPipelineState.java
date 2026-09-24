package dev.folium.render.webgpu;

import java.util.List;

public record FoliumPipelineState(
    String primitiveTopology,
    String frontFace,
    String cullMode,
    String polygonMode,
    List<FoliumVertexLayout> vertexBuffers,
    List<ColorTarget> colorTargets,
    DepthState depthState
) {
    public record BlendComponent(
        String operation,
        String srcFactor,
        String dstFactor
    ) {}

    public record BlendState(
        BlendComponent color,
        BlendComponent alpha
    ) {}

    public record ColorTarget(
        String format,
        int writeMask,
        BlendState blend
    ) {}

    public record DepthState(
        String format,
        boolean depthWriteEnabled,
        String depthCompare,
        float depthBiasSlopeScale,
        int depthBias
    ) {}
}
